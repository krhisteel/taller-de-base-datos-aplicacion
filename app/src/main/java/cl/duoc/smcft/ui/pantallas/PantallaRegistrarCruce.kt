package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.model.Vehiculo
import cl.duoc.smcft.core.pkg.GestionFrontera
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Selector
import cl.duoc.smcft.ui.Tarjeta
import java.time.Period

/** Formulario que llama a PKG_GESTION_FRONTERA.sp_registrar_cruce. */
@Composable
fun PantallaRegistrarCruce(vm: SmcftViewModel, tablas: Tablas, alRegistrar: () -> Unit) {
    val pasosActivos = tablas.pasos.filter { it.estado == "ACTIVO" }
    var idPaso by remember { mutableStateOf(pasosActivos.firstOrNull()?.idPaso) }
    var movimiento by remember { mutableStateOf("SALIDA") }
    var vehiculo by remember { mutableStateOf<Vehiculo?>(null) }
    val viajeros = remember { mutableStateListOf<String>() }
    var conductor by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tarjeta("Cruce") {
            Selector("Paso fronterizo", pasosActivos.map { it.idPaso }, idPaso, { id -> tablas.pasos.first { it.idPaso == id }.nombre }) { idPaso = it }
            Selector("Tipo de movimiento", listOf("ENTRADA", "SALIDA"), movimiento, { it }) { movimiento = it }
            Selector("Vehículo", listOf<Vehiculo?>(null) + tablas.vehiculos, vehiculo, { it?.let { v -> "${v.patente} (${v.tipoVehiculo})" } ?: "Peatonal" }) {
                vehiculo = it
                if (it == null) conductor = null
            }
            if (vehiculo != null && movimiento == "SALIDA") {
                val dias = if (vehiculo!!.tipoVehiculo == "DIPLOMATICO") 90 else 180
                Etiqueta("Al registrar la salida, el trigger TRG_CRUCE_PLAZO_AI creará un plazo de admisión de $dias días.")
            }
        }

        Tarjeta("Viajeros (máximo ${GestionFrontera.MAX_VIAJEROS})") {
            tablas.personas.forEach { p ->
                val edad = Period.between(p.fechaNacimiento, vm.db.hoy()).years
                val marcado = p.rut in viajeros
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = marcado,
                        onCheckedChange = { activo ->
                            if (activo) viajeros += p.rut else {
                                viajeros -= p.rut
                                if (conductor == p.rut) conductor = null
                            }
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(p.nombreCompleto)
                        Etiqueta("${p.rut} · $edad años" + if (edad < 18) " · menor de edad" else "")
                    }
                    if (vehiculo != null && marcado) {
                        RadioButton(selected = conductor == p.rut, onClick = { conductor = p.rut })
                        Etiqueta("Conductor")
                    }
                }
            }
        }

        Button(
            enabled = idPaso != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val ok = vm.ejecutar({ id: Long -> "Cruce #$id registrado" }) {
                    vm.pkg.spRegistrarCruce(
                        idPaso = idPaso!!,
                        patente = vehiculo?.patente,
                        tipoMovimiento = movimiento,
                        rutsViajeros = viajeros.toList(),
                        rutConductor = conductor,
                    )
                }
                if (ok) {
                    viajeros.clear()
                    conductor = null
                    alRegistrar()
                }
            },
        ) { Text("Registrar cruce") }
    }
}
