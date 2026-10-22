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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.NuevoVehiculo
import cl.duoc.smcft.core.repo.VehiculoInfo
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.BarraBusqueda
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Campo
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible
import java.time.LocalDate

@Composable
fun PantallaVehiculos(vm: SmcftViewModel, datos: Datos) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var nuevo by remember { mutableStateOf(false) }
    val lista = datos.vehiculos.filter {
        busqueda.isBlank() || it.patente.contains(busqueda, true) || it.marca.contains(busqueda, true) ||
            it.modelo.contains(busqueda, true) || it.propietario.contains(busqueda, true)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { BarraBusqueda(busqueda, { busqueda = it }, "Buscar por patente, marca o propietario") }
            if (lista.isEmpty()) item { EstadoVacio(Icons.Outlined.SearchOff, "Sin vehículos", "No hay vehículos que coincidan con la búsqueda.") }
            items(lista, key = { it.idVehiculo }) { v -> FilaVehiculo(v, datos) }
        }
        ExtendedFloatingActionButton(
            onClick = { nuevo = true },
            icon = { Icon(Icons.Outlined.Add, null) },
            text = { Text("Nuevo vehículo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
    if (nuevo) NuevoVehiculoHoja(vm, datos) { nuevo = false }
}

@Composable
private fun FilaVehiculo(v: VehiculoInfo, datos: Datos) {
    val c = coloresEstado
    val diplomatico = v.tipoVehiculo == "DIPLOMATICO"
    val plazoActivo = datos.plazos.firstOrNull { it.patente == v.patente && it.estado == "ACTIVO" }
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(Icons.Outlined.DirectionsCar, if (diplomatico) c.info else MaterialTheme.colorScheme.primary,
                if (diplomatico) c.infoSuave else MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${v.marca} ${v.modelo}", style = MaterialTheme.typography.titleSmall)
                Texto2("Año ${v.anio}")
            }
            Patente(v.patente, grande = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pildora(v.propietario, c.neutro, c.neutroSuave, Icons.Outlined.Person)
            Pildora(legible(v.tipoVehiculo) + if (diplomatico) " · 90 días" else " · 180 días",
                if (diplomatico) c.info else c.exito, if (diplomatico) c.infoSuave else c.exitoSuave)
        }
        if (plazoActivo != null) {
            Pildora("Fuera del país: ${plazoActivo.diasRestantes} días de plazo", c.alerta, c.alertaSuave, Icons.Outlined.Schedule)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoVehiculoHoja(vm: SmcftViewModel, datos: Datos, alCerrar: () -> Unit) {
    var patente by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var tipo by remember { mutableStateOf("PARTICULAR") }
    var propietario by remember { mutableStateOf(datos.personas.firstOrNull { !it.esMenor }?.idPersona) }
    var guardando by remember { mutableStateOf(false) }

    val anioNum = anio.toIntOrNull()
    val errorAnio = if (anio.isNotBlank() && (anioNum == null || anioNum !in 1950..LocalDate.now().year + 1)) "Año inválido" else null
    val errorPatente = if (patente.isNotBlank() && !Regex("^[A-Z0-9]{5,8}$").matches(patente)) "Solo letras y números (5 a 8)" else null
    val valido = patente.isNotBlank() && errorPatente == null && marca.isNotBlank() && modelo.isNotBlank() &&
        anioNum != null && errorAnio == null && propietario != null

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registrar vehículo", style = MaterialTheme.typography.headlineSmall)
            if (patente.isNotBlank()) Patente(patente, grande = true)
            Campo("Patente", patente, { patente = it.uppercase().filter { ch -> ch.isLetterOrDigit() } }, error = errorPatente)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Marca", marca, { marca = it }, Modifier.weight(1f))
                Campo("Modelo", modelo, { modelo = it }, Modifier.weight(1f))
            }
            Campo("Año", anio, { anio = it }, tipo = KeyboardType.Number, error = errorAnio)
            Texto2("Tipo de vehículo (define el plazo de admisión)")
            Filtros(listOf("PARTICULAR", "DIPLOMATICO", "CARGA"), tipo, { legible(it) }, { tipo = it })
            Texto2("Propietario")
            Filtros(datos.personas.filter { !it.esMenor }.map { it.idPersona }, propietario ?: -1,
                { id -> datos.personas.first { it.idPersona == id }.nombreCompleto }, { propietario = it })
            BotonPrincipal("Guardar vehículo", icono = Icons.Outlined.Save, habilitado = valido, guardando = guardando, alPulsar = {
                guardando = true
                vm.ejecutar(
                    exito = { _: Long -> "Vehículo $patente registrado" },
                    alTerminar = { ok -> guardando = false; if (ok) alCerrar() },
                ) { registrarVehiculo(NuevoVehiculo(patente, marca.trim(), modelo.trim(), anioNum!!, tipo, propietario!!)) }
            })
        }
    }
}
