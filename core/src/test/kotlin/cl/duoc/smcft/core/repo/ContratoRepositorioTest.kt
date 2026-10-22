package cl.duoc.smcft.core.repo

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Errores
import cl.duoc.smcft.core.db.SmcftException
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.core.seguridad.Seguridad
import cl.duoc.smcft.core.seguridad.TipoUsuario
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Mismo comportamiento para el modo demostración y para Oracle real. Las pruebas funcionan sobre
 * una base que ya tiene datos: usan RUT y patentes nuevos y comparan diferencias.
 */
abstract class ContratoRepositorioTest {

    abstract fun repositorio(): SmcftRepositorio

    private val repo by lazy { repositorio() }
    private val sufijo = Random.nextInt(1_000_000, 9_999_999)

    private fun idPaso(nombre: String) = repo.pasos().first { it.nombre == nombre }.idPaso

    @Test
    fun `registrar una salida vehicular crea el plazo de 180 dias y acepta declaraciones`() {
        val idCruce = repo.registrarCruce(idPaso("Los Libertadores"), "ABCD12", "SALIDA", listOf("12345678-9"), "12345678-9")

        val plazo = repo.plazos().single { it.idCruce == idCruce }
        assertEquals("ADMISION_TEMPORAL_180", plazo.tipo)
        assertEquals(180, plazo.diasRestantes)

        repo.registrarDeclaracion(idCruce, "ADUANA", repo.categorias().first { it.nombre == "ELECTRONICA" }.idCategoria, "Notebook", 1, 450.5)
        assertEquals(450.5, repo.cruces().single { it.idCruce == idCruce }.valorDeclarado)
        assertEquals(listOf(ItemInfo("Notebook", 1, 450.5)), repo.items(idCruce))
        assertEquals(listOf(ViajeroInfo("12345678-9", "Ana Perez Soto", "CONDUCTOR")), repo.viajeros(idCruce))
    }

    @Test
    fun `un menor sin autorizacion es rechazado con -20002 y despues de autorizarlo puede cruzar`() {
        val rut = "$sufijo-1"
        val idMenor = repo.registrarPersona(NuevaPersona(rut, "Tomas", "Prueba", "chilena", LocalDate.now().minusYears(10)))
        val crucesAntes = repo.resumen().cruces

        val error = assertFailsWith<SmcftException> {
            repo.registrarCruce(idPaso("Cardenal Samore"), null, "ENTRADA", listOf("12345678-9", rut), null)
        }
        assertEquals(Errores.MENOR_SIN_AUTORIZACION, error.codigo)
        assertEquals(crucesAntes, repo.resumen().cruces, "el cruce fallido se revierte")

        val idAna = repo.personas().first { it.rut == "12345678-9" }.idPersona
        repo.registrarAutorizacion(idMenor, idAna, "AUT-$sufijo")
        assertTrue(repo.personas().first { it.idPersona == idMenor }.autorizacionVigente)

        val idCruce = repo.registrarCruce(idPaso("Cardenal Samore"), null, "ENTRADA", listOf("12345678-9", rut), null)
        assertEquals(2, repo.viajeros(idCruce).size)
    }

    @Test
    fun `errores de vehiculo y RUT inexistentes`() {
        val paso = idPaso("Los Libertadores")
        assertEquals(Errores.VEHICULO_NO_ENCONTRADO, assertFailsWith<SmcftException> {
            repo.registrarCruce(paso, "NO$sufijo", "SALIDA", listOf("12345678-9"), null)
        }.codigo)
        assertEquals(Errores.PERSONA_NO_EXISTE, assertFailsWith<SmcftException> {
            repo.registrarCruce(paso, null, "ENTRADA", listOf("$sufijo-9"), null)
        }.codigo)
    }

    @Test
    fun `reporte valido y rango invertido rechazado por la validacion privada`() {
        val hoy = LocalDate.now()
        val idReporte = repo.generarReporte(hoy.minusDays(1), hoy, 1)
        val reporte = repo.reportes().single { it.idReporte == idReporte }
        assertEquals("GENERADO", reporte.estado)
        assertEquals(3 * repo.pasos().count { it.estado == "ACTIVO" }, reporte.detalles.size)

        val error = assertFailsWith<SmcftException> { repo.generarReporte(hoy, hoy.minusDays(1), 1) }
        assertEquals(Errores.RANGO_INVALIDO, error.codigo)
    }

    @Test
    fun `los cambios en documentos quedan auditados por los triggers`() {
        fun detalles() = repo.auditorias().sumOf { it.detalles.size }
        fun sentencias() = repo.auditorias().count { it.tipoAuditoria == "TRIGGER_SENTENCIA" }
        val idAna = repo.personas().first { it.rut == "12345678-9" }.idPersona

        val detallesAntes = detalles()
        val sentenciasAntes = sentencias()

        val id = repo.registrarDocumento(
            NuevoDocumento("PASAPORTE", "P-$sufijo", LocalDate.of(2010, 1, 1), LocalDate.of(2020, 1, 1), idAna, null)
        )
        assertEquals(1, repo.cambiarEstadoDocumento(id, "REVISADO"))
        assertEquals(1, repo.cambiarEstadoDocumento(id, "VIGENTE"))
        assertTrue(repo.marcarDocumentosVencidos() >= 1)
        assertEquals("VENCIDO", repo.documentos().single { it.idDocumento == id }.estado)
        assertEquals(1, repo.eliminarDocumento(id))

        // INSERT + 2 UPDATE + UPDATE masivo (>= 1 fila) + DELETE
        assertTrue(detalles() - detallesAntes >= 5)
        assertEquals(sentenciasAntes + 5, sentencias())
    }

    @Test
    fun `cada cuenta de prueba inicia sesion con su tipo de usuario`() {
        for (cuenta in Seguridad.CUENTAS_PRUEBA) {
            val sesion = repo.iniciarSesion(cuenta.email.uppercase(), cuenta.clave)
            assertEquals(cuenta.tipo, sesion.tipo, cuenta.email)
        }
        val admin = repo.iniciarSesion("admin@smcft.cl", "Admin2026")
        assertEquals("Claudia Salazar Vera", admin.nombre)
        assertTrue(admin.puede(Permiso.ADMINISTRAR))
        assertTrue(!repo.iniciarSesion("sag@smcft.cl", "Sag2026").puede(Permiso.REGISTRAR_CRUCE))
    }

    @Test
    fun `credenciales incorrectas se rechazan y quedan registradas en LOGIN_ATTEMPT`() {
        val antes = repo.accesos().firstOrNull()?.idIntento ?: 0
        assertEquals(Errores.CREDENCIALES_INVALIDAS, assertFailsWith<SmcftException> { repo.iniciarSesion("pdi@smcft.cl", "incorrecta") }.codigo)
        assertEquals(Errores.CREDENCIALES_INVALIDAS, assertFailsWith<SmcftException> { repo.iniciarSesion("nadie$sufijo@smcft.cl", "x") }.codigo)

        val nuevos = repo.accesos().filter { it.idIntento > antes }
        assertEquals(2, nuevos.size)
        assertTrue(nuevos.none { it.exitoso })
        assertTrue(nuevos.any { it.email == "pdi@smcft.cl" && it.nombre == "Marcela Rojas Diaz" })

        repo.iniciarSesion("pdi@smcft.cl", "Pdi2026")
        assertTrue(repo.accesos().first().exitoso)
        val pdi = repo.usuarios().single { it.email == "pdi@smcft.cl" }
        assertEquals(TipoUsuario.OFICIAL_PDI.codigo, pdi.rol)
        assertTrue(pdi.ultimoAcceso != null)
        assertTrue(repo.usuarios().map { it.rol }.containsAll(TipoUsuario.entries.map { it.codigo }))
    }

    @Test
    fun `nuevo vehiculo diplomatico recibe plazo de 90 dias`() {
        val patente = "DP${sufijo % 10000}"
        val idCarlos = repo.personas().first { it.rut == "9876543-2" }.idPersona
        repo.registrarVehiculo(NuevoVehiculo(patente, "Volvo", "XC60", 2023, "DIPLOMATICO", idCarlos))

        val idCruce = repo.registrarCruce(idPaso("Los Libertadores"), patente, "SALIDA", listOf("9876543-2"), "9876543-2")
        val plazo = repo.plazos().single { it.idCruce == idCruce }
        assertEquals("DIPLOMATICO_90", plazo.tipo)
        assertEquals(90, plazo.diasRestantes)
        assertTrue(repo.actualizarPlazos() >= 0)
    }
}

class ContratoRepositorioLocalTest : ContratoRepositorioTest() {
    override fun repositorio() = RepositorioLocal(BaseDatos())
}

/**
 * Se ejecuta contra un ORDS real solo si existen las variables de entorno
 * SMCFT_ORDS_URL, SMCFT_ORDS_CLIENT_ID y SMCFT_ORDS_CLIENT_SECRET.
 */
class ContratoRepositorioOracleTest : ContratoRepositorioTest() {
    override fun repositorio(): SmcftRepositorio {
        val url = System.getenv("SMCFT_ORDS_URL")
        org.junit.jupiter.api.Assumptions.assumeTrue(!url.isNullOrBlank(), "Sin SMCFT_ORDS_URL: se omite la prueba contra Oracle")
        return RepositorioOracle(url!!, System.getenv("SMCFT_ORDS_CLIENT_ID").orEmpty(), System.getenv("SMCFT_ORDS_CLIENT_SECRET").orEmpty())
    }
}
