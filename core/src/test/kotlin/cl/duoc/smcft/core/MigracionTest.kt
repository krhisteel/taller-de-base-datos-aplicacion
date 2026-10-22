package cl.duoc.smcft.core

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.repo.RepositorioLocal
import cl.duoc.smcft.core.seguridad.TipoUsuario
import kotlin.test.Test
import kotlin.test.assertEquals

class MigracionTest {

    @Test
    fun `una base de la version anterior sin usuarios recibe las cuentas de prueba`() {
        // Base como la dejaba la versión 2: datos del informe, sin roles ni contraseñas.
        val anterior = BaseDatos().also { DatosPrueba.cargar(it) }.tablas
            .let { t -> t.copy(roles = emptyList(), usuarioRoles = emptyList(), usuarios = t.usuarios.filter { it.idUsuario == 1L }.map { it.copy(email = "claudia.salazar@example.com", passwordHash = "") }) }

        val repo = RepositorioLocal(BaseDatos(anterior))

        assertEquals(TipoUsuario.ADMINISTRADOR, repo.iniciarSesion("admin@smcft.cl", "Admin2026").tipo)
        assertEquals(TipoUsuario.OFICIAL_PDI, repo.iniciarSesion("pdi@smcft.cl", "Pdi2026").tipo)
        assertEquals(5, repo.usuarios().size)
    }
}
