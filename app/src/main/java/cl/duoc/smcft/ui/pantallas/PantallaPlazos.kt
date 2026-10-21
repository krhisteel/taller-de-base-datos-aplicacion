package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.fnTotalPlazosVencidos
import cl.duoc.smcft.core.spActualizarPlazos
import cl.duoc.smcft.ui.Estado
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.texto

@Composable
fun PantallaPlazos(vm: SmcftViewModel, tablas: Tablas) {
    val plazos = tablas.plazos.sortedBy { it.fechaLimite }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Tarjeta("Control de plazos de admisión temporal") {
                Etiqueta("90 días para vehículos diplomáticos y 180 días para el resto. El plazo lo crea el trigger al registrar la salida de un vehículo.")
                Text("Plazos vencidos no cerrados: ${fnTotalPlazosVencidos(vm.db)}")
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.ejecutar({ n: Int -> "Plazos actualizados a VENCIDO: $n" }) { spActualizarPlazos(vm.db) } },
                ) { Text("Ejecutar SP_ACTUALIZAR_PLAZOS") }
            }
        }
        if (plazos.isEmpty()) item { Etiqueta("No hay plazos registrados.") }
        items(plazos, key = { it.idPlazo }) { p ->
            val info = vm.pkg.plazoInfo(p)
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Plazo #${p.idPlazo} · ${info.patente}", modifier = Modifier.weight(1f))
                    Estado(p.estado)
                }
                Etiqueta("${p.tipo} · cruce #${p.idCruceIngreso}")
                Etiqueta("Desde ${p.fechaIngreso.texto()} hasta ${p.fechaLimite.texto()}")
                Text(
                    if (info.diasRestantes >= 0) "Días restantes: ${info.diasRestantes}"
                    else "Vencido hace ${-info.diasRestantes} día(s)"
                )
            }
        }
    }
}
