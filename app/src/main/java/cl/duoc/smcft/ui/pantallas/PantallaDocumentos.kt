package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import cl.duoc.smcft.core.db.Secuencias
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.model.Documento
import cl.duoc.smcft.ui.Campo
import cl.duoc.smcft.ui.Estado
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Selector
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.parsearFecha
import cl.duoc.smcft.ui.texto

/** Cada cambio sobre DOCUMENTO dispara TRG_DOCUMENTO_AUDIT (fila) y TRG_DOCUMENTO_SENTENCIA (sentencia). */
@Composable
fun PantallaDocumentos(vm: SmcftViewModel, tablas: Tablas) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NuevoDocumento(vm, tablas) }
        item {
            Tarjeta("Actualización masiva") {
                Etiqueta("Un UPDATE sobre varios documentos deja un detalle de auditoría por documento (trigger de fila) y una sola auditoría por la instrucción (trigger de sentencia).")
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.ejecutar({ n: Int -> "UPDATE afectó $n documento(s)" }) {
                            vm.db.actualizarDocumentos(
                                donde = { d -> d.fechaVencimiento != null && d.fechaVencimiento!! < vm.db.hoy() && d.estado == "VIGENTE" },
                                cambio = { it.copy(estado = "VENCIDO") },
                            )
                        }
                    },
                ) { Text("Marcar documentos vencidos") }
            }
        }
        items(tablas.documentos.sortedBy { it.idDocumento }, key = { it.idDocumento }) { d ->
            val duenio = d.idPersona?.let { id -> tablas.personas.firstOrNull { it.idPersona == id }?.nombreCompleto }
                ?: d.idVehiculo?.let { id -> "Vehículo " + tablas.vehiculos.firstOrNull { it.idVehiculo == id }?.patente }
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${d.tipoDocumento} ${d.numero}", modifier = Modifier.weight(1f))
                    Estado(d.estado)
                    IconButton(onClick = {
                        vm.ejecutar({ _: Int -> "Documento ${d.numero} eliminado" }) {
                            vm.db.eliminarDocumentos { it.idDocumento == d.idDocumento }
                        }
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                }
                Etiqueta("Titular: ${duenio ?: "-"}")
                Etiqueta("Emisión: ${d.fechaEmision?.texto() ?: "-"} · Vencimiento: ${d.fechaVencimiento?.texto() ?: "-"}")
                Selector("Cambiar estado", listOf("VIGENTE", "VENCIDO", "ANULADO"), d.estado, { it }) { nuevo ->
                    if (nuevo != d.estado) {
                        vm.ejecutar({ _: Int -> "Documento ${d.numero}: $nuevo" }) {
                            vm.db.actualizarDocumentos({ it.idDocumento == d.idDocumento }, { it.copy(estado = nuevo) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NuevoDocumento(vm: SmcftViewModel, tablas: Tablas) {
    var tipo by remember { mutableStateOf("PASAPORTE") }
    var numero by remember { mutableStateOf("") }
    var emision by remember { mutableStateOf(vm.db.hoy().texto()) }
    var vencimiento by remember { mutableStateOf(vm.db.hoy().plusYears(5).texto()) }
    var paraVehiculo by remember { mutableStateOf(false) }
    var idPersona by remember { mutableStateOf(tablas.personas.firstOrNull()?.idPersona) }
    var idVehiculo by remember { mutableStateOf(tablas.vehiculos.firstOrNull()?.idVehiculo) }

    Tarjeta("Nuevo documento") {
        val tipos = if (paraVehiculo) listOf("PERMISO_CIRCULACION", "SEGURO", "PADRON") else listOf("CEDULA", "PASAPORTE", "AUTORIZACION_NOTARIAL")
        Selector("Titular", listOf(false, true), paraVehiculo, { if (it) "Vehículo" else "Persona" }) {
            paraVehiculo = it
            tipo = if (it) "PERMISO_CIRCULACION" else "PASAPORTE"
        }
        if (paraVehiculo) {
            Selector("Vehículo", tablas.vehiculos.map { it.idVehiculo }, idVehiculo, { id -> tablas.vehiculos.first { it.idVehiculo == id }.patente }) { idVehiculo = it }
        } else {
            Selector("Persona", tablas.personas.map { it.idPersona }, idPersona, { id -> tablas.personas.first { it.idPersona == id }.nombreCompleto }) { idPersona = it }
        }
        Selector("Tipo", tipos, tipo, { it }) { tipo = it }
        Campo("Número", numero, { numero = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Campo("Emisión", emision, { emision = it }, modifier = Modifier.weight(1f))
            Campo("Vencimiento", vencimiento, { vencimiento = it }, modifier = Modifier.weight(1f))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = numero.isNotBlank(),
            onClick = {
                val ok = vm.ejecutar({ _: Unit -> "Documento $numero registrado" }) {
                    vm.db.insertarDocumento(
                        Documento(
                            idDocumento = vm.db.nextval(Secuencias.DOCUMENTO),
                            tipoDocumento = tipo,
                            numero = numero.trim(),
                            fechaEmision = parsearFecha(emision),
                            fechaVencimiento = parsearFecha(vencimiento),
                            idPersona = if (paraVehiculo) null else idPersona,
                            idVehiculo = if (paraVehiculo) idVehiculo else null,
                            estado = "VIGENTE",
                        )
                    )
                }
                if (ok) numero = ""
            },
        ) { Text("Registrar documento") }
    }
}
