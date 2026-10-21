package cl.duoc.smcft.core.pkg

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Errores
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.db.SmcftException
import cl.duoc.smcft.core.model.*
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Equivalente al package PKG_GESTION_FRONTERA (Anexos 3 y 4).
 *
 * Lo público de la especificación son los miembros públicos de esta clase (tipos, excepciones,
 * procedimientos y funciones). Lo que solo existe en el cuerpo es `private`: [validarRango].
 *
 * Igual que en el PL/SQL, los procedimientos NO hacen COMMIT: quien los llama decide cuándo
 * confirmar con [BaseDatos.commit].
 */
class GestionFrontera(private val db: BaseDatos) {

    // ------------------------------------------------------------ tipos públicos

    /** t_detalle_cruce */
    data class DetalleCruce(
        val idCruce: Long,
        val nombrePaso: String,
        val tipoMovimiento: String,
        val fechaHora: LocalDateTime,
        val cantViajeros: Int,
        val valorDeclarado: Double,
    )

    /** t_item_resumen */
    data class ItemResumen(val descripcion: String, val cantidad: Int, val valorUsd: Double)

    /** t_plazo_info */
    data class PlazoInfo(val idPlazo: Long, val patente: String, val fechaLimite: LocalDateTime, val diasRestantes: Long)

    companion object {
        /** t_ruts_varray IS VARRAY(10) */
        const val MAX_VIAJEROS = 10

        /** t_items_varray IS VARRAY(50) */
        const val MAX_ITEMS = 50

        private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }

    // ------------------------------------------------------------ elemento privado

    /** Solo existe en el cuerpo del package: rechaza fechas nulas o con inicio posterior al fin. */
    private fun validarRango(desde: LocalDateTime?, hasta: LocalDateTime?) {
        if (desde == null || hasta == null || desde > hasta) {
            throw SmcftException(Errores.RANGO_INVALIDO, "Rango de fechas invalido.")
        }
    }

    // ------------------------------------------------------------ funciones públicas

    /** fn_valor_total_declarado: suma VALOR_USD de todos los ítems de las declaraciones del cruce. */
    fun fnValorTotalDeclarado(idCruce: Long): Double {
        val t = db.tablas
        val declaraciones = t.declaraciones.filter { it.idCruce == idCruce }.map { it.idDeclaracion }.toSet()
        return t.items.filter { it.idDeclaracion in declaraciones }.sumOf { it.valorUsd }
    }

    /** fn_dias_restantes_plazo: días hasta la fecha límite; lanza -20001 si ya venció y -20006 si no existe. */
    fun fnDiasRestantesPlazo(idPlazo: Long): Long {
        val plazo = db.tablas.plazos.firstOrNull { it.idPlazo == idPlazo }
            ?: throw SmcftException(Errores.PLAZO_NO_EXISTE, "No existe el plazo de admision indicado (ID_PLAZO=$idPlazo).")
        val dias = ChronoUnit.DAYS.between(db.hoy(), plazo.fechaLimite.toLocalDate())
        if (dias < 0) {
            throw SmcftException(
                Errores.PLAZO_VENCIDO,
                "El plazo de admision ID_PLAZO=$idPlazo se encuentra VENCIDO (dias=$dias).",
            )
        }
        return dias
    }

    /** fn_total_plazos_vencidos: plazos no cerrados con fecha límite anterior a hoy (no cambia su estado). */
    fun fnTotalPlazosVencidos(): Int {
        val hoy = db.hoy().atStartOfDay()
        return db.tablas.plazos.count { it.estado != "CERRADO" && it.fechaLimite < hoy }
    }

    // ------------------------------------------------------------ procedimientos públicos

    /**
     * sp_registrar_cruce: valida el movimiento, el vehículo y a cada viajero (los menores de 18
     * necesitan autorización VIGENTE), inserta la cabecera y el detalle de viajeros.
     *
     * @param patente null si el cruce es peatonal.
     * @return ID del cruce creado (p_id_cruce_out).
     */
    fun spRegistrarCruce(
        idPaso: Long,
        patente: String?,
        tipoMovimiento: String?,
        rutsViajeros: List<String>?,
        rutConductor: String? = null,
    ): Long {
        val savepoint = db.savepoint() // SAVEPOINT operacion_cruce
        try {
            if (tipoMovimiento == null || tipoMovimiento !in listOf("ENTRADA", "SALIDA")) {
                throw SmcftException(Errores.MOVIMIENTO_INVALIDO, "Movimiento invalido.")
            }
            if (rutsViajeros.isNullOrEmpty()) {
                throw SmcftException(Errores.SIN_VIAJEROS, "Debe informar viajeros.")
            }
            require(rutsViajeros.size <= MAX_VIAJEROS) { "ORA-06532: Subindice fuera del limite (VARRAY de $MAX_VIAJEROS)" }

            val idVehiculo = patente?.takeIf { it.isNotBlank() }?.let { p ->
                db.tablas.vehiculos.firstOrNull { it.patente.equals(p, ignoreCase = true) }?.idVehiculo
                    ?: throw SmcftException(
                        Errores.VEHICULO_NO_ENCONTRADO,
                        "No existe un vehiculo registrado con patente $p.",
                    )
            }

            val idCruce = db.nextval(Secuencias.CRUCE)
            // El INSERT dispara TRG_CRUCE_PLAZO_AI cuando es SALIDA con vehículo.
            db.insertarCruce(
                CruceFronterizo(
                    idCruce = idCruce,
                    idPaso = idPaso,
                    idVehiculo = idVehiculo,
                    tipoMovimiento = tipoMovimiento,
                    fechaHora = db.ahora(),
                    estado = "AUTORIZADO",
                )
            )

            for (rut in rutsViajeros) {
                val persona = db.tablas.personas.firstOrNull { it.rut.equals(rut, ignoreCase = true) }
                    ?: throw SmcftException(Errores.PERSONA_NO_EXISTE, "No existe una persona registrada con RUT $rut.")

                val edad = Period.between(persona.fechaNacimiento, db.hoy()).years
                if (edad < 18) {
                    val autorizaciones = db.tablas.autorizaciones.count {
                        it.idMenor == persona.idPersona && it.estado == "VIGENTE"
                    }
                    if (autorizaciones == 0) {
                        throw SmcftException(
                            Errores.MENOR_SIN_AUTORIZACION,
                            "El viajero con RUT $rut es menor de edad y no cuenta con autorizacion vigente para cruzar.",
                        )
                    }
                }

                val rol = if (rutConductor != null && rut.equals(rutConductor, ignoreCase = true)) "CONDUCTOR" else "ACOMPANANTE"
                db.insertarDetalleCruce(DetalleCrucePersona(idCruce, persona.idPersona, rol))
            }
            return idCruce
        } catch (e: Throwable) {
            db.rollbackTo(savepoint) // ROLLBACK TO operacion_cruce; RAISE;
            throw e
        }
    }

    /**
     * sp_reporte_cruces_por_paso: recorre los pasos ACTIVOS, sus cruces del rango y sus viajeros
     * (cursores anidados) y guarda tres indicadores por paso en REPORTE_DETALLE.
     *
     * @return ID del reporte generado (p_id_reporte_out).
     */
    fun spReporteCrucesPorPaso(desde: LocalDateTime?, hasta: LocalDateTime?, idUsuarioGenerador: Long): Long {
        val savepoint = db.savepoint() // SAVEPOINT operacion_reporte
        try {
            validarRango(desde, hasta)
            desde!!; hasta!!

            // c_pasos
            val pasos = db.tablas.pasos.filter { it.estado == "ACTIVO" }.sortedBy { it.nombre }
            if (pasos.isEmpty()) {
                throw SmcftException(
                    Errores.SIN_PASOS_ACTIVOS,
                    "No fue posible generar el reporte: no existen pasos fronterizos activos.",
                )
            }

            val idReporte = db.nextval(Secuencias.REPORTE)
            db.insertarReporte(
                Reporte(
                    idReporte = idReporte,
                    titulo = "Cruces por paso fronterizo ${desde.format(FORMATO_FECHA)} a ${hasta.format(FORMATO_FECHA)}",
                    tipoReporte = "CRUCES",
                    fechaInicio = desde,
                    fechaFin = hasta,
                    idUsuarioGenerador = idUsuarioGenerador,
                    estado = "EN_PROCESO",
                )
            )

            for (paso in pasos) {
                var totalCruces = 0
                var totalViajeros = 0
                var totalValor = 0.0

                // c_cruces_paso: FECHA_HORA BETWEEN p_desde AND p_hasta
                val cruces = db.tablas.cruces
                    .filter { it.idPaso == paso.idPaso && it.fechaHora >= desde && it.fechaHora <= hasta }
                    .sortedBy { it.fechaHora }

                for (cruce in cruces) {
                    totalCruces++
                    // c_viajeros_cruce
                    totalViajeros += db.tablas.detallesCruce.count { it.idCruce == cruce.idCruce }
                    totalValor += fnValorTotalDeclarado(cruce.idCruce)
                }

                insertarDetalle(idReporte, "Cruces registrados en ${paso.nombre}", totalCruces.toDouble(), "cruces", "CRUCES_POR_PASO")
                insertarDetalle(idReporte, "Viajeros registrados en ${paso.nombre}", totalViajeros.toDouble(), "personas", "VIAJEROS_POR_PASO")
                insertarDetalle(idReporte, "Valor USD declarado en ${paso.nombre}", totalValor, "USD", "VALOR_DECLARADO_POR_PASO")
            }

            db.actualizarReporte(idReporte) { it.copy(estado = "GENERADO") }
            return idReporte
        } catch (e: Throwable) {
            db.rollbackTo(savepoint)
            throw e
        }
    }

    private fun insertarDetalle(idReporte: Long, descripcion: String, valor: Double, unidad: String, categoria: String) {
        db.insertarReporteDetalle(
            ReporteDetalle(db.nextval(Secuencias.REPORTE_DET), idReporte, descripcion, valor, unidad, categoria)
        )
    }

    /** sp_actualizar_plazos_vencidos: marca VENCIDO los plazos ACTIVO con fecha límite anterior a hoy. */
    fun spActualizarPlazosVencidos(): Int {
        val hoy = db.hoy().atStartOfDay()
        return db.actualizarPlazos(
            donde = { it.estado == "ACTIVO" && it.fechaLimite < hoy },
            cambio = { it.copy(estado = "VENCIDO") },
        )
    }

    // ------------------------------------------------------------ consultas de apoyo para la app

    /** Lista de cruces con el formato t_detalle_cruce, más recientes primero. */
    fun detallesCruces(): List<DetalleCruce> {
        val t = db.tablas
        return t.cruces.sortedByDescending { it.fechaHora }.map { c ->
            DetalleCruce(
                idCruce = c.idCruce,
                nombrePaso = t.pasos.firstOrNull { it.idPaso == c.idPaso }?.nombre ?: "?",
                tipoMovimiento = c.tipoMovimiento,
                fechaHora = c.fechaHora,
                cantViajeros = t.detallesCruce.count { it.idCruce == c.idCruce },
                valorDeclarado = fnValorTotalDeclarado(c.idCruce),
            )
        }
    }

    /** Ítems declarados de un cruce (t_items_varray). */
    fun itemsCruce(idCruce: Long): List<ItemResumen> {
        val t = db.tablas
        val declaraciones = t.declaraciones.filter { it.idCruce == idCruce }.map { it.idDeclaracion }.toSet()
        return t.items.filter { it.idDeclaracion in declaraciones }
            .take(MAX_ITEMS)
            .map { ItemResumen(it.descripcion, it.cantidad, it.valorUsd) }
    }

    /** Información de un plazo (t_plazo_info); días negativos si ya venció. */
    fun plazoInfo(plazo: PlazoAdmision): PlazoInfo {
        val patente = db.tablas.vehiculos.firstOrNull { it.idVehiculo == plazo.idVehiculo }?.patente ?: "?"
        val dias = ChronoUnit.DAYS.between(db.hoy(), plazo.fechaLimite.toLocalDate())
        return PlazoInfo(plazo.idPlazo, patente, plazo.fechaLimite, dias)
    }

    /** Agrega una declaración (Aduana/SAG) con sus ítems a un cruce. */
    fun registrarDeclaracion(idCruce: Long, tipo: String, items: List<Triple<Long, String, Pair<Int, Double>>>): Long =
        db.atomico {
            require(items.size <= MAX_ITEMS) { "Maximo $MAX_ITEMS items por declaracion." }
            val idDeclaracion = db.nextval(Secuencias.DECLARACION)
            db.insertarDeclaracion(Declaracion(idDeclaracion, idCruce, tipo, db.hoy(), "PRESENTADA"))
            for ((idCategoria, descripcion, cantidadValor) in items) {
                db.insertarItem(
                    ItemDeclarado(
                        idItem = db.nextval(Secuencias.ITEM),
                        idDeclaracion = idDeclaracion,
                        idCategoria = idCategoria,
                        descripcion = descripcion,
                        cantidad = cantidadValor.first,
                        valorUsd = cantidadValor.second,
                    )
                )
            }
            idDeclaracion
        }
}
