@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package cl.duoc.smcft.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * Entidades del esquema SMCFT (Anexo de código 1: 01_tablas.sql).
 * Cada data class corresponde a una tabla; los nombres de campos siguen a las columnas.
 */

@Serializable
data class Persona(
    val idPersona: Long,
    val rut: String,
    val nombres: String,
    val apellidos: String,
    val nacionalidad: String,
    val fechaNacimiento: LocalDate,
) {
    val nombreCompleto: String get() = "$nombres $apellidos"
}

@Serializable
data class Usuario(
    val idUsuario: Long,
    val idPersona: Long,
    val email: String,
    val activo: Boolean = true,
    /** SHA-256 de la contraseña (ver [cl.duoc.smcft.core.seguridad.Seguridad.hash]). */
    val passwordHash: String = "",
)

@Serializable
data class Rol(val idRol: Long, val nombreRol: String)

@Serializable
data class UsuarioRol(val idUsuarioRol: Long, val idUsuario: Long, val idRol: Long, val activo: Boolean = true)

/** LOGIN_ATTEMPT: cada intento de inicio de sesión, exitoso o no. */
@Serializable
data class IntentoLogin(
    val idIntento: Long,
    val idUsuario: Long?,
    val exitoso: Boolean,
    val ipAddress: String?,
    val fechaIntento: LocalDateTime,
)

@Serializable
data class Vehiculo(
    val idVehiculo: Long,
    val patente: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    /** PARTICULAR, DIPLOMATICO, ... */
    val tipoVehiculo: String,
    val idPersonaPropietario: Long,
    val estado: String = "ACTIVO",
)

@Serializable
data class Documento(
    val idDocumento: Long,
    val tipoDocumento: String,
    val numero: String,
    val fechaEmision: LocalDate?,
    val fechaVencimiento: LocalDate?,
    val idPersona: Long?,
    val idVehiculo: Long?,
    val estado: String,
)

@Serializable
data class PasoFronterizo(
    val idPaso: Long,
    val nombre: String,
    val region: String,
    val pais: String,
    val estado: String = "ACTIVO",
)

@Serializable
data class CruceFronterizo(
    val idCruce: Long,
    val idPaso: Long,
    val idVehiculo: Long?,
    /** ENTRADA / SALIDA */
    val tipoMovimiento: String,
    val fechaHora: LocalDateTime,
    val estado: String,
)

@Serializable
data class DetalleCrucePersona(
    val idCruce: Long,
    val idPersona: Long,
    /** CONDUCTOR / ACOMPANANTE */
    val rolViajero: String,
)

@Serializable
data class AutorizacionMenor(
    val idAutorizacion: Long,
    val idMenor: Long,
    val idRepresentante: Long,
    val idDocumento: Long,
    val tipoPermiso: String,
    val fechaEmision: LocalDate,
    val estado: String,
)

@Serializable
data class CategoriaItem(
    val idCategoria: Long,
    val nombre: String,
    val descripcion: String,
    val requiereDeclaracion: Boolean = false,
    val limiteValorUsd: Double? = null,
    val activo: Boolean = true,
)

@Serializable
data class Declaracion(
    val idDeclaracion: Long,
    val idCruce: Long,
    /** ADUANA / SAG */
    val tipo: String,
    val fecha: LocalDate,
    val estado: String,
)

@Serializable
data class ItemDeclarado(
    val idItem: Long,
    val idDeclaracion: Long,
    val idCategoria: Long,
    val descripcion: String,
    val cantidad: Int,
    /** Monto total del ítem (no se multiplica por CANTIDAD, igual que en el informe). */
    val valorUsd: Double,
    val aprobado: Boolean = false,
)

@Serializable
data class PlazoAdmision(
    val idPlazo: Long,
    val idVehiculo: Long,
    val idCruceIngreso: Long,
    val fechaIngreso: LocalDateTime,
    val fechaLimite: LocalDateTime,
    /** DIPLOMATICO_90 / ADMISION_TEMPORAL_180 */
    val tipo: String,
    /** ACTIVO / VENCIDO / CERRADO */
    val estado: String,
    val observaciones: String? = null,
)

@Serializable
data class Reporte(
    val idReporte: Long,
    val titulo: String,
    val tipoReporte: String,
    val fechaInicio: LocalDateTime,
    val fechaFin: LocalDateTime,
    val idUsuarioGenerador: Long,
    val estado: String,
    val observaciones: String? = null,
)

@Serializable
data class ReporteDetalle(
    val idDetalle: Long,
    val idReporte: Long,
    val descripcion: String,
    val valor: Double,
    val unidad: String,
    val categoria: String,
)

@Serializable
data class Auditoria(
    val idAuditoria: Long,
    val idUsuarioAuditor: Long,
    val tipoAuditoria: String,
    val entidad: String,
    val entidadId: Long?,
    val descripcion: String,
    val estado: String,
    val fechaInicio: LocalDateTime,
    val fechaCierre: LocalDateTime?,
)

@Serializable
data class AuditoriaDetalle(
    val idDetalle: Long,
    val idAuditoria: Long,
    val accion: String,
    val descripcion: String,
    val idUsuario: Long,
    val resultado: String,
    val ipAddress: String?,
    val fechaAccion: LocalDateTime,
)
