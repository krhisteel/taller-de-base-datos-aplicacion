package cl.duoc.smcft

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.smcft.core.db.ArchivoJson
import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.SmcftException
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.RepositorioLocal
import cl.duoc.smcft.core.repo.RepositorioOracle
import cl.duoc.smcft.core.repo.SmcftRepositorio
import cl.duoc.smcft.core.seguridad.Sesion
import cl.duoc.smcft.core.seguridad.TipoUsuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dónde se guardan los datos: en el teléfono (demostración) o en Oracle mediante ORDS. */
enum class Modo { DEMO, ORACLE }

data class Ajustes(
    val modo: Modo = Modo.DEMO,
    val urlOrds: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    /** ID_USUARIO que figura como generador de los reportes. */
    val idUsuarioReportes: Long = 1,
)

data class EstadoUi(
    val datos: Datos? = null,
    val cargando: Boolean = false,
    val error: String? = null,
)

/**
 * Mantiene el repositorio activo y los datos que muestran las pantallas. Las operaciones se
 * ejecutan fuera del hilo principal y, al terminar, se recargan los datos.
 */
class SmcftViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("smcft", Context.MODE_PRIVATE)
    private val archivoDemo = File(app.filesDir, "smcft.json")

    private val _ajustes = MutableStateFlow(leerAjustes())
    val ajustes: StateFlow<Ajustes> = _ajustes.asStateFlow()

    private val _estado = MutableStateFlow(EstadoUi(cargando = true))
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes

    private val _sesion = MutableStateFlow(leerSesion())
    /** Usuario conectado; null muestra la pantalla de inicio de sesión. */
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    @Volatile
    private var repo: SmcftRepositorio? = null

    init {
        if (_sesion.value != null) recargar() else _estado.value = EstadoUi()
    }

    // ---------------------------------------------------------------- sesión

    /** Valida las credenciales en el repositorio activo (demostración u Oracle). */
    fun iniciarSesion(email: String, clave: String, alTerminar: (String?) -> Unit) {
        viewModelScope.launch {
            val error = try {
                val r = repositorio()
                val sesion = withContext(Dispatchers.IO) { r.iniciarSesion(email, clave) }
                guardarSesion(sesion)
                _sesion.value = sesion
                recargar()
                null
            } catch (e: SmcftException) {
                when (e.codigo) {
                    -20031 -> "Correo o contraseña incorrectos."
                    -20032 -> "Su usuario está desactivado. Contacte al administrador."
                    else -> mensaje(e)
                }
            } catch (e: Exception) {
                mensaje(e)
            }
            alTerminar(error)
        }
    }

    fun cerrarSesion() {
        guardarSesion(null)
        _sesion.value = null
        _estado.value = EstadoUi()
    }

    /** Correo del último usuario que inició sesión (para completar el formulario). */
    val ultimoCorreo: String get() = prefs.getString("ultimoCorreo", "").orEmpty()

    private fun guardarSesion(s: Sesion?) {
        val e = prefs.edit()
        if (s == null) {
            e.putLong("sesion.id", -1)
        } else {
            e.putLong("sesion.id", s.idUsuario)
                .putString("sesion.nombre", s.nombre)
                .putString("sesion.email", s.email)
                .putString("sesion.rol", s.tipo.codigo)
                .putString("ultimoCorreo", s.email)
        }
        e.apply()
    }

    private fun leerSesion(): Sesion? {
        val id = prefs.getLong("sesion.id", -1)
        if (id < 0) return null
        return Sesion(
            idUsuario = id,
            nombre = prefs.getString("sesion.nombre", "").orEmpty(),
            email = prefs.getString("sesion.email", "").orEmpty(),
            tipo = TipoUsuario.desdeCodigo(prefs.getString("sesion.rol", null)),
        )
    }

    private suspend fun repositorio(): SmcftRepositorio = withContext(Dispatchers.IO) {
        repo ?: crearRepositorio(_ajustes.value).also { repo = it }
    }

    private fun crearRepositorio(a: Ajustes): SmcftRepositorio = when (a.modo) {
        Modo.DEMO -> {
            val archivo = ArchivoJson(archivoDemo)
            val db = BaseDatos(archivo.cargar() ?: Tablas()).apply { alConfirmar = archivo::guardar }
            RepositorioLocal(db)
        }
        Modo.ORACLE -> RepositorioOracle(a.urlOrds, a.clientId, a.clientSecret)
    }

    fun recargar() {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            try {
                val r = repositorio()
                val datos = withContext(Dispatchers.IO) { r.cargarTodo() }
                _estado.value = EstadoUi(datos = datos)
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensaje(e)) }
            }
        }
    }

    /**
     * Ejecuta una operación del repositorio (una transacción). Muestra [exito] o el error de Oracle,
     * avisa el resultado con [alTerminar] y recarga los datos.
     */
    fun <T> ejecutar(
        exito: (T) -> String,
        alTerminar: (Boolean) -> Unit = {},
        operacion: SmcftRepositorio.() -> T,
    ) {
        viewModelScope.launch {
            val ok = try {
                val r = repositorio()
                val resultado = withContext(Dispatchers.IO) { r.operacion() }
                _mensajes.emit(exito(resultado))
                true
            } catch (e: Exception) {
                _mensajes.emit(mensaje(e))
                false
            }
            alTerminar(ok)
            recargar()
        }
    }

    /** Consulta puntual (por ejemplo, el detalle de un cruce). */
    suspend fun <T> consultar(operacion: SmcftRepositorio.() -> T): T {
        val r = repositorio()
        return withContext(Dispatchers.IO) { r.operacion() }
    }

    /** Prueba una configuración sin guardarla. Devuelve un texto para mostrar. */
    suspend fun probarConexion(a: Ajustes): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resumen = crearRepositorio(a).resumen()
            "Conexión correcta: ${resumen.personas} personas y ${resumen.cruces} cruces registrados."
        }.recoverCatching { throw IllegalStateException(mensaje(it)) }
    }

    fun guardarAjustes(a: Ajustes) {
        prefs.edit()
            .putString("modo", a.modo.name)
            .putString("url", a.urlOrds.trim())
            .putString("clientId", a.clientId.trim())
            .putString("clientSecret", a.clientSecret.trim())
            .putLong("idUsuario", a.idUsuarioReportes)
            .apply()
        val cambioServidor = a.modo != _ajustes.value.modo || a.urlOrds.trim() != _ajustes.value.urlOrds
        _ajustes.value = a
        repo = null
        _mensajes.tryEmit(if (a.modo == Modo.ORACLE) "Conexión con Oracle guardada" else "Modo demostración activado")
        if (cambioServidor) {
            // Las cuentas pertenecen a cada servidor: hay que volver a iniciar sesión.
            cerrarSesion()
        } else if (_sesion.value != null) {
            _estado.value = EstadoUi(cargando = true)
            recargar()
        }
    }

    /** Borra los datos de demostración y vuelve a cargar los del informe. */
    fun reiniciarDemo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { archivoDemo.delete() }
            if (_ajustes.value.modo == Modo.DEMO) repo = null
            _mensajes.emit("Datos de demostración reiniciados")
            cerrarSesion()
        }
    }

    private fun leerAjustes() = Ajustes(
        modo = runCatching { Modo.valueOf(prefs.getString("modo", null) ?: "DEMO") }.getOrDefault(Modo.DEMO),
        urlOrds = prefs.getString("url", "").orEmpty(),
        clientId = prefs.getString("clientId", "").orEmpty(),
        clientSecret = prefs.getString("clientSecret", "").orEmpty(),
        idUsuarioReportes = prefs.getLong("idUsuario", 1),
    )

    /** Texto para el usuario: sin el prefijo técnico cuando es un error de negocio conocido. */
    private fun mensaje(e: Throwable): String = when (e) {
        is SmcftException -> "${e.detalle} (ORA${e.codigo})"
        else -> e.message ?: e.javaClass.simpleName
    }
}
