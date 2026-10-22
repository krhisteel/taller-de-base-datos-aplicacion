package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.PlazoInfo
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.Indicador
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible
import cl.duoc.smcft.ui.componentes.texto
import java.time.temporal.ChronoUnit

private enum class FiltroPlazo(val texto: String) { TODOS("Todos"), ACTIVOS("Activos"), POR_VENCER("Por vencer"), VENCIDOS("Vencidos") }

private fun PlazoInfo.vencido() = estado == "VENCIDO" || (estado != "CERRADO" && diasRestantes < 0)

@Composable
fun PantallaPlazos(vm: SmcftViewModel, datos: Datos) {
    val c = coloresEstado
    var filtro by rememberSaveable { mutableStateOf(FiltroPlazo.TODOS) }
    var actualizando by remember { mutableStateOf(false) }

    val plazos = datos.plazos
    val lista = plazos.filter {
        when (filtro) {
            FiltroPlazo.TODOS -> true
            FiltroPlazo.ACTIVOS -> it.estado == "ACTIVO" && it.diasRestantes >= 0
            FiltroPlazo.POR_VENCER -> it.estado == "ACTIVO" && it.diasRestantes in 0..30
            FiltroPlazo.VENCIDOS -> it.vencido()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Indicador("Activos", plazos.count { it.estado == "ACTIVO" && it.diasRestantes >= 0 }.toString(),
                    Icons.Outlined.EventAvailable, c.exito, c.exitoSuave, Modifier.weight(1f))
                Indicador("Por vencer (≤30 días)", plazos.count { it.estado == "ACTIVO" && it.diasRestantes in 0..30 }.toString(),
                    Icons.Outlined.HourglassBottom, c.alerta, c.alertaSuave, Modifier.weight(1f))
                Indicador("Vencidos (función)", datos.resumen.plazosVencidos.toString(),
                    Icons.Outlined.EventBusy, c.peligro, c.peligroSuave, Modifier.weight(1f))
            }
        }
        item {
            Tarjeta(titulo = "Admisión temporal de vehículos", subtitulo = "90 días para diplomáticos y 180 días para el resto", icono = Icons.Outlined.Schedule) {
                Texto2("Los plazos se crean solos con el trigger TRG_CRUCE_PLAZO_AI al registrar la salida de un vehículo. " +
                    "El procedimiento SP_ACTUALIZAR_PLAZOS marca como VENCIDO los plazos activos cuya fecha límite ya pasó.")
                BotonPrincipal("Ejecutar SP_ACTUALIZAR_PLAZOS", icono = Icons.Outlined.Update, guardando = actualizando, alPulsar = {
                    actualizando = true
                    vm.ejecutar(
                        exito = { n: Int -> if (n == 0) "No había plazos por actualizar" else "$n plazo(s) marcado(s) como VENCIDO" },
                        alTerminar = { actualizando = false },
                    ) { actualizarPlazos() }
                })
            }
        }
        item { Filtros(FiltroPlazo.entries, filtro, { it.texto }, { filtro = it }) }

        if (lista.isEmpty()) {
            item {
                EstadoVacio(
                    Icons.Outlined.Schedule,
                    if (plazos.isEmpty()) "Sin plazos de admisión" else "Sin plazos en este filtro",
                    if (plazos.isEmpty()) "Registre la SALIDA de un vehículo para que el trigger cree su plazo." else "Pruebe con otro filtro.",
                )
            }
        }
        items(lista, key = { it.idPlazo }) { FilaPlazo(it) }
    }
}

@Composable
private fun FilaPlazo(p: PlazoInfo) {
    val c = coloresEstado
    val total = ChronoUnit.DAYS.between(p.fechaIngreso.toLocalDate(), p.fechaLimite.toLocalDate()).coerceAtLeast(1)
    val transcurrido = (total - p.diasRestantes).coerceIn(0, total)
    val color = when {
        p.vencido() -> c.peligro
        p.diasRestantes <= 30 -> c.alerta
        else -> c.exito
    }
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Patente(p.patente, grande = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(legible(p.tipo), style = MaterialTheme.typography.titleSmall)
                Texto2("Plazo #${p.idPlazo} · cruce #${p.idCruce}")
            }
            ChipEstado(p.estado)
        }
        LinearProgressIndicator(
            progress = { transcurrido.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Texto2("Desde ${p.fechaIngreso.toLocalDate().texto()}")
                Texto2("Hasta ${p.fechaLimite.toLocalDate().texto()}")
            }
            Text(
                when {
                    p.diasRestantes < 0 -> "Vencido hace ${-p.diasRestantes} d"
                    p.diasRestantes == 0L -> "Vence hoy"
                    else -> "${p.diasRestantes} días"
                },
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
        Spacer(Modifier.height(0.dp))
    }
}
