package cl.duoc.smcft.core

import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.model.*
import cl.duoc.smcft.core.seguridad.Seguridad
import cl.duoc.smcft.core.seguridad.TipoUsuario
import java.time.LocalDate

/**
 * Datos base de 03_datos_prueba.sql (Anexo 12) y usuarios de 04_usuarios_roles.sql.
 * Los datos base solo se cargan si la base está vacía; los usuarios y roles, si todavía no existen
 * (así una base de demostración de la versión anterior también recibe las cuentas).
 */
object DatosPrueba {

    fun cargar(db: BaseDatos) {
        val triggers = db.triggersHabilitados
        db.triggersHabilitados = false // la carga inicial no debe quedar en la auditoría
        try {
            db.atomico {
                if (db.tablas.personas.isEmpty()) cargarBase(db)
                if (db.tablas.roles.isEmpty()) cargarUsuarios(db)
            }
        } finally {
            db.triggersHabilitados = triggers
        }
        db.commit()
    }

    private fun cargarBase(db: BaseDatos) {
        val hoy = db.hoy()
        db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "12345678-9", "Ana", "Perez Soto", "CHILENA", LocalDate.of(1985, 4, 12)))
        db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "9876543-2", "Carlos", "Diaz Munoz", "ARGENTINA", LocalDate.of(1990, 7, 1)))
        db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "11222333-4", "Sofia", "Perez Rivas", "CHILENA", LocalDate.of(2014, 2, 20)))
        db.insertarPersona(Persona(sig(db, Secuencias.PERSONA), "5555555-5", "Claudia", "Salazar Vera", "CHILENA", LocalDate.of(1980, 1, 1)))

        db.insertarUsuario(Usuario(sig(db, Secuencias.USUARIO), 4, "claudia.salazar@example.com"))

        db.insertarPaso(PasoFronterizo(sig(db, Secuencias.PASO), "Los Libertadores", "Valparaiso", "Chile"))
        db.insertarPaso(PasoFronterizo(sig(db, Secuencias.PASO), "Cardenal Samore", "Los Lagos", "Chile"))

        db.insertarVehiculo(Vehiculo(sig(db, Secuencias.VEHICULO), "ABCD12", "Toyota", "Yaris", 2020, "PARTICULAR", 1))
        db.insertarVehiculo(Vehiculo(sig(db, Secuencias.VEHICULO), "XX999Z", "Ford", "Ranger", 2022, "DIPLOMATICO", 2))

        db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "CEDULA", "C-12345678", LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), 1, null, "VIGENTE"))
        db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "PASAPORTE", "P-9876543", LocalDate.of(2019, 5, 1), LocalDate.of(2029, 5, 1), 2, null, "VIGENTE"))
        db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "CEDULA", "C-11222333", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), 3, null, "VIGENTE"))
        db.insertarDocumento(Documento(sig(db, Secuencias.DOCUMENTO), "AUTORIZACION_NOTARIAL", "AUT-000123", hoy.minusDays(30), hoy.plusMonths(12), 3, null, "VIGENTE"))

        db.insertarAutorizacion(AutorizacionMenor(sig(db, Secuencias.AUTORIZACION), 3, 1, 4, "NOTARIAL", hoy.minusDays(30), "VIGENTE"))

        db.insertarCategoria(CategoriaItem(sig(db, Secuencias.CATEGORIA), "ELECTRONICA", "Equipos electronicos personales", true, 500.0))
        db.insertarCategoria(CategoriaItem(sig(db, Secuencias.CATEGORIA), "ALIMENTOS", "Alimentos de origen animal o vegetal", true, 0.0))
    }

    /** Funcionarios que usan la app, con sus roles. Mismos datos que sql/04_usuarios_roles.sql. */
    private val FUNCIONARIOS = listOf(
        Triple("15678234-3", "Marcela" to "Rojas Diaz", TipoUsuario.OFICIAL_PDI),
        Triple("16789345-7", "Jorge" to "Fuentes Vera", TipoUsuario.FUNCIONARIO_ADUANA),
        Triple("17890456-6", "Paula" to "Contreras Mella", TipoUsuario.INSPECTOR_SAG),
        Triple("18901567-4", "Rodrigo" to "Silva Araya", TipoUsuario.SUPERVISOR),
    )

    private fun cargarUsuarios(db: BaseDatos) {
        val idRol = TipoUsuario.entries.associateWith { tipo ->
            sig(db, Secuencias.ROL).also { db.insertarRol(Rol(it, tipo.codigo)) }
        }

        fun cuenta(tipo: TipoUsuario) = Seguridad.CUENTAS_PRUEBA.first { it.tipo == tipo }

        // El usuario técnico 1 (Claudia Salazar) pasa a ser el administrador.
        val admin = cuenta(TipoUsuario.ADMINISTRADOR)
        db.actualizarUsuario(1) { it.copy(email = admin.email, passwordHash = Seguridad.hash(admin.email, admin.clave)) }
        db.insertarUsuarioRol(UsuarioRol(sig(db, Secuencias.USUARIO_ROL), 1, idRol.getValue(TipoUsuario.ADMINISTRADOR)))

        for ((rut, nombre, tipo) in FUNCIONARIOS) {
            val idPersona = db.tablas.personas.firstOrNull { it.rut == rut }?.idPersona
                ?: sig(db, Secuencias.PERSONA).also {
                    db.insertarPersona(Persona(it, rut, nombre.first, nombre.second, "CHILENA", LocalDate.of(1988, 6, 15)))
                }
            val c = cuenta(tipo)
            val idUsuario = sig(db, Secuencias.USUARIO)
            db.insertarUsuario(Usuario(idUsuario, idPersona, c.email, true, Seguridad.hash(c.email, c.clave)))
            db.insertarUsuarioRol(UsuarioRol(sig(db, Secuencias.USUARIO_ROL), idUsuario, idRol.getValue(tipo)))
        }
    }

    private fun sig(db: BaseDatos, secuencia: String) = db.nextval(secuencia)
}
