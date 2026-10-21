package cl.duoc.smcft.core.db

import cl.duoc.smcft.core.model.*

/**
 * Los tres triggers del informe (Anexos 9, 10 y 11). [BaseDatos] los ejecuta automáticamente
 * desde sus sentencias DML; nadie los llama por su nombre.
 */
internal object Triggers {

    enum class Accion { INSERT, UPDATE, DELETE }

    /** Usuario técnico fijo que usan los triggers (ID_USUARIO = 1). */
    const val ID_USUARIO_SISTEMA = 1L

    /**
     * TRG_CRUCE_PLAZO_AI — AFTER INSERT ON CRUCE_FRONTERIZO FOR EACH ROW
     * WHEN (NEW.ID_VEHICULO IS NOT NULL AND NEW.TIPO_MOVIMIENTO = 'SALIDA')
     *
     * Crea el plazo de admisión: 90 días si el vehículo es diplomático, 180 en los demás casos.
     */
    fun trgCrucePlazoAi(db: BaseDatos, new: CruceFronterizo) {
        // Cláusula WHEN
        val idVehiculo = new.idVehiculo ?: return
        if (new.tipoMovimiento != "SALIDA") return

        val vehiculo = db.tablas.vehiculos.firstOrNull { it.idVehiculo == idVehiculo }
            ?: throw SmcftException(Errores.VEHICULO_NO_ENCONTRADO, "Vehiculo inexistente al generar plazo.")

        val (dias, tipoPlazo) = if (vehiculo.tipoVehiculo == "DIPLOMATICO") {
            90L to "DIPLOMATICO_90"
        } else {
            180L to "ADMISION_TEMPORAL_180"
        }

        db.insertarPlazo(
            PlazoAdmision(
                idPlazo = db.nextval(Secuencias.PLAZO),
                idVehiculo = idVehiculo,
                idCruceIngreso = new.idCruce,
                fechaIngreso = new.fechaHora,
                fechaLimite = new.fechaHora.plusDays(dias),
                tipo = tipoPlazo,
                estado = "ACTIVO",
                observaciones = "Generado automaticamente por TRG_CRUCE_PLAZO_AI",
            )
        )
    }

    /**
     * TRG_DOCUMENTO_AUDIT — AFTER INSERT OR UPDATE OR DELETE ON DOCUMENTO FOR EACH ROW
     *
     * Reutiliza una auditoría ABIERTA de la entidad DOCUMENTO (o crea una) y agrega un detalle
     * por cada documento afectado.
     */
    fun trgDocumentoAudit(db: BaseDatos, accion: Accion, old: Documento?, new: Documento?) {
        val idDocumento = when (accion) {
            Accion.INSERT, Accion.UPDATE -> new!!.idDocumento
            Accion.DELETE -> old!!.idDocumento
        }

        val idAuditoria = db.tablas.auditorias
            .firstOrNull { it.entidad == "DOCUMENTO" && it.estado == "ABIERTA" }
            ?.idAuditoria
            ?: db.nextval(Secuencias.AUDITORIA).also { id ->
                db.insertarAuditoria(
                    Auditoria(
                        idAuditoria = id,
                        idUsuarioAuditor = ID_USUARIO_SISTEMA,
                        tipoAuditoria = "AUTOMATICA_TRIGGER",
                        entidad = "DOCUMENTO",
                        entidadId = null,
                        descripcion = "Auditoria automatica de cambios en la tabla DOCUMENTO",
                        estado = "ABIERTA",
                        fechaInicio = db.ahora(),
                        fechaCierre = null,
                    )
                )
            }

        db.insertarAuditoriaDetalle(
            AuditoriaDetalle(
                idDetalle = db.nextval(Secuencias.AUDITORIA_DET),
                idAuditoria = idAuditoria,
                accion = accion.name,
                descripcion = "Operacion ${accion.name} sobre SMCFT.DOCUMENTO.ID_DOCUMENTO=$idDocumento",
                idUsuario = ID_USUARIO_SISTEMA,
                resultado = "OK",
                ipAddress = null,
                fechaAccion = db.ahora(),
            )
        )
    }

    /**
     * TRG_DOCUMENTO_SENTENCIA — AFTER INSERT OR UPDATE OR DELETE ON DOCUMENTO (de sentencia)
     *
     * Se ejecuta una sola vez por instrucción, aunque afecte varias filas o ninguna.
     */
    fun trgDocumentoSentencia(db: BaseDatos, accion: Accion) {
        val ahora = db.ahora()
        db.insertarAuditoria(
            Auditoria(
                idAuditoria = db.nextval(Secuencias.AUDITORIA),
                idUsuarioAuditor = ID_USUARIO_SISTEMA,
                tipoAuditoria = "TRIGGER_SENTENCIA",
                entidad = "DOCUMENTO",
                entidadId = null,
                descripcion = "Sentencia ${accion.name}; sesion Oracle: ${db.usuarioSesion}",
                estado = "COMPLETADA",
                fechaInicio = ahora,
                fechaCierre = ahora,
            )
        )
    }
}
