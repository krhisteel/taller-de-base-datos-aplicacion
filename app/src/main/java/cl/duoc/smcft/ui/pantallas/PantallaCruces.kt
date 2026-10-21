package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.core.fnValorDeclarado
import cl.duoc.smcft.ui.Campo
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Selector
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.texto
import cl.duoc.smcft.ui.usd

@Composable
fun PantallaCruces(vm: SmcftViewModel, tablas: Tablas) {
    var abierto by remember { mutableStateOf<Long?>(null) }
    val cruces = remember(tablas) { vm.pkg.detallesCruces() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cruces.isEmpty()) item { Etiqueta("No hay cruces registrados.") }
        items(cruces, key = { it.idCruce }) { c ->
            Tarjeta(modifier = Modifier.clickable { abierto = if (abierto == c.idCruce) null else c.idCruce }) {
                Text("Cruce #${c.idCruce} · ${c.tipoMovimiento} · ${c.nombrePaso}")
                Etiqueta("${c.fechaHora.texto()} · ${c.cantViajeros} viajero(s)")
                // FN_VALOR_DECLARADO: función con parámetros
                Text("Valor declarado: ${usd(fnValorDeclarado(vm.db, c.idCruce))}")
                if (abierto == c.idCruce) {
                    HorizontalDivider()
                    DetalleCruce(vm, tablas, c.idCruce)
                }
            }
        }
    }
}

@Composable
private fun DetalleCruce(vm: SmcftViewModel, tablas: Tablas, idCruce: Long) {
    val cruce = tablas.cruces.first { it.idCruce == idCruce }
    cruce.idVehiculo?.let { id ->
        val v = tablas.vehiculos.first { it.idVehiculo == id }
        Text("Vehículo: ${v.patente} · ${v.marca} ${v.modelo} (${v.tipoVehiculo})")
    }
    Text("Viajeros:")
    tablas.detallesCruce.filter { it.idCruce == idCruce }.forEach { d ->
        val p = tablas.personas.first { it.idPersona == d.idPersona }
        Etiqueta("• ${p.nombreCompleto} (${p.rut}) — ${d.rolViajero}")
    }

    Text("Ítems declarados:")
    val items = vm.pkg.itemsCruce(idCruce)
    if (items.isEmpty()) Etiqueta("Sin declaraciones.")
    items.forEach { Etiqueta("• ${it.descripcion} x${it.cantidad} — ${usd(it.valorUsd)}") }

    HorizontalDivider()
    NuevaDeclaracion(vm, tablas, idCruce)
}

@Composable
private fun NuevaDeclaracion(vm: SmcftViewModel, tablas: Tablas, idCruce: Long) {
    var tipo by remember { mutableStateOf("ADUANA") }
    var categoria by remember { mutableStateOf(tablas.categorias.firstOrNull()?.idCategoria) }
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var valor by remember { mutableStateOf("") }

    Text("Agregar declaración (Aduana/SAG)")
    Selector("Tipo", listOf("ADUANA", "SAG"), tipo, { it }) { tipo = it }
    Selector("Categoría", tablas.categorias.map { it.idCategoria }, categoria, { id -> tablas.categorias.first { it.idCategoria == id }.nombre }) { categoria = it }
    Campo("Descripción", descripcion, { descripcion = it })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Campo("Cantidad", cantidad, { cantidad = it }, numerico = true, modifier = Modifier.weight(1f))
        Campo("Valor USD", valor, { valor = it }, numerico = true, modifier = Modifier.weight(1f))
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = categoria != null && descripcion.isNotBlank(),
        onClick = {
            val ok = vm.ejecutar({ id: Long -> "Declaración #$id registrada" }) {
                val cant = cantidad.trim().toIntOrNull() ?: error("Cantidad inválida")
                val usd = valor.trim().replace(',', '.').toDoubleOrNull() ?: error("Valor USD inválido")
                vm.pkg.registrarDeclaracion(idCruce, tipo, listOf(Triple(categoria!!, descripcion.trim(), cant to usd)))
            }
            if (ok) {
                descripcion = ""
                valor = ""
                cantidad = "1"
            }
        },
    ) { Text("Agregar ítem declarado") }
}
