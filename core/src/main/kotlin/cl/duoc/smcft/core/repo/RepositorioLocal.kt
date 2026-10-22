package cl.duoc.smcft.core.repo

import cl.duoc.smcft.core.DatosPrueba
import cl.duoc.smcft.core.db.BaseDatos
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.model.AutorizacionMenor
import cl.duoc.smcft.core.model.Documento
import cl.duoc.smcft.core.model.Persona
import cl.duoc.smcft.core.model.Vehiculo
import cl.duoc.smcft.core.pkg.GestionFrontera
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Modo demostración: la misma lógica del informe sobre la base local del teléfono, sin conexión.
 * Cada operación hace COMMIT al terminar bien y ROLLBACK si falla.
 */
class RepositorioLocal(private val db: BaseDatos) : SmcftRepositorio {

    private val pkg = GestionFrontera(db)

    init {
        DatosPrueba.cargar(db)
    }

    @Synchronized
    private fun <T> transaccion(operacion: () -> T): T =
        try {
            operacion().also { db.commit() }
        } catch (e: Throwable) {
            db.rollback()
            throw e
        }

    private val t get() = db.tablas

    // ---------------------------------------------------------------- consultas

    override fun resumen() = Resumen(
        personas = t.personas.size,
        vehiculos = t.vehiculos.size,
        cruces = t.cruces.size,
        plazosActivos = t.plazos.count { it.estado == "ACTIVO" },
        plazosVencidos = pkg.fnTotalPlazosVencidos(),
        registrosAuditoria = t.auditorias.size + t.auditoriaDetalles.size,
    )

    override fun pasos() = t.pasos.sortedBy { it.nombre }.map { PasoInfo(it.idPaso, it.nombre, it.region, it.estado) }

    override fun categorias() = t.categorias.filter { it.activo }.sortedBy { it.nombre }
        .map { CategoriaInfo(it.idCategoria, it.nombre) }

    override fun personas() = t.personas.sortedWith(compareBy({ it.apellidos }, { it.nombres })).map { p ->
        PersonaInfo(
            idPersona = p.idPersona,
            rut = p.rut,
            nombres = p.nombres,
            apellidos = p.apellidos,
            nacionalidad = p.nacionalidad,
            fechaNacimiento = p.fechaNacimiento,
            edad = Period.between(p.fechaNacimiento, db.hoy()).years,
            autorizacionVigente = t.autorizaciones.any { it.idMenor == p.idPersona && it.estado == "VIGENTE" },
        )
    }

    override fun vehiculos() = t.vehiculos.sortedBy { it.patente }.map { v ->
        val propietario = t.personas.firstOrNull { it.idPersona == v.idPersonaPropietario }
        VehiculoInfo(v.idVehiculo, v.patente, v.marca, v.modelo, v.anio, v.tipoVehiculo, propietario?.nombreCompleto ?: "?")
    }

    override fun cruces() = t.cruces.sortedWith(compareByDescending<cl.duoc.smcft.core.model.CruceFronterizo> { it.fechaHora }.thenByDescending { it.idCruce })
        .map { c ->
            CruceInfo(
                idCruce = c.idCruce,
                nombrePaso = t.pasos.firstOrNull { it.idPaso == c.idPaso }?.nombre ?: "?",
                tipoMovimiento = c.tipoMovimiento,
                fechaHora = c.fechaHora,
                estado = c.estado,
                patente = c.idVehiculo?.let { id -> t.vehiculos.firstOrNull { it.idVehiculo == id }?.patente },
                cantViajeros = t.detallesCruce.count { it.idCruce == c.idCruce },
                valorDeclarado = pkg.fnValorTotalDeclarado(c.idCruce),
            )
        }

    override fun viajeros(idCruce: Long) = t.detallesCruce.filter { it.idCruce == idCruce }
        .sortedByDescending { it.rolViajero }
        .map { d ->
            val p = t.personas.first { it.idPersona == d.idPersona }
            ViajeroInfo(p.rut, p.nombreCompleto, d.rolViajero)
        }

    override fun items(idCruce: Long) = pkg.itemsCruce(idCruce).map { ItemInfo(it.descripcion, it.cantidad, it.valorUsd) }

    override fun plazos() = t.plazos.sortedBy { it.fechaLimite }.map { p ->
        PlazoInfo(
            idPlazo = p.idPlazo,
            patente = t.vehiculos.firstOrNull { it.idVehiculo == p.idVehiculo }?.patente ?: "?",
            idCruce = p.idCruceIngreso,
            fechaIngreso = p.fechaIngreso,
            fechaLimite = p.fechaLimite,
            tipo = p.tipo,
            estado = p.estado,
            diasRestantes = ChronoUnit.DAYS.between(db.hoy(), p.fechaLimite.toLocalDate()),
        )
    }

    override fun reportes() = t.reportes.sortedByDescending { it.idReporte }.map { r ->
        ReporteInfo(
            idReporte = r.idReporte,
            titulo = r.titulo,
            estado = r.estado,
            detalles = t.reporteDetalles.filter { it.idReporte == r.idReporte }.sortedBy { it.idDetalle }
                .map { ReporteDetalleInfo(it.descripcion, it.valor, it.unidad, it.categoria) },
        )
    }

    override fun documentos() = t.documentos.sortedBy { it.idDocumento }.map { d ->
        val titular = d.idPersona?.let { id -> t.personas.firstOrNull { it.idPersona == id }?.nombreCompleto }
            ?: d.idVehiculo?.let { id -> "Vehiculo " + t.vehiculos.firstOrNull { it.idVehiculo == id }?.patente }
            ?: "-"
        DocumentoInfo(d.idDocumento, d.tipoDocumento, d.numero, d.fechaEmision, d.fechaVencimiento, titular, d.estado)
    }

    override fun auditorias() = t.auditorias.sortedByDescending { it.idAuditoria }.map { a ->
        AuditoriaInfo(
            idAuditoria = a.idAuditoria,
            tipoAuditoria = a.tipoAuditoria,
            descripcion = a.descripcion,
            estado = a.estado,
            fechaInicio = a.fechaInicio,
            fechaCierre = a.fechaCierre,
            detalles = t.auditoriaDetalles.filter { it.idAuditoria == a.idAuditoria }.sortedByDescending { it.idDetalle }
                .map { AuditoriaDetalleInfo(it.idDetalle, it.accion, it.descripcion, it.resultado, it.fechaAccion) },
        )
    }

    // ---------------------------------------------------------------- procesos

    override fun registrarCruce(idPaso: Long, patente: String?, tipoMovimiento: String, ruts: List<String>, rutConductor: String?) =
        transaccion { pkg.spRegistrarCruce(idPaso, patente, tipoMovimiento, ruts, rutConductor) }

    override fun actualizarPlazos() = transaccion { pkg.spActualizarPlazosVencidos() }

    override fun generarReporte(desde: LocalDate, hasta: LocalDate, idUsuario: Long) =
        transaccion { pkg.spReporteCrucesPorPaso(desde.atStartOfDay(), hasta.atTime(23, 59, 59), idUsuario) }

    override fun registrarDeclaracion(idCruce: Long, tipo: String, idCategoria: Long, descripcion: String, cantidad: Int, valorUsd: Double) =
        transaccion { pkg.registrarDeclaracion(idCruce, tipo, listOf(Triple(idCategoria, descripcion, cantidad to valorUsd))) }

    override fun registrarDocumento(documento: NuevoDocumento) = transaccion {
        val id = db.nextval(Secuencias.DOCUMENTO)
        db.insertarDocumento(
            Documento(
                idDocumento = id,
                tipoDocumento = documento.tipoDocumento,
                numero = documento.numero,
                fechaEmision = documento.fechaEmision,
                fechaVencimiento = documento.fechaVencimiento,
                idPersona = documento.idPersona,
                idVehiculo = documento.idVehiculo,
                estado = "VIGENTE",
            )
        )
        id
    }

    override fun cambiarEstadoDocumento(idDocumento: Long, estado: String) =
        transaccion { db.actualizarDocumentos({ it.idDocumento == idDocumento }, { it.copy(estado = estado) }) }

    override fun marcarDocumentosVencidos() = transaccion {
        val hoy = db.hoy()
        db.actualizarDocumentos(
            donde = { d -> d.estado == "VIGENTE" && d.fechaVencimiento?.isBefore(hoy) == true },
            cambio = { it.copy(estado = "VENCIDO") },
        )
    }

    override fun eliminarDocumento(idDocumento: Long) = transaccion { db.eliminarDocumentos { it.idDocumento == idDocumento } }

    override fun registrarPersona(persona: NuevaPersona) = transaccion {
        val id = db.nextval(Secuencias.PERSONA)
        db.insertarPersona(
            Persona(id, persona.rut, persona.nombres, persona.apellidos, persona.nacionalidad.uppercase(), persona.fechaNacimiento)
        )
        id
    }

    override fun registrarVehiculo(vehiculo: NuevoVehiculo) = transaccion {
        val id = db.nextval(Secuencias.VEHICULO)
        db.insertarVehiculo(
            Vehiculo(id, vehiculo.patente.uppercase(), vehiculo.marca, vehiculo.modelo, vehiculo.anio, vehiculo.tipoVehiculo, vehiculo.idPropietario)
        )
        id
    }

    override fun registrarAutorizacion(idMenor: Long, idRepresentante: Long, numeroDocumento: String) = transaccion {
        val hoy = db.hoy()
        val idDoc = db.nextval(Secuencias.DOCUMENTO)
        db.insertarDocumento(Documento(idDoc, "AUTORIZACION_NOTARIAL", numeroDocumento, hoy, hoy.plusYears(1), idMenor, null, "VIGENTE"))
        val id = db.nextval(Secuencias.AUTORIZACION)
        db.insertarAutorizacion(AutorizacionMenor(id, idMenor, idRepresentante, idDoc, "NOTARIAL", hoy, "VIGENTE"))
        id
    }
}
