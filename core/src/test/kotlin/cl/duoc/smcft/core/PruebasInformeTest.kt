package cl.duoc.smcft.core

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Errores
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.db.SmcftException
import cl.duoc.smcft.core.model.CruceFronterizo
import cl.duoc.smcft.core.model.Documento
import cl.duoc.smcft.core.model.Persona
import cl.duoc.smcft.core.pkg.GestionFrontera
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Las siete pruebas funcionales del informe (01 a 07) y los casos de error que el informe
 * recomienda agregar. Cada prueba trabaja con SAVEPOINT al inicio y ROLLBACK al final.
 */
class PruebasInformeTest {

    private val reloj = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneId.of("America/Santiago"))
    private lateinit var db: BaseDatos
    private lateinit var pkg: GestionFrontera

    @BeforeTest
    fun preparar() {
        db = BaseDatos(reloj = reloj)
        DatosPrueba.cargar(db)
        pkg = GestionFrontera(db)
    }

    private fun conRollback(prueba: () -> Unit) {
        val sp = db.savepoint()
        try {
            prueba()
        } finally {
            db.rollbackTo(sp)
        }
    }

    @Test
    fun `01 trigger de plazo crea plazo de 180 dias con INSERT directo`() {
        val antes = db.tablas
        conRollback {
            val id = db.nextval(Secuencias.CRUCE)
            db.insertarCruce(CruceFronterizo(id, 1, 1, "SALIDA", db.ahora(), "AUTORIZADO"))
            val plazo = db.tablas.plazos.single { it.idCruceIngreso == id }
            assertEquals("ADMISION_TEMPORAL_180", plazo.tipo)
            assertEquals("ACTIVO", plazo.estado)
            assertEquals(db.ahora().plusDays(180), plazo.fechaLimite)
        }
        assertEquals(antes, db.tablas, "los datos de prueba deben revertirse")
    }

    @Test
    fun `01b vehiculo diplomatico recibe 90 dias y una ENTRADA no genera plazo`() = conRollback {
        val salida = pkg.spRegistrarCruce(2, "XX999Z", "SALIDA", listOf("9876543-2"), "9876543-2")
        assertEquals("DIPLOMATICO_90", db.tablas.plazos.single { it.idCruceIngreso == salida }.tipo)

        val entrada = pkg.spRegistrarCruce(2, "XX999Z", "ENTRADA", listOf("9876543-2"), "9876543-2")
        assertTrue(db.tablas.plazos.none { it.idCruceIngreso == entrada })
    }

    @Test
    fun `02 un UPDATE sobre dos documentos genera 2 detalles de fila y 1 auditoria de sentencia`() = conRollback {
        val detallesAntes = db.tablas.auditoriaDetalles.size
        val sentenciaAntes = db.tablas.auditorias.count { it.tipoAuditoria == "TRIGGER_SENTENCIA" }

        val filas = db.actualizarDocumentos({ it.idDocumento in setOf(1L, 2L) }, { it.copy(estado = "REVISADO") })

        assertEquals(2, filas)
        assertEquals(detallesAntes + 2, db.tablas.auditoriaDetalles.size)
        assertEquals(sentenciaAntes + 1, db.tablas.auditorias.count { it.tipoAuditoria == "TRIGGER_SENTENCIA" })
        assertTrue(db.tablas.auditoriaDetalles.all { it.accion == "UPDATE" })
    }

    @Test
    fun `02b insert y delete de documento tambien quedan auditados`() = conRollback {
        val id = db.nextval(Secuencias.DOCUMENTO)
        db.insertarDocumento(Documento(id, "PERMISO_CIRCULACION", "PC-1", null, null, null, 1, "VIGENTE"))
        db.eliminarDocumentos { it.idDocumento == id }
        assertEquals(listOf("INSERT", "DELETE"), db.tablas.auditoriaDetalles.map { it.accion })
        assertEquals(2, db.tablas.auditorias.count { it.tipoAuditoria == "TRIGGER_SENTENCIA" })
        assertEquals(1, db.tablas.auditorias.count { it.tipoAuditoria == "AUTOMATICA_TRIGGER" })
    }

    @Test
    fun `03 package publico registra un cruce con su viajero`() = conRollback {
        val idCruce = pkg.spRegistrarCruce(1, "ABCD12", "SALIDA", listOf("12345678-9"), "12345678-9")
        val detalle = db.tablas.detallesCruce.single { it.idCruce == idCruce }
        assertEquals(1, detalle.idPersona)
        assertEquals("CONDUCTOR", detalle.rolViajero)
        assertEquals(1, db.tablas.plazos.count { it.idCruceIngreso == idCruce })
    }

    @Test
    fun `03b menor con autorizacion vigente puede cruzar como acompanante`() = conRollback {
        val idCruce = pkg.spRegistrarCruce(1, null, "SALIDA", listOf("12345678-9", "11222333-4"), "12345678-9")
        val roles = db.tablas.detallesCruce.filter { it.idCruce == idCruce }.associate { it.idPersona to it.rolViajero }
        assertEquals(mapOf(1L to "CONDUCTOR", 3L to "ACOMPANANTE"), roles)
        assertTrue(db.tablas.plazos.none { it.idCruceIngreso == idCruce }, "un cruce peatonal no genera plazo")
    }

    @Test
    fun `04 procedimiento con parametros genera un reporte con detalles`() = conRollback {
        val idCruce = pkg.spRegistrarCruce(1, "ABCD12", "SALIDA", listOf("12345678-9"), "12345678-9")
        pkg.registrarDeclaracion(idCruce, "ADUANA", listOf(Triple(1L, "Notebook", 1 to 450.0)))

        val hoy = db.hoy()
        val idReporte = spReporteCruces(db, hoy.minusDays(1).atStartOfDay(), hoy.plusDays(1).atStartOfDay(), 1)

        val reporte = db.tablas.reportes.single { it.idReporte == idReporte }
        assertEquals("GENERADO", reporte.estado)
        val detalles = db.tablas.reporteDetalles.filter { it.idReporte == idReporte }
        assertEquals(6, detalles.size, "3 indicadores x 2 pasos activos")
        val libertadores = detalles.filter { it.descripcion.endsWith("Los Libertadores") }.associate { it.categoria to it.valor }
        assertEquals(1.0, libertadores["CRUCES_POR_PASO"])
        assertEquals(1.0, libertadores["VIAJEROS_POR_PASO"])
        assertEquals(450.0, libertadores["VALOR_DECLARADO_POR_PASO"])
    }

    @Test
    fun `05 funcion cuenta el plazo vencido y el procedimiento lo marca VENCIDO`() = conRollback {
        val idCruce = pkg.spRegistrarCruce(1, "ABCD12", "SALIDA", listOf("12345678-9"), "12345678-9")
        val plazo = db.tablas.plazos.single { it.idCruceIngreso == idCruce }
        db.actualizarPlazos({ it.idPlazo == plazo.idPlazo }, { it.copy(fechaLimite = db.ahora().minusDays(5)) })

        assertEquals(1, fnTotalPlazosVencidos(db))
        assertEquals(1, spActualizarPlazos(db))
        assertEquals("VENCIDO", db.tablas.plazos.single { it.idPlazo == plazo.idPlazo }.estado)
        // La función sigue contándolo porque no está CERRADO.
        assertEquals(1, fnTotalPlazosVencidos(db))
        assertEquals(0, spActualizarPlazos(db))

        val error = assertFailsWith<SmcftException> { pkg.fnDiasRestantesPlazo(plazo.idPlazo) }
        assertEquals(Errores.PLAZO_VENCIDO, error.codigo)
    }

    @Test
    fun `06 funcion con parametros devuelve USD 450`() = conRollback {
        val idCruce = pkg.spRegistrarCruce(1, null, "ENTRADA", listOf("9876543-2"))
        pkg.registrarDeclaracion(idCruce, "ADUANA", listOf(Triple(1L, "Notebook", 1 to 450.0)))
        // Equivale a: SELECT ID_CRUCE, FN_VALOR_DECLARADO(ID_CRUCE) FROM CRUCE_FRONTERIZO
        val valores = db.tablas.cruces.associate { it.idCruce to fnValorDeclarado(db, it.idCruce) }
        assertEquals(450.0, valores[idCruce])
    }

    @Test
    fun `07 validacion privada rechaza el rango invertido con -20020`() = conRollback {
        val hoy = db.hoy()
        val error = assertFailsWith<SmcftException> {
            spReporteCruces(db, hoy.plusDays(1).atStartOfDay(), hoy.minusDays(1).atStartOfDay(), 1)
        }
        assertEquals(Errores.RANGO_INVALIDO, error.codigo)
        assertTrue(db.tablas.reportes.isEmpty())
    }

    // ---------------------------------------------------------- casos de error (recomendaciones)

    @Test
    fun `menor sin autorizacion es rechazado y no queda nada registrado`() = conRollback {
        db.insertarPersona(Persona(db.nextval(Secuencias.PERSONA), "22333444-5", "Tomas", "Diaz", "CHILENA", LocalDate.of(2015, 3, 3)))
        val antes = db.tablas
        val error = assertFailsWith<SmcftException> {
            pkg.spRegistrarCruce(1, "ABCD12", "SALIDA", listOf("12345678-9", "22333444-5"), "12345678-9")
        }
        assertEquals(Errores.MENOR_SIN_AUTORIZACION, error.codigo)
        assertEquals(antes, db.tablas, "el SAVEPOINT revierte el cruce y el plazo creado por el trigger")
    }

    @Test
    fun `vehiculo inexistente lanza -20004`() = conRollback {
        val error = assertFailsWith<SmcftException> { pkg.spRegistrarCruce(1, "ZZZZ99", "SALIDA", listOf("12345678-9")) }
        assertEquals(Errores.VEHICULO_NO_ENCONTRADO, error.codigo)
    }

    @Test
    fun `RUT no registrado lanza -20010`() = conRollback {
        val error = assertFailsWith<SmcftException> { pkg.spRegistrarCruce(1, null, "ENTRADA", listOf("1-9")) }
        assertEquals(Errores.PERSONA_NO_EXISTE, error.codigo)
        assertTrue(db.tablas.cruces.isEmpty())
    }

    @Test
    fun `movimiento invalido y lista vacia de viajeros`() = conRollback {
        assertEquals(Errores.MOVIMIENTO_INVALIDO, assertFailsWith<SmcftException> { pkg.spRegistrarCruce(1, null, "TRANSITO", listOf("12345678-9")) }.codigo)
        assertEquals(Errores.SIN_VIAJEROS, assertFailsWith<SmcftException> { pkg.spRegistrarCruce(1, null, "ENTRADA", emptyList()) }.codigo)
    }

    @Test
    fun `commit y rollback`() {
        pkg.spRegistrarCruce(1, null, "ENTRADA", listOf("12345678-9"))
        db.rollback()
        assertTrue(db.tablas.cruces.isEmpty())

        pkg.spRegistrarCruce(1, null, "ENTRADA", listOf("12345678-9"))
        db.commit()
        db.rollback()
        assertEquals(1, db.tablas.cruces.size)
    }
}
