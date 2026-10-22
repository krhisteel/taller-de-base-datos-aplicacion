package cl.duoc.smcft

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.smcft.core.db.ArchivoJson
import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.RepositorioLocal
import cl.duoc.smcft.core.repo.RepositorioOracle
import cl.duoc.smcft.core.repo.SmcftRepositorio
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

    @Volatile
    private var repo: SmcftRepositorio? = null

    init {
        recargar()
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
        _ajustes.value = a
        repo = null
        _estado.value = EstadoUi(cargando = true)
        _mensajes.tryEmit(if (a.modo == Modo.ORACLE) "Conectando con Oracle…" else "Modo demostración activado")
        recargar()
    }

    /** Borra los datos de demostración y vuelve a cargar los del informe. */
    fun reiniciarDemo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { archivoDemo.delete() }
            if (_ajustes.value.modo == Modo.DEMO) repo = null
            _mensajes.emit("Datos de demostración reiniciados")
            recargar()
        }
    }

    private fun leerAjustes() = Ajustes(
        modo = runCatching { Modo.valueOf(prefs.getString("modo", null) ?: "DEMO") }.getOrDefault(Modo.DEMO),
        urlOrds = prefs.getString("url", "").orEmpty(),
        clientId = prefs.getString("clientId", "").orEmpty(),
        clientSecret = prefs.getString("clientSecret", "").orEmpty(),
        idUsuarioReportes = prefs.getLong("idUsuario", 1),
    )

    private fun mensaje(e: Throwable): String = e.message ?: e.javaClass.simpleName
}
