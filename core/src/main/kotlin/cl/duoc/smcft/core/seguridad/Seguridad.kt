package cl.duoc.smcft.core.seguridad

import java.security.MessageDigest

/** Acciones que un usuario puede realizar en la app. */
enum class Permiso {
    REGISTRAR_CRUCE,
    DECLARAR_ADUANA,
    DECLARAR_SAG,
    GESTIONAR_PLAZOS,
    GESTIONAR_DOCUMENTOS,
    GESTIONAR_PERSONAS,
    GESTIONAR_VEHICULOS,
    GENERAR_REPORTES,
    VER_AUDITORIA,
    ADMINISTRAR,
}

/**
 * Tipos de usuario. [codigo] es el NOMBRE_ROL guardado en la tabla ROL.
 * Todos pueden consultar cruces, plazos y documentos; los permisos definen qué pueden modificar.
 */
enum class TipoUsuario(val codigo: String, val nombre: String, val descripcion: String, val permisos: Set<Permiso>) {
    ADMINISTRADOR(
        "ADMINISTRADOR", "Administrador",
        "Acceso completo: operación, reportes, auditoría, usuarios y conexión.",
        Permiso.entries.toSet(),
    ),
    OFICIAL_PDI(
        "OFICIAL_PDI", "Oficial PDI",
        "Control migratorio: registra cruces, viajeros, autorizaciones de menores y sus documentos.",
        setOf(Permiso.REGISTRAR_CRUCE, Permiso.GESTIONAR_PERSONAS, Permiso.GESTIONAR_DOCUMENTOS),
    ),
    FUNCIONARIO_ADUANA(
        "FUNCIONARIO_ADUANA", "Funcionario de Aduana",
        "Declaraciones de Aduana, vehículos, plazos de admisión temporal y reportes.",
        setOf(Permiso.DECLARAR_ADUANA, Permiso.GESTIONAR_VEHICULOS, Permiso.GESTIONAR_PLAZOS, Permiso.GENERAR_REPORTES),
    ),
    INSPECTOR_SAG(
        "INSPECTOR_SAG", "Inspector SAG",
        "Control sanitario: registra las declaraciones SAG de los cruces.",
        setOf(Permiso.DECLARAR_SAG),
    ),
    SUPERVISOR(
        "SUPERVISOR", "Supervisor",
        "Consulta general, reportes estadísticos y auditoría. No modifica datos operativos.",
        setOf(Permiso.GENERAR_REPORTES, Permiso.VER_AUDITORIA),
    );

    companion object {
        fun desdeCodigo(codigo: String?): TipoUsuario =
            entries.firstOrNull { it.codigo.equals(codigo, ignoreCase = true) } ?: SUPERVISOR
    }
}

/** Usuario que inició sesión. */
data class Sesion(
    val idUsuario: Long,
    val nombre: String,
    val email: String,
    val tipo: TipoUsuario,
) {
    fun puede(permiso: Permiso) = permiso in tipo.permisos
}

/** Cuentas de demostración (las crea DatosPrueba y el script sql/04_usuarios_roles.sql). */
data class CuentaPrueba(val email: String, val clave: String, val tipo: TipoUsuario)

object Seguridad {

    val CUENTAS_PRUEBA = listOf(
        CuentaPrueba("admin@smcft.cl", "Admin2026", TipoUsuario.ADMINISTRADOR),
        CuentaPrueba("pdi@smcft.cl", "Pdi2026", TipoUsuario.OFICIAL_PDI),
        CuentaPrueba("aduana@smcft.cl", "Aduana2026", TipoUsuario.FUNCIONARIO_ADUANA),
        CuentaPrueba("sag@smcft.cl", "Sag2026", TipoUsuario.INSPECTOR_SAG),
        CuentaPrueba("supervisor@smcft.cl", "Super2026", TipoUsuario.SUPERVISOR),
    )

    /**
     * Hash que se guarda en USUARIO.PASSWORD_HASH. Usa el correo como sal.
     * Equivale en Oracle a: RAWTOHEX(STANDARD_HASH('smcft:' || LOWER(email) || ':' || clave, 'SHA256')).
     */
    fun hash(email: String, clave: String): String {
        val datos = "smcft:${email.trim().lowercase()}:$clave".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02X".format(it) }
    }
}
