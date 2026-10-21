package cl.duoc.smcft.core.db

import cl.duoc.smcft.core.model.*
import kotlinx.serialization.Serializable

/**
 * Fotografía inmutable de todas las tablas del esquema SMCFT más el valor actual de cada
 * secuencia (Anexo 2: 02_secuencias.sql).
 *
 * Al ser inmutable, un SAVEPOINT es simplemente guardar la referencia a la instancia actual
 * y un ROLLBACK TO SAVEPOINT es volver a ella.
 */
@Serializable
data class Tablas(
    val personas: List<Persona> = emptyList(),
    val usuarios: List<Usuario> = emptyList(),
    val vehiculos: List<Vehiculo> = emptyList(),
    val documentos: List<Documento> = emptyList(),
    val pasos: List<PasoFronterizo> = emptyList(),
    val cruces: List<CruceFronterizo> = emptyList(),
    val detallesCruce: List<DetalleCrucePersona> = emptyList(),
    val autorizaciones: List<AutorizacionMenor> = emptyList(),
    val categorias: List<CategoriaItem> = emptyList(),
    val declaraciones: List<Declaracion> = emptyList(),
    val items: List<ItemDeclarado> = emptyList(),
    val plazos: List<PlazoAdmision> = emptyList(),
    val reportes: List<Reporte> = emptyList(),
    val reporteDetalles: List<ReporteDetalle> = emptyList(),
    val auditorias: List<Auditoria> = emptyList(),
    val auditoriaDetalles: List<AuditoriaDetalle> = emptyList(),
    val secuencias: Map<String, Long> = emptyMap(),
)

object Secuencias {
    const val PERSONA = "SEQ_PERSONA"
    const val VEHICULO = "SEQ_VEHICULO"
    const val DOCUMENTO = "SEQ_DOCUMENTO"
    const val PASO = "SEQ_PASO"
    const val CRUCE = "SEQ_CRUCE"
    const val CATEGORIA = "SEQ_CATEGORIA"
    const val DECLARACION = "SEQ_DECLARACION"
    const val ITEM = "SEQ_ITEM"
    const val PLAZO = "SEQ_PLAZO"
    const val AUTORIZACION = "SEQ_AUTORIZACION"
    const val REPORTE = "SEQ_REPORTE"
    const val REPORTE_DET = "SEQ_REPORTE_DET"
    const val AUDITORIA = "SEQ_AUDITORIA"
    const val AUDITORIA_DET = "SEQ_AUDITORIA_DET"
    const val USUARIO = "SEQ_USUARIO"
}
