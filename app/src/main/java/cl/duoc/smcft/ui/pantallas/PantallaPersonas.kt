package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.model.AutorizacionMenor
import cl.duoc.smcft.core.model.Documento
import cl.duoc.smcft.core.model.Persona
import cl.duoc.smcft.core.model.Vehiculo
import cl.duoc.smcft.ui.Campo
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Selector
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.parsearFecha
import cl.duoc.smcft.ui.texto
import java.time.Period

@Composable
fun PantallaPersonas(vm: SmcftViewModel, tablas: Tablas) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NuevaPersona(vm) }
        item { NuevoVehiculo(vm, tablas) }
        item { NuevaAutorizacion(vm, tablas) }
        item { Text("Personas", style = MaterialTheme.typography.titleMedium) }
        items(tablas.personas, key = { "p" + it.idPersona }) { p ->
            val edad = Period.between(p.fechaNacimiento, vm.db.hoy()).years
            val autorizado = tablas.autorizaciones.any { it.idMenor == p.idPersona && it.estado == "VIGENTE" }
            Tarjeta {
                Text(p.nombreCompleto)
                Etiqueta("${p.rut} · ${p.nacionalidad} · nacido ${p.fechaNacimiento.texto()} ($edad años)")
                if (edad < 18) Etiqueta(if (autorizado) "Menor con autorización vigente" else "Menor SIN autorización vigente")
            }
        }
        item { Text("Vehículos", style = MaterialTheme.typography.titleMedium) }
        items(tablas.vehiculos, key = { "v" + it.idVehiculo }) { v ->
            val propietario = tablas.personas.firstOrNull { it.idPersona == v.idPersonaPropietario }?.nombreCompleto ?: "?"
            Tarjeta {
                Text("${v.patente} · ${v.marca} ${v.modelo} ${v.anio}")
                Etiqueta("${v.tipoVehiculo} · propietario: $propietario")
            }
        }
    }
}

@Composable
private fun NuevaPersona(vm: SmcftViewModel) {
    var rut by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("CHILENA") }
    var nacimiento by remember { mutableStateOf("") }

    Tarjeta("Registrar viajero") {
        Campo("RUT (12345678-9)", rut, { rut = it })
        Campo("Nombres", nombres, { nombres = it })
        Campo("Apellidos", apellidos, { apellidos = it })
        Campo("Nacionalidad", nacionalidad, { nacionalidad = it })
        Campo("Fecha de nacimiento (dd-mm-aaaa)", nacimiento, { nacimiento = it })
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = rut.isNotBlank() && nombres.isNotBlank() && apellidos.isNotBlank(),
            onClick = {
                val ok = vm.ejecutar({ _: Unit -> "Persona $rut registrada" }) {
                    vm.db.insertarPersona(
                        Persona(
                            idPersona = vm.db.nextval(Secuencias.PERSONA),
                            rut = rut.trim(),
                            nombres = nombres.trim(),
                            apellidos = apellidos.trim(),
                            nacionalidad = nacionalidad.trim().uppercase(),
                            fechaNacimiento = parsearFecha(nacimiento) ?: error("Fecha de nacimiento inválida"),
                        )
                    )
                }
                if (ok) { rut = ""; nombres = ""; apellidos = ""; nacimiento = "" }
            },
        ) { Text("Registrar persona") }
    }
}

@Composable
private fun NuevoVehiculo(vm: SmcftViewModel, tablas: Tablas) {
    var patente by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("PARTICULAR") }
    var propietario by remember { mutableStateOf(tablas.personas.firstOrNull()?.idPersona) }

    Tarjeta("Registrar vehículo") {
        Campo("Patente", patente, { patente = it.uppercase() })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo("Marca", marca, { marca = it }, modifier = Modifier.weight(1f))
            Campo("Modelo", modelo, { modelo = it }, modifier = Modifier.weight(1f))
        }
        Campo("Año", anio, { anio = it }, numerico = true)
        Selector("Tipo", listOf("PARTICULAR", "DIPLOMATICO", "CARGA"), tipo, { it }) { tipo = it }
        Selector("Propietario", tablas.personas.map { it.idPersona }, propietario, { id -> tablas.personas.first { it.idPersona == id }.nombreCompleto }) { propietario = it }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && propietario != null,
            onClick = {
                val ok = vm.ejecutar({ _: Unit -> "Vehículo $patente registrado" }) {
                    vm.db.insertarVehiculo(
                        Vehiculo(
                            idVehiculo = vm.db.nextval(Secuencias.VEHICULO),
                            patente = patente.trim(),
                            marca = marca.trim(),
                            modelo = modelo.trim(),
                            anio = anio.trim().toIntOrNull() ?: error("Año inválido"),
                            tipoVehiculo = tipo,
                            idPersonaPropietario = propietario!!,
                        )
                    )
                }
                if (ok) { patente = ""; marca = ""; modelo = ""; anio = "" }
            },
        ) { Text("Registrar vehículo") }
    }
}

/** Crea el documento notarial y la autorización VIGENTE que exige sp_registrar_cruce a los menores. */
@Composable
private fun NuevaAutorizacion(vm: SmcftViewModel, tablas: Tablas) {
    val hoy = vm.db.hoy()
    val menores = tablas.personas.filter { Period.between(it.fechaNacimiento, hoy).years < 18 }
    val adultos = tablas.personas.filter { Period.between(it.fechaNacimiento, hoy).years >= 18 }
    var menor by remember { mutableStateOf<Long?>(null) }
    var representante by remember { mutableStateOf<Long?>(null) }
    var numero by remember { mutableStateOf("") }

    Tarjeta("Autorización de viaje de menor") {
        if (menores.isEmpty()) {
            Etiqueta("No hay menores de edad registrados.")
        } else {
            Selector("Menor", menores.map { it.idPersona }, menor, { id -> tablas.personas.first { it.idPersona == id }.nombreCompleto }) { menor = it }
            Selector("Representante", adultos.map { it.idPersona }, representante, { id -> tablas.personas.first { it.idPersona == id }.nombreCompleto }) { representante = it }
            Campo("N° documento notarial", numero, { numero = it })
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = menor != null && representante != null && numero.isNotBlank(),
                onClick = {
                    val ok = vm.ejecutar({ _: Unit -> "Autorización registrada" }) {
                        val idDoc = vm.db.nextval(Secuencias.DOCUMENTO)
                        vm.db.insertarDocumento(
                            Documento(idDoc, "AUTORIZACION_NOTARIAL", numero.trim(), hoy, hoy.plusYears(1), menor, null, "VIGENTE")
                        )
                        vm.db.insertarAutorizacion(
                            AutorizacionMenor(vm.db.nextval(Secuencias.AUTORIZACION), menor!!, representante!!, idDoc, "NOTARIAL", hoy, "VIGENTE")
                        )
                    }
                    if (ok) numero = ""
                },
            ) { Text("Registrar autorización") }
        }
    }
}
