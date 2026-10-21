package cl.duoc.smcft.core

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.pkg.GestionFrontera
import java.time.LocalDateTime

/*
 * Procedimientos y funciones independientes (Anexos 5 a 8).
 * Son una capa pública simple que delega en el package: la lógica existe una sola vez.
 */

/** SP_REPORTE_CRUCES — procedimiento con parámetros. Devuelve el ID del reporte (parámetro OUT). */
fun spReporteCruces(db: BaseDatos, desde: LocalDateTime?, hasta: LocalDateTime?, usuario: Long): Long =
    GestionFrontera(db).spReporteCrucesPorPaso(desde, hasta, usuario)

/** SP_ACTUALIZAR_PLAZOS — procedimiento sin parámetros. Devuelve SQL%ROWCOUNT. */
fun spActualizarPlazos(db: BaseDatos): Int =
    GestionFrontera(db).spActualizarPlazosVencidos()

/** FN_VALOR_DECLARADO — función con parámetros. */
fun fnValorDeclarado(db: BaseDatos, cruce: Long): Double =
    GestionFrontera(db).fnValorTotalDeclarado(cruce)

/** FN_TOTAL_PLAZOS_VENCIDOS — función sin parámetros. */
fun fnTotalPlazosVencidos(db: BaseDatos): Int =
    GestionFrontera(db).fnTotalPlazosVencidos()
