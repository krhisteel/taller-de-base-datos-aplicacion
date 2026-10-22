package cl.duoc.smcft.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.ReporteInfo
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.BarraValor
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.CampoFecha
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion
import cl.duoc.smcft.ui.componentes.numero
import cl.duoc.smcft.ui.componentes.usd
import java.time.LocalDate

private enum class Rango(val texto: String, val dias: Long?) { HOY("Hoy", 0), SEMANA("7 días", 6), MES("30 días", 29), PERSONALIZADO("Personalizado", null) }

@Composable
fun PantallaReportes(vm: SmcftViewModel, datos: Datos, ajustes: Ajustes) {
    val hoy = LocalDate.now()
    var rango by remember { mutableStateOf(Rango.MES) }
    var desde by remember { mutableStateOf(hoy.minusDays(29)) }
    var hasta by remember { mutableStateOf(hoy) }
    var generando by remember { mutableStateOf(false) }
    var abierto by remember { mutableStateOf(datos.reportes.firstOrNull()?.idReporte) }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Tarjeta(titulo = "Generar reporte", subtitulo = "Procedimiento SP_REPORTE_CRUCES", icono = Icons.Outlined.QueryStats) {
                Texto2("Recorre los pasos activos con sus cruces y viajeros del período y guarda tres indicadores por paso.")
                Filtros(Rango.entries, rango, { it.texto }, {
                    rango = it
                    it.dias?.let { d -> desde = hoy.minusDays(d); hasta = hoy }
                })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoFecha("Desde", desde, { desde = it; rango = Rango.PERSONALIZADO }, Modifier.weight(1f))
                    CampoFecha("Hasta", hasta, { hasta = it; rango = Rango.PERSONALIZADO }, Modifier.weight(1f))
                }
                if (desde.isAfter(hasta)) {
                    Text(
                        "La fecha inicial es posterior a la final: la validación privada validar_rango lo rechazará (error -20020).",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    )
                }
                BotonPrincipal("Generar reporte", icono = Icons.Outlined.PlayArrow, guardando = generando, alPulsar = {
                    generando = true
                    vm.ejecutar(
                        exito = { id: Long -> abierto = id; "Reporte #$id generado" },
                        alTerminar = { generando = false },
                    ) { generarReporte(desde, hasta, ajustes.idUsuarioReportes) }
                })
            }
        }

        item { TituloSeccion("Reportes generados (${datos.reportes.size})") }
        if (datos.reportes.isEmpty()) {
            item { EstadoVacio(Icons.Outlined.Assessment, "Sin reportes", "Elija un período y toque \"Generar reporte\".") }
        }
        items(datos.reportes, key = { it.idReporte }) { r ->
            TarjetaReporte(r, abierto == r.idReporte) { abierto = if (abierto == r.idReporte) null else r.idReporte }
        }
    }
}

@Composable
private fun TarjetaReporte(r: ReporteInfo, abierto: Boolean, alPulsar: () -> Unit) {
    val c = coloresEstado
    val porPaso = r.detalles.groupBy { it.descripcion.substringAfter(" en ") }
    fun total(cat: String) = r.detalles.filter { it.categoria == cat }.sumOf { it.valor }
    val cruces = total("CRUCES_POR_PASO")
    val viajeros = total("VIAJEROS_POR_PASO")
    val monto = total("VALOR_DECLARADO_POR_PASO")

    Tarjeta(alPulsar = alPulsar, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(Icons.Outlined.Assessment, c.info, c.infoSuave)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Reporte #${r.idReporte}", style = MaterialTheme.typography.titleSmall)
                Texto2(r.titulo)
            }
            ChipEstado(r.estado)
            Icon(if (abierto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metrica("Cruces", numero(cruces), Modifier.weight(1f))
            Metrica("Viajeros", numero(viajeros), Modifier.weight(1f))
            Metrica("Declarado", usd(monto), Modifier.weight(1.4f))
        }
        AnimatedVisibility(abierto) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val maxCruces = porPaso.values.maxOfOrNull { l -> l.firstOrNull { it.categoria == "CRUCES_POR_PASO" }?.valor ?: 0.0 } ?: 0.0
                val maxViajeros = porPaso.values.maxOfOrNull { l -> l.firstOrNull { it.categoria == "VIAJEROS_POR_PASO" }?.valor ?: 0.0 } ?: 0.0
                val maxUsd = porPaso.values.maxOfOrNull { l -> l.firstOrNull { it.categoria == "VALOR_DECLARADO_POR_PASO" }?.valor ?: 0.0 } ?: 0.0
                porPaso.forEach { (paso, detalles) ->
                    Text(paso, style = MaterialTheme.typography.titleSmall)
                    detalles.forEach { d ->
                        when (d.categoria) {
                            "CRUCES_POR_PASO" -> BarraValor("Cruces", d.valor, maxCruces, MaterialTheme.colorScheme.primary)
                            "VIAJEROS_POR_PASO" -> BarraValor("Viajeros", d.valor, maxViajeros, MaterialTheme.colorScheme.secondary)
                            "VALOR_DECLARADO_POR_PASO" -> BarraValor("Valor declarado", d.valor, maxUsd, c.exito, usd(d.valor))
                            else -> BarraValor(d.descripcion, d.valor, d.valor, c.neutro)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metrica(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Texto2(titulo)
        Text(valor, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}
