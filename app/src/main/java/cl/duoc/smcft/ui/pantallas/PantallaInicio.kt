package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.fnTotalPlazosVencidos
import cl.duoc.smcft.core.spActualizarPlazos
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Pantalla
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.texto
import cl.duoc.smcft.ui.usd

@Composable
fun PantallaInicio(vm: SmcftViewModel, tablas: Tablas, irA: (Pantalla) -> Unit) {
    // FN_TOTAL_PLAZOS_VENCIDOS: función sin parámetros
    val vencidos = fnTotalPlazosVencidos(vm.db)
    val recientes = vm.pkg.detallesCruces().take(5)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Tarjeta("Resumen del sistema") {
                Indicador("Viajeros registrados", tablas.personas.size.toString())
                Indicador("Vehículos registrados", tablas.vehiculos.size.toString())
                Indicador("Cruces registrados", tablas.cruces.size.toString())
                Indicador("Plazos activos", tablas.plazos.count { it.estado == "ACTIVO" }.toString())
                Indicador("Plazos vencidos (FN_TOTAL_PLAZOS_VENCIDOS)", vencidos.toString())
                Indicador("Registros de auditoría", (tablas.auditorias.size + tablas.auditoriaDetalles.size).toString())
            }
        }
        item {
            Tarjeta("Acciones") {
                Button(onClick = { irA(Pantalla.REGISTRAR_CRUCE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar un cruce")
                }
                OutlinedButton(
                    onClick = {
                        // SP_ACTUALIZAR_PLAZOS: procedimiento sin parámetros
                        vm.ejecutar({ n: Int -> "Plazos actualizados: $n" }) { spActualizarPlazos(vm.db) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Actualizar plazos vencidos") }
                OutlinedButton(onClick = { irA(Pantalla.REPORTES) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generar reporte por paso")
                }
            }
        }
        item { Text("Últimos cruces", style = MaterialTheme.typography.titleMedium) }
        if (recientes.isEmpty()) {
            item { Etiqueta("Aún no hay cruces registrados.") }
        }
        items(recientes, key = { it.idCruce }) { c ->
            Tarjeta {
                Text("Cruce #${c.idCruce} · ${c.tipoMovimiento} · ${c.nombrePaso}")
                Etiqueta("${c.fechaHora.texto()} · ${c.cantViajeros} viajero(s) · ${usd(c.valorDeclarado)}")
            }
        }
    }
}

@Composable
private fun Indicador(nombre: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(nombre, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}
