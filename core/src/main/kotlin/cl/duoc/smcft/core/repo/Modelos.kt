package cl.duoc.smcft.core.repo

import java.time.LocalDate
import java.time.LocalDateTime

/*
 * Datos que la app muestra. Los entrega el repositorio, ya sea desde Oracle (ORDS) o desde la
 * base local de demostración.
 */

data class Resumen(
    val personas: Int,
    val vehiculos: Int,
    val cruces: Int,
    val plazosActivos: Int,
    /** Resultado de SMCFT.FN_TOTAL_PLAZOS_VENCIDOS(). */
    val plazosVencidos: Int,
    val registrosAuditoria: Int,
)

data class PasoInfo(val idPaso: Long, val nombre: String, val region: String, val estado: String)

data class CategoriaInfo(val idCategoria: Long, val nombre: String)

data class PersonaInfo(
    val idPersona: Long,
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val nacionalidad: String,
    val fechaNacimiento: LocalDate,
    val edad: Int,
    val autorizacionVigente: Boolean,
) {
    val nombreCompleto: String get() = "$nombres $apellidos"
    val esMenor: Boolean get() = edad < 18
}

data class VehiculoInfo(
    val idVehiculo: Long,
    val patente: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val tipoVehiculo: String,
    val propietario: String,
)

data class CruceInfo(
    val idCruce: Long,
    val nombrePaso: String,
    val tipoMovimiento: String,
    val fechaHora: LocalDateTime,
    val estado: String,
    val patente: String?,
    val cantViajeros: Int,
    /** Resultado de SMCFT.FN_VALOR_DECLARADO(ID_CRUCE). */
    val valorDeclarado: Double,
)

data class ViajeroInfo(val rut: String, val nombre: String, val rol: String)

data class ItemInfo(val descripcion: String, val cantidad: Int, val valorUsd: Double)

data class PlazoInfo(
    val idPlazo: Long,
    val patente: String,
    val idCruce: Long,
    val fechaIngreso: LocalDateTime,
    val fechaLimite: LocalDateTime,
    val tipo: String,
    val estado: String,
    /** Negativo si el plazo ya venció. */
    val diasRestantes: Long,
)

data class ReporteDetalleInfo(val descripcion: String, val valor: Double, val unidad: String, val categoria: String)

data class ReporteInfo(
    val idReporte: Long,
    val titulo: String,
    val estado: String,
    val detalles: List<ReporteDetalleInfo>,
)

data class DocumentoInfo(
    val idDocumento: Long,
    val tipoDocumento: String,
    val numero: String,
    val fechaEmision: LocalDate?,
    val fechaVencimiento: LocalDate?,
    val titular: String,
    val estado: String,
)

data class AuditoriaDetalleInfo(
    val idDetalle: Long,
    val accion: String,
    val descripcion: String,
    val resultado: String,
    val fechaAccion: LocalDateTime,
)

data class AuditoriaInfo(
    val idAuditoria: Long,
    val tipoAuditoria: String,
    val descripcion: String,
    val estado: String,
    val fechaInicio: LocalDateTime,
    val fechaCierre: LocalDateTime?,
    val detalles: List<AuditoriaDetalleInfo>,
)

data class NuevaPersona(
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val nacionalidad: String,
    val fechaNacimiento: LocalDate,
)

data class NuevoVehiculo(
    val patente: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val tipoVehiculo: String,
    val idPropietario: Long,
)

data class NuevoDocumento(
    val tipoDocumento: String,
    val numero: String,
    val fechaEmision: LocalDate?,
    val fechaVencimiento: LocalDate?,
    val idPersona: Long?,
    val idVehiculo: Long?,
)

/** Todo lo que muestran las pantallas, cargado de una vez. */
data class Datos(
    val resumen: Resumen,
    val pasos: List<PasoInfo>,
    val categorias: List<CategoriaInfo>,
    val personas: List<PersonaInfo>,
    val vehiculos: List<VehiculoInfo>,
    val cruces: List<CruceInfo>,
    val plazos: List<PlazoInfo>,
    val reportes: List<ReporteInfo>,
    val documentos: List<DocumentoInfo>,
    val auditorias: List<AuditoriaInfo>,
)
