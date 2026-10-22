package cl.duoc.smcft.core.repo

import cl.duoc.smcft.core.seguridad.Sesion
import java.time.LocalDate

/**
 * Operaciones que la app necesita del sistema SMCFT.
 *
 * - [RepositorioOracle]: llama al esquema SMCFT en Oracle mediante ORDS. El package, los
 *   procedimientos, las funciones y los triggers se ejecutan dentro de Oracle.
 * - [RepositorioLocal]: modo demostración sin conexión, sobre la base local que imita al esquema.
 *
 * Cada operación de escritura es una transacción: si termina bien queda confirmada (COMMIT); si
 * falla se revierte (ROLLBACK) y se lanza [cl.duoc.smcft.core.db.SmcftException] con el código
 * de error de Oracle.
 *
 * Los métodos bloquean el hilo: la app los llama fuera del hilo principal.
 */
interface SmcftRepositorio {

    // ---------------------------------------------------------------- sesión

    /**
     * Valida el correo y la contraseña contra USUARIO y registra el intento en LOGIN_ATTEMPT.
     * Lanza SmcftException -20031 si las credenciales no son válidas y -20032 si el usuario está inactivo.
     */
    fun iniciarSesion(email: String, clave: String): Sesion

    /** Usuarios con su rol y su último acceso exitoso (solo administración). */
    fun usuarios(): List<UsuarioInfo>

    /** Últimos intentos de inicio de sesión (solo administración). */
    fun accesos(): List<AccesoInfo>

    // ---------------------------------------------------------------- consultas

    fun resumen(): Resumen
    fun pasos(): List<PasoInfo>
    fun categorias(): List<CategoriaInfo>
    fun personas(): List<PersonaInfo>
    fun vehiculos(): List<VehiculoInfo>
    fun cruces(): List<CruceInfo>
    fun viajeros(idCruce: Long): List<ViajeroInfo>
    fun items(idCruce: Long): List<ItemInfo>
    fun plazos(): List<PlazoInfo>
    fun reportes(): List<ReporteInfo>
    fun documentos(): List<DocumentoInfo>
    fun auditorias(): List<AuditoriaInfo>

    fun cargarTodo(): Datos = Datos(
        resumen = resumen(),
        pasos = pasos(),
        categorias = categorias(),
        personas = personas(),
        vehiculos = vehiculos(),
        cruces = cruces(),
        plazos = plazos(),
        reportes = reportes(),
        documentos = documentos(),
        auditorias = auditorias(),
    )

    // ---------------------------------------------------------------- procesos del informe

    /** PKG_GESTION_FRONTERA.sp_registrar_cruce. Devuelve el ID del cruce. */
    fun registrarCruce(
        idPaso: Long,
        patente: String?,
        tipoMovimiento: String,
        ruts: List<String>,
        rutConductor: String?,
    ): Long

    /** SP_ACTUALIZAR_PLAZOS. Devuelve la cantidad de plazos marcados como VENCIDO. */
    fun actualizarPlazos(): Int

    /** SP_REPORTE_CRUCES entre el inicio de [desde] y el final de [hasta]. Devuelve el ID del reporte. */
    fun generarReporte(desde: LocalDate, hasta: LocalDate, idUsuario: Long): Long

    // ---------------------------------------------------------------- mantenimiento

    /** Inserta una declaración con un ítem. Devuelve el ID de la declaración. */
    fun registrarDeclaracion(
        idCruce: Long,
        tipo: String,
        idCategoria: Long,
        descripcion: String,
        cantidad: Int,
        valorUsd: Double,
    ): Long

    /** INSERT en DOCUMENTO (dispara los triggers de auditoría). Devuelve el ID del documento. */
    fun registrarDocumento(documento: NuevoDocumento): Long

    /** UPDATE de un documento. Devuelve SQL%ROWCOUNT. */
    fun cambiarEstadoDocumento(idDocumento: Long, estado: String): Int

    /** Un solo UPDATE que marca VENCIDO los documentos vigentes con fecha de vencimiento pasada. */
    fun marcarDocumentosVencidos(): Int

    /** DELETE de un documento. Devuelve SQL%ROWCOUNT. */
    fun eliminarDocumento(idDocumento: Long): Int

    fun registrarPersona(persona: NuevaPersona): Long
    fun registrarVehiculo(vehiculo: NuevoVehiculo): Long

    /** Crea el documento notarial y la AUTORIZACION_MENOR vigente. Devuelve el ID de la autorización. */
    fun registrarAutorizacion(idMenor: Long, idRepresentante: Long, numeroDocumento: String): Long
}

/** No se pudo hablar con el servidor (red, URL o credenciales). */
class ErrorConexion(mensaje: String, causa: Throwable? = null) : RuntimeException(mensaje, causa)
