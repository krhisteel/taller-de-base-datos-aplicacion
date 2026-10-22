package cl.duoc.smcft.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Icon
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
import cl.duoc.smcft.core.repo.AuditoriaDetalleInfo
import cl.duoc.smcft.core.repo.AuditoriaInfo
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Indicador
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.coloresDe
import cl.duoc.smcft.ui.componentes.legible
import cl.duoc.smcft.ui.componentes.texto

private enum class FiltroAuditoria(val texto: String) { TODAS("Todas"), FILA("Por documento (fila)"), SENTENCIA("Por instrucción (sentencia)") }

@Composable
fun PantallaAuditoria(datos: Datos) {
    val c = coloresEstado
    var filtro by rememberSaveable { mutableStateOf(FiltroAuditoria.TODAS) }
    val fila = datos.auditorias.filter { it.tipoAuditoria != "TRIGGER_SENTENCIA" }
    val sentencia = datos.auditorias.filter { it.tipoAuditoria == "TRIGGER_SENTENCIA" }
    val lista = when (filtro) {
        FiltroAuditoria.TODAS -> datos.auditorias
        FiltroAuditoria.FILA -> fila
        FiltroAuditoria.SENTENCIA -> sentencia
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Indicador("Detalles de fila (TRG_DOCUMENTO_AUDIT)", fila.sumOf { it.detalles.size }.toString(),
                    Icons.Outlined.TableRows, c.info, c.infoSuave, Modifier.weight(1f))
                Indicador("Instrucciones (TRG_DOCUMENTO_SENTENCIA)", sentencia.size.toString(),
                    Icons.Outlined.Layers, c.alerta, c.alertaSuave, Modifier.weight(1f))
            }
        }
        item { Filtros(FiltroAuditoria.entries, filtro, { it.texto }, { filtro = it }) }
        if (lista.isEmpty()) {
            item { EstadoVacio(Icons.Outlined.HistoryEdu, "Sin registros", "Modifique un documento para que los triggers generen auditoría.") }
        }
        items(lista, key = { it.idAuditoria }) { TarjetaAuditoria(it) }
    }
}

@Composable
private fun TarjetaAuditoria(a: AuditoriaInfo) {
    val c = coloresEstado
    var abierto by remember { mutableStateOf(a.tipoAuditoria != "TRIGGER_SENTENCIA") }
    val sentencia = a.tipoAuditoria == "TRIGGER_SENTENCIA"
    Tarjeta(relleno = 14.dp, alPulsar = if (a.detalles.isNotEmpty()) ({ abierto = !abierto }) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(
                if (sentencia) Icons.Outlined.Layers else Icons.Outlined.TableRows,
                if (sentencia) c.alerta else c.info,
                if (sentencia) c.alertaSuave else c.infoSuave,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("#${a.idAuditoria} · ${legible(a.tipoAuditoria)}", style = MaterialTheme.typography.titleSmall)
                Texto2(a.descripcion)
                Texto2(a.fechaInicio.texto())
            }
            ChipEstado(a.estado)
            if (a.detalles.isNotEmpty()) Icon(if (abierto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        if (a.detalles.isNotEmpty()) {
            Texto2("${a.detalles.size} operación(es) registradas")
            AnimatedVisibility(abierto) {
                Column { a.detalles.forEachIndexed { i, d -> PuntoLineaTiempo(d, esUltimo = i == a.detalles.lastIndex) } }
            }
        }
    }
}

/** Un evento de la línea de tiempo: punto de color, línea vertical y descripción. */
@Composable
private fun PuntoLineaTiempo(d: AuditoriaDetalleInfo, esUltimo: Boolean) {
    val col = coloresDe(d.accion)
    val icono = when (d.accion) {
        "INSERT" -> Icons.Outlined.AddCircleOutline
        "DELETE" -> Icons.Outlined.DeleteOutline
        else -> Icons.Outlined.Edit
    }
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(col.fondo), contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = col.texto, modifier = Modifier.size(16.dp))
            }
            if (!esUltimo) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.accion, style = MaterialTheme.typography.labelLarge, color = col.texto, modifier = Modifier.weight(1f))
                Texto2(d.fechaAccion.texto())
            }
            Text(d.descripcion, style = MaterialTheme.typography.bodySmall)
        }
    }
}
