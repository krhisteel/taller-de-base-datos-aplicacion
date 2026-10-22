package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.PersonaInfo
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.BarraBusqueda
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible

private const val MAX_VIAJEROS = 10 // t_ruts_varray IS VARRAY(10)

@Composable
fun PantallaNuevoCruce(vm: SmcftViewModel, datos: Datos, alTerminar: () -> Unit) {
    val c = coloresEstado
    val pasosActivos = datos.pasos.filter { it.estado == "ACTIVO" }
    var idPaso by remember { mutableStateOf(pasosActivos.firstOrNull()?.idPaso) }
    var movimiento by remember { mutableStateOf("SALIDA") }
    var patente by remember { mutableStateOf<String?>(null) }
    val viajeros = remember { mutableStateListOf<String>() }
    var conductor by remember { mutableStateOf<String?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    val vehiculo = datos.vehiculos.firstOrNull { it.patente == patente }
    val seleccionados = datos.personas.filter { it.rut in viajeros }
    val menoresSinAutorizacion = seleccionados.filter { it.esMenor && !it.autorizacionVigente }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Paso
            Paso(1, "Paso fronterizo", "¿Por dónde cruza?") {
                pasosActivos.forEach { paso ->
                    Opcion(
                        seleccionada = paso.idPaso == idPaso,
                        icono = Icons.Outlined.Place,
                        titulo = paso.nombre,
                        subtitulo = "Región de ${paso.region}",
                        alPulsar = { idPaso = paso.idPaso },
                    )
                }
            }

            // 2. Movimiento
            Paso(2, "Tipo de movimiento", null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("ENTRADA" to Icons.AutoMirrored.Outlined.Login, "SALIDA" to Icons.AutoMirrored.Outlined.Logout)
                        .forEachIndexed { i, (valor, icono) ->
                            SegmentedButton(
                                selected = movimiento == valor,
                                onClick = { movimiento = valor },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                                icon = { Icon(icono, null, Modifier.size(18.dp)) },
                            ) { Text(legible(valor)) }
                        }
                }
            }

            // 3. Vehículo
            Paso(3, "Vehículo", "Elija \"Peatonal\" si cruza sin vehículo") {
                Opcion(
                    seleccionada = patente == null,
                    icono = Icons.Outlined.DirectionsWalk,
                    titulo = "Peatonal",
                    subtitulo = "Sin vehículo",
                    alPulsar = { patente = null; conductor = null },
                )
                datos.vehiculos.forEach { v ->
                    Opcion(
                        seleccionada = patente == v.patente,
                        titulo = "${v.marca} ${v.modelo} ${v.anio}",
                        subtitulo = "Propietario: ${v.propietario}",
                        alPulsar = { patente = v.patente },
                        inicio = { Patente(v.patente) },
                        fin = { if (v.tipoVehiculo == "DIPLOMATICO") Pildora("Diplomático", c.info, c.infoSuave) },
                    )
                }
            }

            // 4. Viajeros
            Paso(4, "Viajeros", "Seleccione hasta $MAX_VIAJEROS personas. ${if (patente != null) "Toque la estrella para marcar al conductor." else ""}") {
                BarraBusqueda(busqueda, { busqueda = it }, "Buscar por nombre o RUT")
                datos.personas
                    .filter { busqueda.isBlank() || it.nombreCompleto.contains(busqueda, true) || it.rut.contains(busqueda, true) }
                    .forEach { p ->
                        FilaViajero(
                            persona = p,
                            marcado = p.rut in viajeros,
                            esConductor = conductor == p.rut,
                            puedeConducir = patente != null && !p.esMenor,
                            alMarcar = { activo ->
                                if (activo) {
                                    if (viajeros.size < MAX_VIAJEROS) viajeros += p.rut
                                } else {
                                    viajeros -= p.rut
                                    if (conductor == p.rut) conductor = null
                                }
                            },
                            alConductor = {
                                if (p.rut !in viajeros && viajeros.size < MAX_VIAJEROS) viajeros += p.rut
                                conductor = if (conductor == p.rut) null else p.rut
                            },
                        )
                    }
            }

            // Resumen de lo que hará Oracle
            Tarjeta(titulo = "Antes de confirmar", subtitulo = "Revise el resumen del registro", icono = Icons.Outlined.Info) {
                val paso = datos.pasos.firstOrNull { it.idPaso == idPaso }?.nombre ?: "—"
                Texto2("${legible(movimiento)} por $paso · ${if (vehiculo != null) "vehículo ${vehiculo.patente}" else "peatonal"} · ${viajeros.size} viajero(s)")
                Aviso(
                    ok = viajeros.isNotEmpty(),
                    texto = if (viajeros.isEmpty()) "Seleccione al menos un viajero." else "${viajeros.size} viajero(s) listo(s) para registrar.",
                )
                if (menoresSinAutorizacion.isNotEmpty()) {
                    Aviso(
                        ok = false,
                        texto = "${menoresSinAutorizacion.joinToString { it.nombres }}: menor sin autorización de viaje. " +
                            "El sistema no permitirá el cruce hasta registrar la autorización en Viajeros.",
                    )
                }
                if (vehiculo != null && movimiento == "SALIDA") {
                    val dias = if (vehiculo.tipoVehiculo == "DIPLOMATICO") 90 else 180
                    Aviso(ok = true, texto = "Se creará automáticamente un plazo de admisión temporal de $dias días para el vehículo.")
                }
                if (vehiculo != null && conductor == null && viajeros.isNotEmpty()) {
                    Aviso(ok = null, texto = "No marcó quién conduce: todos quedarán como acompañantes.")
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
            BotonPrincipal(
                texto = "Registrar cruce",
                icono = Icons.Outlined.Save,
                habilitado = idPaso != null && viajeros.isNotEmpty(),
                guardando = guardando,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                alPulsar = {
                    guardando = true
                    vm.ejecutar(
                        exito = { id: Long -> "Cruce N° $id registrado correctamente" },
                        alTerminar = { ok -> guardando = false; if (ok) alTerminar() },
                    ) { registrarCruce(idPaso!!, patente, movimiento, viajeros.toList(), conductor) }
                },
            )
        }
    }
}

@Composable
private fun Paso(numero: Int, titulo: String, ayuda: String?, contenido: @Composable () -> Unit) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Text("$numero", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (!ayuda.isNullOrBlank()) Texto2(ayuda)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { contenido() }
    }
}

/** Tarjeta seleccionable con borde resaltado cuando está elegida. */
@Composable
private fun Opcion(
    seleccionada: Boolean,
    titulo: String,
    subtitulo: String,
    alPulsar: () -> Unit,
    icono: ImageVector? = null,
    inicio: (@Composable () -> Unit)? = null,
    fin: (@Composable () -> Unit)? = null,
) {
    val color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (seleccionada) 2.dp else 1.dp, color),
        color = if (seleccionada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = alPulsar),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) Icon(icono, null, tint = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            inicio?.invoke()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge)
                Texto2(subtitulo)
            }
            fin?.invoke()
            if (seleccionada) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FilaViajero(
    persona: PersonaInfo,
    marcado: Boolean,
    esConductor: Boolean,
    puedeConducir: Boolean,
    alMarcar: (Boolean) -> Unit,
    alConductor: () -> Unit,
) {
    val c = coloresEstado
    Column {
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { alMarcar(!marcado) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = marcado, onCheckedChange = alMarcar)
            Avatar(persona.nombreCompleto, 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(persona.nombreCompleto, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Texto2("${persona.rut} · ${persona.edad} años")
                    if (persona.esMenor) {
                        if (persona.autorizacionVigente) Pildora("Menor autorizado", c.exito, c.exitoSuave)
                        else Pildora("Menor sin autorización", c.peligro, c.peligroSuave)
                    }
                }
            }
            if (puedeConducir) {
                IconButton(onClick = alConductor) {
                    Icon(
                        if (esConductor) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        "Conductor",
                        tint = if (esConductor) c.alerta else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/** Línea del resumen: verde si está bien, roja si fallará y gris si es solo un aviso. */
@Composable
private fun Aviso(ok: Boolean?, texto: String) {
    val c = coloresEstado
    val (icono, color) = when (ok) {
        true -> Icons.Outlined.CheckCircle to c.exito
        false -> Icons.Outlined.ErrorOutline to c.peligro
        null -> Icons.Outlined.Info to c.neutro
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
