package cl.duoc.smcft.core.repo

import cl.duoc.smcft.core.seguridad.Sesion
import cl.duoc.smcft.core.seguridad.TipoUsuario
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * Repositorio conectado al esquema SMCFT en Oracle mediante el módulo REST de ORDS.
 * Toda la lógica (package, procedimientos, funciones y triggers) se ejecuta en Oracle.
 */
class RepositorioOracle(private val cliente: ClienteOrds) : SmcftRepositorio {

    constructor(baseUrl: String, clientId: String, clientSecret: String) :
        this(ClienteOrds(baseUrl, clientId, clientSecret))

    // ---------------------------------------------------------------- sesión

    override fun iniciarSesion(email: String, clave: String): Sesion {
        val r = cliente.procesoObjeto("POST", "auth/login", mapOf("email" to email.trim().lowercase(), "clave" to clave))
        return Sesion(
            idUsuario = r.entero("id_usuario"),
            nombre = r.texto("nombre"),
            email = email.trim().lowercase(),
            tipo = TipoUsuario.desdeCodigo(r.textoONulo("rol")),
        )
    }

    override fun usuarios() = cliente.lista("usuarios").map {
        UsuarioInfo(
            idUsuario = it.entero("id_usuario"),
            nombre = it.texto("nombre"),
            email = it.texto("email"),
            rol = it.textoONulo("rol") ?: "-",
            activo = it.texto("activo") == "S",
            ultimoAcceso = it.fechaHora("ultimo_acceso"),
        )
    }

    override fun accesos() = cliente.lista("accesos").map {
        AccesoInfo(
            idIntento = it.entero("id_intento"),
            email = it.textoONulo("email"),
            nombre = it.textoONulo("nombre"),
            exitoso = it.texto("exitoso") == "S",
            fecha = it.fechaHora("fecha_intento") ?: LocalDateTime.MIN,
        )
    }

    // ---------------------------------------------------------------- consultas

    override fun resumen(): Resumen = cliente.objeto("resumen").let {
        Resumen(
            personas = it.entero("personas").toInt(),
            vehiculos = it.entero("vehiculos").toInt(),
            cruces = it.entero("cruces").toInt(),
            plazosActivos = it.entero("plazos_activos").toInt(),
            plazosVencidos = it.entero("plazos_vencidos").toInt(),
            registrosAuditoria = it.entero("registros_auditoria").toInt(),
        )
    }

    override fun pasos() = cliente.lista("pasos").map {
        PasoInfo(it.entero("id_paso"), it.texto("nombre"), it.texto("region"), it.texto("estado"))
    }

    override fun categorias() = cliente.lista("categorias").map {
        CategoriaInfo(it.entero("id_categoria"), it.texto("nombre"))
    }

    override fun personas() = cliente.lista("personas").map {
        PersonaInfo(
            idPersona = it.entero("id_persona"),
            rut = it.texto("rut"),
            nombres = it.texto("nombres"),
            apellidos = it.texto("apellidos"),
            nacionalidad = it.texto("nacionalidad"),
            fechaNacimiento = it.fecha("fecha_nacimiento") ?: LocalDate.MIN,
            edad = it.entero("edad").toInt(),
            autorizacionVigente = it.entero("autorizacion_vigente") == 1L,
        )
    }

    override fun vehiculos() = cliente.lista("vehiculos").map {
        VehiculoInfo(
            idVehiculo = it.entero("id_vehiculo"),
            patente = it.texto("patente"),
            marca = it.texto("marca"),
            modelo = it.texto("modelo"),
            anio = it.entero("anio").toInt(),
            tipoVehiculo = it.texto("tipo_vehiculo"),
            propietario = it.texto("propietario"),
        )
    }

    override fun cruces() = cliente.lista("cruces").map {
        CruceInfo(
            idCruce = it.entero("id_cruce"),
            nombrePaso = it.texto("nombre_paso"),
            tipoMovimiento = it.texto("tipo_movimiento"),
            fechaHora = it.fechaHora("fecha_hora") ?: LocalDateTime.MIN,
            estado = it.texto("estado"),
            patente = it.textoONulo("patente"),
            cantViajeros = it.entero("cant_viajeros").toInt(),
            valorDeclarado = it.decimal("valor_declarado"),
        )
    }

    override fun viajeros(idCruce: Long) = cliente.lista("cruces/$idCruce/viajeros").map {
        ViajeroInfo(it.texto("rut"), it.texto("nombre"), it.texto("rol"))
    }

    override fun items(idCruce: Long) = cliente.lista("cruces/$idCruce/items").map {
        ItemInfo(it.texto("descripcion"), it.entero("cantidad").toInt(), it.decimal("valor_usd"))
    }

    override fun plazos() = cliente.lista("plazos").map {
        PlazoInfo(
            idPlazo = it.entero("id_plazo"),
            patente = it.texto("patente"),
            idCruce = it.entero("id_cruce"),
            fechaIngreso = it.fechaHora("fecha_ingreso") ?: LocalDateTime.MIN,
            fechaLimite = it.fechaHora("fecha_limite") ?: LocalDateTime.MIN,
            tipo = it.texto("tipo"),
            estado = it.texto("estado"),
            diasRestantes = it.entero("dias_restantes"),
        )
    }

    override fun reportes(): List<ReporteInfo> {
        val detalles = cliente.lista("reportes/detalles").groupBy({ it.entero("id_reporte") }) {
            ReporteDetalleInfo(it.texto("descripcion"), it.decimal("valor"), it.texto("unidad"), it.texto("categoria"))
        }
        return cliente.lista("reportes").map {
            val id = it.entero("id_reporte")
            ReporteInfo(id, it.texto("titulo"), it.texto("estado"), detalles[id].orEmpty())
        }
    }

    override fun documentos() = cliente.lista("documentos").map {
        DocumentoInfo(
            idDocumento = it.entero("id_documento"),
            tipoDocumento = it.texto("tipo_documento"),
            numero = it.texto("numero"),
            fechaEmision = it.fecha("fecha_emision"),
            fechaVencimiento = it.fecha("fecha_vencimiento"),
            titular = it.texto("titular"),
            estado = it.texto("estado"),
        )
    }

    override fun auditorias(): List<AuditoriaInfo> {
        val detalles = cliente.lista("auditorias/detalles").groupBy({ it.entero("id_auditoria") }) {
            AuditoriaDetalleInfo(
                idDetalle = it.entero("id_detalle"),
                accion = it.texto("accion"),
                descripcion = it.texto("descripcion"),
                resultado = it.texto("resultado"),
                fechaAccion = it.fechaHora("fecha_accion") ?: LocalDateTime.MIN,
            )
        }
        return cliente.lista("auditorias").map {
            val id = it.entero("id_auditoria")
            AuditoriaInfo(
                idAuditoria = id,
                tipoAuditoria = it.texto("tipo_auditoria"),
                descripcion = it.texto("descripcion"),
                estado = it.texto("estado"),
                fechaInicio = it.fechaHora("fecha_inicio") ?: LocalDateTime.MIN,
                fechaCierre = it.fechaHora("fecha_cierre"),
                detalles = detalles[id].orEmpty(),
            )
        }
    }

    // ---------------------------------------------------------------- procesos

    override fun registrarCruce(idPaso: Long, patente: String?, tipoMovimiento: String, ruts: List<String>, rutConductor: String?) =
        cliente.proceso(
            "POST", "cruces",
            mapOf(
                "id_paso" to idPaso,
                "patente" to patente?.takeIf { it.isNotBlank() },
                "tipo_movimiento" to tipoMovimiento,
                "ruts" to ruts.joinToString(","),
                "rut_conductor" to rutConductor,
            ),
            resultado = "id_cruce",
        )

    override fun actualizarPlazos() =
        cliente.proceso("POST", "procesos/actualizar-plazos", resultado = "filas").toInt()

    override fun generarReporte(desde: LocalDate, hasta: LocalDate, idUsuario: Long) =
        cliente.proceso(
            "POST", "reportes",
            mapOf("desde" to desde.toString(), "hasta" to hasta.toString(), "id_usuario" to idUsuario),
            resultado = "id_reporte",
        )

    override fun registrarDeclaracion(idCruce: Long, tipo: String, idCategoria: Long, descripcion: String, cantidad: Int, valorUsd: Double) =
        cliente.proceso(
            "POST", "declaraciones",
            mapOf(
                "id_cruce" to idCruce,
                "tipo" to tipo,
                "id_categoria" to idCategoria,
                "descripcion" to descripcion,
                "cantidad" to cantidad,
                // como texto con punto decimal, independiente del idioma de la sesión Oracle
                "valor_usd" to String.format(Locale.US, "%.2f", valorUsd),
            ),
            resultado = "id_declaracion",
        )

    override fun registrarDocumento(documento: NuevoDocumento) =
        cliente.proceso(
            "POST", "documentos",
            mapOf(
                "tipo_documento" to documento.tipoDocumento,
                "numero" to documento.numero,
                "fecha_emision" to documento.fechaEmision?.toString(),
                "fecha_vencimiento" to documento.fechaVencimiento?.toString(),
                "id_persona" to documento.idPersona,
                "id_vehiculo" to documento.idVehiculo,
            ),
            resultado = "id_documento",
        )

    override fun cambiarEstadoDocumento(idDocumento: Long, estado: String) =
        cliente.proceso("PUT", "documentos/$idDocumento", mapOf("estado" to estado), resultado = "filas").toInt()

    override fun marcarDocumentosVencidos() =
        cliente.proceso("POST", "procesos/documentos-vencidos", resultado = "filas").toInt()

    override fun eliminarDocumento(idDocumento: Long) =
        cliente.proceso("DELETE", "documentos/$idDocumento", resultado = "filas").toInt()

    override fun registrarPersona(persona: NuevaPersona) =
        cliente.proceso(
            "POST", "personas",
            mapOf(
                "rut" to persona.rut,
                "nombres" to persona.nombres,
                "apellidos" to persona.apellidos,
                "nacionalidad" to persona.nacionalidad,
                "fecha_nacimiento" to persona.fechaNacimiento.toString(),
            ),
            resultado = "id_persona",
        )

    override fun registrarVehiculo(vehiculo: NuevoVehiculo) =
        cliente.proceso(
            "POST", "vehiculos",
            mapOf(
                "patente" to vehiculo.patente,
                "marca" to vehiculo.marca,
                "modelo" to vehiculo.modelo,
                "anio" to vehiculo.anio,
                "tipo_vehiculo" to vehiculo.tipoVehiculo,
                "id_propietario" to vehiculo.idPropietario,
            ),
            resultado = "id_vehiculo",
        )

    override fun registrarAutorizacion(idMenor: Long, idRepresentante: Long, numeroDocumento: String) =
        cliente.proceso(
            "POST", "autorizaciones",
            mapOf("id_menor" to idMenor, "id_representante" to idRepresentante, "numero_documento" to numeroDocumento),
            resultado = "id_autorizacion",
        )
}
