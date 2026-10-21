package cl.duoc.smcft

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duoc.smcft.core.DatosPrueba
import cl.duoc.smcft.core.db.ArchivoJson
import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.pkg.GestionFrontera
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Mantiene la base SMCFT de la app. Cada acción de la interfaz es una transacción:
 * si termina bien se hace COMMIT (y se guarda en disco); si falla, ROLLBACK y se muestra el error.
 */
class SmcftViewModel(app: Application) : AndroidViewModel(app) {

    private val archivo = ArchivoJson(File(app.filesDir, "smcft.json"))

    val db = BaseDatos(archivo.cargar() ?: Tablas()).apply {
        alConfirmar = archivo::guardar
    }

    val pkg = GestionFrontera(db)

    private val _tablas = MutableStateFlow(db.tablas)
    val tablas: StateFlow<Tablas> = _tablas.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes

    init {
        DatosPrueba.cargar(db)
        _tablas.value = db.tablas
    }

    /** Ejecuta [operacion] como una transacción. Devuelve true si se confirmó. */
    fun <T> ejecutar(mensajeExito: (T) -> String, operacion: () -> T): Boolean {
        val ok = try {
            val resultado = operacion()
            db.commit()
            _mensajes.tryEmit(mensajeExito(resultado))
            true
        } catch (e: Exception) {
            db.rollback()
            _mensajes.tryEmit(e.message ?: e.toString())
            false
        }
        _tablas.value = db.tablas
        return ok
    }
}
