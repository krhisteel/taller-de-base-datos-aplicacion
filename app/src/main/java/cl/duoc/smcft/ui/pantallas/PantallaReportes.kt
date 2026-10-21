package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.spReporteCruces
import cl.duoc.smcft.ui.Campo
import cl.duoc.smcft.ui.Estado
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.parsearFecha
import cl.duoc.smcft.ui.texto
import cl.duoc.smcft.ui.usd

@Composable
fun PantallaReportes(vm: SmcftViewModel, tablas: Tablas) {
    val hoy = vm.db.hoy()
    var desde by remember { mutableStateOf(hoy.minusDays(30).texto()) }
    var hasta by remember { mutableStateOf(hoy.texto()) }
    var abierto by remember { mutableStateOf<Long?>(null) }
    val idUsuario = tablas.usuarios.firstOrNull()?.idUsuario ?: 1L

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Tarjeta("Reporte de cruces por paso fronterizo") {
                Etiqueta("Cantidad de cruces, viajeros y valor declarado en USD por paso activo (SP_REPORTE_CRUCES).")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campo("Desde (dd-mm-aaaa)", desde, { desde = it }, modifier = Modifier.weight(1f))
                    Campo("Hasta (dd-mm-aaaa)", hasta, { hasta = it }, modifier = Modifier.weight(1f))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.ejecutar({ id: Long -> abierto = id; "Reporte #$id generado" }) {
                            // Fechas nulas o invertidas las rechaza la validación privada (-20020).
                            val d = parsearFecha(desde)?.atStartOfDay()
                            val h = parsearFecha(hasta)?.atTime(23, 59, 59)
                            spReporteCruces(vm.db, d, h, idUsuario)
                        }
                    },
                ) { Text("Generar reporte") }
            }
        }
        items(tablas.reportes.sortedByDescending { it.idReporte }, key = { it.idReporte }) { r ->
            Tarjeta(modifier = Modifier.clickable { abierto = if (abierto == r.idReporte) null else r.idReporte }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reporte #${r.idReporte}", modifier = Modifier.weight(1f))
                    Estado(r.estado)
                }
                Etiqueta(r.titulo)
                if (abierto == r.idReporte) {
                    HorizontalDivider()
                    tablas.reporteDetalles.filter { it.idReporte == r.idReporte }.groupBy { it.descripcion.substringAfter(" en ") }
                        .forEach { (paso, detalles) ->
                            Text(paso)
                            detalles.forEach { d ->
                                val valor = if (d.unidad == "USD") usd(d.valor) else "${d.valor.toLong()} ${d.unidad}"
                                Etiqueta("• ${d.categoria}: $valor")
                            }
                        }
                }
            }
        }
    }
}
