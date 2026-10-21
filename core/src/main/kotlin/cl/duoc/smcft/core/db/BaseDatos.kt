package cl.duoc.smcft.core.db

import cl.duoc.smcft.core.model.*
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Base de datos local del esquema SMCFT.
 *
 * Reproduce lo que el informe usa de Oracle:
 *  - secuencias (NEXTVAL),
 *  - transacciones con SAVEPOINT / ROLLBACK TO / COMMIT,
 *  - restricciones (PK, UNIQUE, FK y CHECK) en las sentencias DML,
 *  - triggers de fila y de sentencia que se disparan solos al modificar las tablas
 *    (ver [Triggers]), aunque el dato no pase por el package.
 *
 * Cada método insertar/actualizar/eliminar es una "sentencia": si falla (incluido un trigger),
 * se deshace completa, igual que en Oracle.
 */
class BaseDatos(
    inicial: Tablas = Tablas(),
    private val reloj: Clock = Clock.systemDefaultZone(),
    /** Equivalente a SYS_CONTEXT('USERENV','SESSION_USER'). */
    val usuarioSesion: String = "SMCFT",
) {
    var tablas: Tablas = inicial
        private set

    private var confirmado: Tablas = inicial

    /** Se invoca en cada COMMIT con el estado confirmado (la app lo usa para guardar en disco). */
    var alConfirmar: ((Tablas) -> Unit)? = null

    /** Permite deshabilitar los triggers (ALTER TRIGGER ... DISABLE). */
    var triggersHabilitados: Boolean = true

    // ---------------------------------------------------------------- tiempo (SYSDATE)

    fun ahora(): LocalDateTime = LocalDateTime.now(reloj)
    fun hoy(): LocalDate = LocalDate.now(reloj)

    // ---------------------------------------------------------------- secuencias

    @Synchronized
    fun nextval(secuencia: String): Long {
        val siguiente = (tablas.secuencias[secuencia] ?: 0L) + 1
        tablas = tablas.copy(secuencias = tablas.secuencias + (secuencia to siguiente))
        return siguiente
    }

    // ---------------------------------------------------------------- transacciones

    class Savepoint internal constructor(internal val tablas: Tablas)

    fun savepoint(): Savepoint = Savepoint(tablas)

    @Synchronized
    fun rollbackTo(savepoint: Savepoint) {
        tablas = savepoint.tablas
    }

    @Synchronized
    fun commit() {
        confirmado = tablas
        alConfirmar?.invoke(tablas)
    }

    @Synchronized
    fun rollback() {
        tablas = confirmado
    }

    /** Ejecuta [bloque] y, si lanza una excepción, vuelve al estado previo (SAVEPOINT implícito). */
    fun <T> atomico(bloque: () -> T): T {
        val sp = savepoint()
        try {
            return bloque()
        } catch (e: Throwable) {
            rollbackTo(sp)
            throw e
        }
    }

    private fun modificar(cambio: (Tablas) -> Tablas) {
        tablas = cambio(tablas)
    }

    // ---------------------------------------------------------------- restricciones

    private fun unico(condicion: Boolean, restriccion: String) {
        if (condicion) throw SmcftException(Errores.UNICO_VIOLADO, "restriccion unica (SMCFT.$restriccion) violada")
    }

    private fun padre(existe: Boolean, restriccion: String) {
        if (!existe) throw SmcftException(Errores.FK_VIOLADA, "restriccion de integridad (SMCFT.$restriccion) violada - clave principal no encontrada")
    }

    private fun sinHijos(tieneHijos: Boolean, restriccion: String) {
        if (tieneHijos) throw SmcftException(-2292, "restriccion de integridad (SMCFT.$restriccion) violada - registro secundario encontrado")
    }

    private fun existePersona(id: Long) = tablas.personas.any { it.idPersona == id }
    private fun existeVehiculo(id: Long) = tablas.vehiculos.any { it.idVehiculo == id }
    private fun existeCruce(id: Long) = tablas.cruces.any { it.idCruce == id }
    private fun existeUsuario(id: Long) = tablas.usuarios.any { it.idUsuario == id }

    // ---------------------------------------------------------------- PERSONA / USUARIO / VEHICULO

    @Synchronized
    fun insertarPersona(p: Persona) = atomico {
        unico(tablas.personas.any { it.idPersona == p.idPersona }, "PERSONA_PK")
        unico(tablas.personas.any { it.rut.equals(p.rut, ignoreCase = true) }, "PERSONA_RUT_UN")
        modificar { it.copy(personas = it.personas + p) }
    }

    @Synchronized
    fun insertarUsuario(u: Usuario) = atomico {
        unico(tablas.usuarios.any { it.idUsuario == u.idUsuario }, "USUARIO_PK")
        unico(tablas.usuarios.any { it.email == u.email }, "USUARIO_EMAIL_UN")
        unico(tablas.usuarios.any { it.idPersona == u.idPersona }, "USUARIO_PERSONA_UN")
        padre(existePersona(u.idPersona), "FK_USUARIO_PERSONA")
        modificar { it.copy(usuarios = it.usuarios + u) }
    }

    @Synchronized
    fun insertarVehiculo(v: Vehiculo) = atomico {
        unico(tablas.vehiculos.any { it.idVehiculo == v.idVehiculo }, "VEHICULO_PK")
        unico(tablas.vehiculos.any { it.patente.equals(v.patente, ignoreCase = true) }, "VEHICULO_PATENTE_UN")
        padre(existePersona(v.idPersonaPropietario), "FK_VEHICULO_PROPIETARIO")
        modificar { it.copy(vehiculos = it.vehiculos + v) }
    }

    // ---------------------------------------------------------------- PASO / CRUCE

    @Synchronized
    fun insertarPaso(p: PasoFronterizo) = atomico {
        unico(tablas.pasos.any { it.idPaso == p.idPaso }, "PASO_FRONTERIZO_PK")
        modificar { it.copy(pasos = it.pasos + p) }
    }

    /** INSERT INTO CRUCE_FRONTERIZO: dispara TRG_CRUCE_PLAZO_AI (AFTER INSERT, FOR EACH ROW). */
    @Synchronized
    fun insertarCruce(c: CruceFronterizo) = atomico {
        unico(tablas.cruces.any { it.idCruce == c.idCruce }, "CRUCE_FRONTERIZO_PK")
        padre(tablas.pasos.any { it.idPaso == c.idPaso }, "FK_CRUCE_PASO")
        c.idVehiculo?.let { padre(existeVehiculo(it), "FK_CRUCE_VEHICULO") }
        modificar { it.copy(cruces = it.cruces + c) }
        if (triggersHabilitados) Triggers.trgCrucePlazoAi(this, c)
    }

    @Synchronized
    fun insertarDetalleCruce(d: DetalleCrucePersona) = atomico {
        unico(tablas.detallesCruce.any { it.idCruce == d.idCruce && it.idPersona == d.idPersona }, "DETALLE_CRUCE_PK")
        padre(existeCruce(d.idCruce), "FK_DET_CRUCE_CRUCE")
        padre(existePersona(d.idPersona), "FK_DET_CRUCE_PERSONA")
        modificar { it.copy(detallesCruce = it.detallesCruce + d) }
    }

    @Synchronized
    fun insertarAutorizacion(a: AutorizacionMenor) = atomico {
        unico(tablas.autorizaciones.any { it.idAutorizacion == a.idAutorizacion }, "AUTORIZACION_MENOR_PK")
        padre(existePersona(a.idMenor), "FK_AUT_MENOR")
        padre(existePersona(a.idRepresentante), "FK_AUT_REPRESENTANTE")
        padre(tablas.documentos.any { it.idDocumento == a.idDocumento }, "FK_AUT_DOCUMENTO")
        modificar { it.copy(autorizaciones = it.autorizaciones + a) }
    }

    // ---------------------------------------------------------------- DECLARACIONES

    @Synchronized
    fun insertarCategoria(c: CategoriaItem) = atomico {
        unico(tablas.categorias.any { it.idCategoria == c.idCategoria }, "CATEGORIA_ITEM_PK")
        unico(tablas.categorias.any { it.nombre == c.nombre }, "CATEGORIA_ITEM_NOMBRE_UN")
        modificar { it.copy(categorias = it.categorias + c) }
    }

    @Synchronized
    fun insertarDeclaracion(d: Declaracion) = atomico {
        unico(tablas.declaraciones.any { it.idDeclaracion == d.idDeclaracion }, "DECLARACION_PK")
        padre(existeCruce(d.idCruce), "FK_DEC_CRUCE")
        modificar { it.copy(declaraciones = it.declaraciones + d) }
    }

    @Synchronized
    fun insertarItem(i: ItemDeclarado) = atomico {
        unico(tablas.items.any { it.idItem == i.idItem }, "ITEM_DECLARADO_PK")
        padre(tablas.declaraciones.any { it.idDeclaracion == i.idDeclaracion }, "FK_ITEM_DEC")
        padre(tablas.categorias.any { it.idCategoria == i.idCategoria }, "FK_ITEM_CATEGORIA")
        modificar { it.copy(items = it.items + i) }
    }

    // ---------------------------------------------------------------- PLAZO_ADMISION

    @Synchronized
    fun insertarPlazo(p: PlazoAdmision) = atomico {
        unico(tablas.plazos.any { it.idPlazo == p.idPlazo }, "PLAZO_ADMISION_PK")
        padre(existeVehiculo(p.idVehiculo), "FK_PLAZO_VEHICULO")
        padre(existeCruce(p.idCruceIngreso), "FK_PLAZO_CRUCE")
        modificar { it.copy(plazos = it.plazos + p) }
    }

    /** UPDATE PLAZO_ADMISION SET ... WHERE ...; devuelve SQL%ROWCOUNT. */
    @Synchronized
    fun actualizarPlazos(donde: (PlazoAdmision) -> Boolean, cambio: (PlazoAdmision) -> PlazoAdmision): Int = atomico {
        var filas = 0
        modificar { t ->
            t.copy(plazos = t.plazos.map { if (donde(it)) cambio(it).also { filas++ } else it })
        }
        filas
    }

    // ---------------------------------------------------------------- REPORTE

    @Synchronized
    fun insertarReporte(r: Reporte) = atomico {
        unico(tablas.reportes.any { it.idReporte == r.idReporte }, "REPORTE_PK")
        padre(existeUsuario(r.idUsuarioGenerador), "FK_REPORTE_USUARIO")
        modificar { it.copy(reportes = it.reportes + r) }
    }

    @Synchronized
    fun actualizarReporte(idReporte: Long, cambio: (Reporte) -> Reporte): Int = atomico {
        var filas = 0
        modificar { t ->
            t.copy(reportes = t.reportes.map { if (it.idReporte == idReporte) cambio(it).also { filas++ } else it })
        }
        filas
    }

    @Synchronized
    fun insertarReporteDetalle(d: ReporteDetalle) = atomico {
        unico(tablas.reporteDetalles.any { it.idDetalle == d.idDetalle }, "REPORTE_DETALLE_PK")
        padre(tablas.reportes.any { it.idReporte == d.idReporte }, "FK_REPORTE_DET_REPORTE")
        modificar { it.copy(reporteDetalles = it.reporteDetalles + d) }
    }

    // ---------------------------------------------------------------- AUDITORIA

    @Synchronized
    fun insertarAuditoria(a: Auditoria) = atomico {
        unico(tablas.auditorias.any { it.idAuditoria == a.idAuditoria }, "AUDITORIA_PK")
        padre(existeUsuario(a.idUsuarioAuditor), "FK_AUDITORIA_USUARIO")
        modificar { it.copy(auditorias = it.auditorias + a) }
    }

    @Synchronized
    fun insertarAuditoriaDetalle(d: AuditoriaDetalle) = atomico {
        unico(tablas.auditoriaDetalles.any { it.idDetalle == d.idDetalle }, "AUDITORIA_DETALLE_PK")
        padre(tablas.auditorias.any { it.idAuditoria == d.idAuditoria }, "FK_AUD_DET_AUDITORIA")
        padre(existeUsuario(d.idUsuario), "FK_AUD_DET_USUARIO")
        modificar { it.copy(auditoriaDetalles = it.auditoriaDetalles + d) }
    }

    // ---------------------------------------------------------------- DOCUMENTO (con triggers de auditoría)

    private fun validarDocumento(d: Documento, otros: List<Documento>) {
        unico(otros.any { it.idDocumento == d.idDocumento }, "DOCUMENTO_PK")
        unico(otros.any { it.numero == d.numero }, "DOCUMENTO_NUMERO_UN")
        val duenioValido = (d.idPersona != null && d.idVehiculo == null) || (d.idPersona == null && d.idVehiculo != null)
        if (!duenioValido) throw SmcftException(Errores.CHECK_VIOLADO, "restriccion de control (SMCFT.DOCUMENTO_DUENIO_CK) violada")
        d.idPersona?.let { padre(existePersona(it), "FK_DOC_PERSONA") }
        d.idVehiculo?.let { padre(existeVehiculo(it), "FK_DOC_VEHICULO") }
    }

    /** INSERT INTO DOCUMENTO: dispara TRG_DOCUMENTO_AUDIT (fila) y TRG_DOCUMENTO_SENTENCIA (sentencia). */
    @Synchronized
    fun insertarDocumento(d: Documento) = atomico {
        validarDocumento(d, tablas.documentos)
        modificar { it.copy(documentos = it.documentos + d) }
        if (triggersHabilitados) {
            Triggers.trgDocumentoAudit(this, Triggers.Accion.INSERT, old = null, new = d)
            Triggers.trgDocumentoSentencia(this, Triggers.Accion.INSERT)
        }
    }

    /** UPDATE DOCUMENTO SET ... WHERE ...; devuelve SQL%ROWCOUNT. */
    @Synchronized
    fun actualizarDocumentos(donde: (Documento) -> Boolean, cambio: (Documento) -> Documento): Int = atomico {
        val afectados = tablas.documentos.filter(donde)
        val nuevos = afectados.associate { it.idDocumento to cambio(it) }
        val resultado = tablas.documentos.map { nuevos[it.idDocumento] ?: it }
        nuevos.values.forEach { n -> validarDocumento(n, resultado.filter { it !== n }) }
        modificar { it.copy(documentos = resultado) }
        if (triggersHabilitados) {
            afectados.forEach { old -> Triggers.trgDocumentoAudit(this, Triggers.Accion.UPDATE, old, nuevos.getValue(old.idDocumento)) }
            Triggers.trgDocumentoSentencia(this, Triggers.Accion.UPDATE)
        }
        afectados.size
    }

    /** DELETE FROM DOCUMENTO WHERE ...; devuelve SQL%ROWCOUNT. */
    @Synchronized
    fun eliminarDocumentos(donde: (Documento) -> Boolean): Int = atomico {
        val afectados = tablas.documentos.filter(donde)
        val ids = afectados.map { it.idDocumento }.toSet()
        sinHijos(tablas.autorizaciones.any { it.idDocumento in ids }, "FK_AUT_DOCUMENTO")
        modificar { it.copy(documentos = it.documentos.filterNot { d -> d.idDocumento in ids }) }
        if (triggersHabilitados) {
            afectados.forEach { old -> Triggers.trgDocumentoAudit(this, Triggers.Accion.DELETE, old, new = null) }
            Triggers.trgDocumentoSentencia(this, Triggers.Accion.DELETE)
        }
        afectados.size
    }
}
