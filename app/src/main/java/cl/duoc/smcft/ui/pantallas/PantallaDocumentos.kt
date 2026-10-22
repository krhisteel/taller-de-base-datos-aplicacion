package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.DocumentoInfo
import cl.duoc.smcft.core.repo.NuevoDocumento
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.puede
import cl.duoc.smcft.ui.componentes.BarraBusqueda
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Campo
import cl.duoc.smcft.ui.componentes.CampoFecha
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.DialogoConfirmar
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible
import cl.duoc.smcft.ui.componentes.texto
import java.time.LocalDate

private val ESTADOS = listOf("TODOS", "VIGENTE", "VENCIDO", "ANULADO", "REVISADO")

private fun iconoDocumento(tipo: String): ImageVector = when {
    tipo.contains("PASAPORTE") -> Icons.Outlined.Public
    tipo.contains("CEDULA") -> Icons.Outlined.Badge
    tipo.contains("AUTORIZACION") -> Icons.Outlined.Gavel
    tipo.contains("CIRCULACION") || tipo.contains("PADRON") || tipo.contains("SEGURO") -> Icons.Outlined.DirectionsCar
    else -> Icons.Outlined.Description
}

@Composable
fun PantallaDocumentos(vm: SmcftViewModel, datos: Datos) {
    val c = coloresEstado
    var busqueda by rememberSaveable { mutableStateOf("") }
    var estado by rememberSaveable { mutableStateOf("TODOS") }
    var nuevo by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<DocumentoInfo?>(null) }
    var confirmarMasivo by remember { mutableStateOf(false) }
    val gestiona = puede(Permiso.GESTIONAR_DOCUMENTOS)

    val hoy = LocalDate.now()
    val vencibles = datos.documentos.count { it.estado == "VIGENTE" && it.fechaVencimiento?.isBefore(hoy) == true }
    val lista = datos.documentos
        .filter { estado == "TODOS" || it.estado == estado }
        .filter { d ->
            busqueda.isBlank() || d.numero.contains(busqueda, true) || d.titular.contains(busqueda, true) ||
                d.tipoDocumento.contains(busqueda.replace(' ', '_'), true)
        }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BarraBusqueda(busqueda, { busqueda = it }, "Buscar por número, titular o tipo")
                    Filtros(ESTADOS, estado, { if (it == "TODOS") "Todos" else legible(it) }, { estado = it })
                }
            }
            item {
                if (gestiona) {
                    Tarjeta(titulo = "Documentos vencidos", subtitulo = "$vencibles vigente(s) con fecha de vencimiento pasada", icono = Icons.Outlined.FactCheck) {
                        Texto2("Actualiza su estado de una sola vez. Cada cambio queda registrado en la auditoría.")
                        cl.duoc.smcft.ui.componentes.Boton(
                            "Marcar como vencidos ($vencibles)", { confirmarMasivo = true },
                            variante = cl.duoc.smcft.ui.componentes.VarianteBoton.SECUNDARIO,
                            icono = Icons.Outlined.EventBusy, habilitado = vencibles > 0,
                        )
                    }
                } else {
                    cl.duoc.smcft.ui.componentes.AvisoSoloLectura("Los documentos los administran la PDI y el administrador.")
                }
            }
            if (lista.isEmpty()) {
                item { EstadoVacio(Icons.Outlined.SearchOff, "Sin documentos", "No hay documentos que coincidan con la búsqueda o el filtro.") }
            }
            items(lista, key = { it.idDocumento }) { d ->
                FilaDocumento(
                    d,
                    gestiona = gestiona,
                    alCambiarEstado = { nuevoEstado ->
                        vm.ejecutar({ _: Int -> "Documento ${d.numero}: ${legible(nuevoEstado)}" }) { cambiarEstadoDocumento(d.idDocumento, nuevoEstado) }
                    },
                    alEliminar = { aEliminar = d },
                )
            }
        }
        if (gestiona) {
            ExtendedFloatingActionButton(
                onClick = { nuevo = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Nuevo documento") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    if (nuevo) NuevoDocumentoHoja(vm, datos) { nuevo = false }

    aEliminar?.let { d ->
        DialogoConfirmar(
            titulo = "¿Eliminar documento?",
            mensaje = "Se eliminará ${legible(d.tipoDocumento)} ${d.numero} de ${d.titular}. La eliminación quedará registrada en la auditoría.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = { vm.ejecutar({ _: Int -> "Documento ${d.numero} eliminado" }) { eliminarDocumento(d.idDocumento) } },
            alCerrar = { aEliminar = null },
        )
    }

    if (confirmarMasivo) {
        DialogoConfirmar(
            titulo = "¿Marcar documentos vencidos?",
            mensaje = "Se marcarán como vencidos los $vencibles documento(s) vigentes cuya fecha de vencimiento ya pasó.",
            textoConfirmar = "Ejecutar",
            alConfirmar = { vm.ejecutar({ n: Int -> "Se marcaron $n documento(s) como vencidos" }) { marcarDocumentosVencidos() } },
            alCerrar = { confirmarMasivo = false },
        )
    }
}

@Composable
private fun FilaDocumento(d: DocumentoInfo, gestiona: Boolean, alCambiarEstado: (String) -> Unit, alEliminar: () -> Unit) {
    val c = coloresEstado
    var menu by remember { mutableStateOf(false) }
    val vencido = d.fechaVencimiento?.isBefore(LocalDate.now()) == true
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(iconoDocumento(d.tipoDocumento), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(legible(d.tipoDocumento), style = MaterialTheme.typography.titleSmall)
                Texto2("N° ${d.numero} · ${d.titular}")
            }
            ChipEstado(d.estado)
            if (gestiona) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Opciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(
                        Triple("VIGENTE", "Marcar vigente", Icons.Outlined.CheckCircle),
                        Triple("VENCIDO", "Marcar vencido", Icons.Outlined.EventBusy),
                        Triple("ANULADO", "Anular", Icons.Outlined.Block),
                    ).filter { it.first != d.estado }.forEach { (estado, texto, icono) ->
                        DropdownMenuItem(text = { Text(texto) }, leadingIcon = { Icon(icono, null) }, onClick = { menu = false; alCambiarEstado(estado) })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; alEliminar() },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Texto2("Emisión")
                Text(d.fechaEmision?.texto() ?: "—", style = MaterialTheme.typography.bodyMedium)
            }
            Column(Modifier.weight(1f)) {
                Texto2("Vencimiento")
                Text(
                    d.fechaVencimiento?.texto() ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (vencido) c.peligro else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoDocumentoHoja(vm: SmcftViewModel, datos: Datos, alCerrar: () -> Unit) {
    var paraVehiculo by remember { mutableStateOf(false) }
    var idPersona by remember { mutableStateOf(datos.personas.firstOrNull()?.idPersona) }
    var idVehiculo by remember { mutableStateOf(datos.vehiculos.firstOrNull()?.idVehiculo) }
    var tipo by remember { mutableStateOf("PASAPORTE") }
    var numero by remember { mutableStateOf("") }
    var emision by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var vencimiento by remember { mutableStateOf<LocalDate?>(LocalDate.now().plusYears(5)) }
    var guardando by remember { mutableStateOf(false) }

    val tipos = if (paraVehiculo) listOf("PERMISO_CIRCULACION", "PADRON", "SEGURO") else listOf("PASAPORTE", "CEDULA", "AUTORIZACION_NOTARIAL")
    val fechasValidas = emision == null || vencimiento == null || !vencimiento!!.isBefore(emision)

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Nuevo documento", style = MaterialTheme.typography.headlineSmall)
            Filtros(listOf(false, true), paraVehiculo, { if (it) "De un vehículo" else "De una persona" }, {
                paraVehiculo = it
                tipo = if (it) "PERMISO_CIRCULACION" else "PASAPORTE"
            })
            if (paraVehiculo) {
                Texto2("Vehículo")
                Filtros(datos.vehiculos.map { it.idVehiculo }, idVehiculo ?: -1, { id -> datos.vehiculos.first { it.idVehiculo == id }.patente }, { idVehiculo = it })
            } else {
                Texto2("Titular")
                Filtros(datos.personas.map { it.idPersona }, idPersona ?: -1, { id -> datos.personas.first { it.idPersona == id }.nombreCompleto }, { idPersona = it })
            }
            Texto2("Tipo de documento")
            Filtros(tipos, tipo, { legible(it) }, { tipo = it })
            Campo("Número", numero, { numero = it.uppercase() }, icono = if (paraVehiculo) Icons.Outlined.DirectionsCar else Icons.Outlined.Person,
                ayuda = "Cada número de documento debe ser único")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoFecha("Emisión", emision, { emision = it }, Modifier.weight(1f))
                CampoFecha("Vencimiento", vencimiento, { vencimiento = it }, Modifier.weight(1f))
            }
            if (!fechasValidas) Text("El vencimiento no puede ser anterior a la emisión.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            BotonPrincipal(
                "Guardar documento",
                icono = Icons.Outlined.Save,
                habilitado = numero.isNotBlank() && fechasValidas && (if (paraVehiculo) idVehiculo != null else idPersona != null),
                guardando = guardando,
                alPulsar = {
                    guardando = true
                    vm.ejecutar(
                        exito = { id: Long -> "Documento registrado (ID $id)" },
                        alTerminar = { ok -> guardando = false; if (ok) alCerrar() },
                    ) {
                        registrarDocumento(
                            NuevoDocumento(
                                tipoDocumento = tipo,
                                numero = numero.trim(),
                                fechaEmision = emision,
                                fechaVencimiento = vencimiento,
                                idPersona = if (paraVehiculo) null else idPersona,
                                idVehiculo = if (paraVehiculo) idVehiculo else null,
                            )
                        )
                    }
                },
            )
        }
    }
}
