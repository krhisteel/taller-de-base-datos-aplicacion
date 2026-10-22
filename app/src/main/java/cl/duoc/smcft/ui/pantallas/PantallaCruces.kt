package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.CruceInfo
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.ItemInfo
import cl.duoc.smcft.core.repo.ViajeroInfo
import cl.duoc.smcft.ui.Navegacion
import cl.duoc.smcft.ui.Sub
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.BarraBusqueda
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Campo
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.FilaDato
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible
import cl.duoc.smcft.ui.componentes.texto
import cl.duoc.smcft.ui.componentes.usd

private enum class FiltroCruce(val texto: String) { TODOS("Todos"), ENTRADA("Entradas"), SALIDA("Salidas"), VEHICULAR("Vehiculares"), PEATONAL("Peatonales") }

@Composable
fun PantallaCruces(vm: SmcftViewModel, datos: Datos, nav: Navegacion) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroCruce.TODOS) }
    var seleccionado by remember { mutableStateOf<CruceInfo?>(null) }

    val lista = datos.cruces.filter { c ->
        when (filtro) {
            FiltroCruce.TODOS -> true
            FiltroCruce.ENTRADA -> c.tipoMovimiento == "ENTRADA"
            FiltroCruce.SALIDA -> c.tipoMovimiento == "SALIDA"
            FiltroCruce.VEHICULAR -> c.patente != null
            FiltroCruce.PEATONAL -> c.patente == null
        }
    }.filter { c ->
        val q = busqueda.trim()
        q.isEmpty() || c.idCruce.toString() == q.removePrefix("#") ||
            c.nombrePaso.contains(q, ignoreCase = true) || c.patente?.contains(q, ignoreCase = true) == true
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BarraBusqueda(busqueda, { busqueda = it }, "Buscar por paso, patente o N° de cruce")
                    Filtros(FiltroCruce.entries, filtro, { it.texto }, { filtro = it })
                    Texto2("${lista.size} de ${datos.cruces.size} cruces · toque uno para ver el detalle")
                }
            }
            if (lista.isEmpty()) {
                item {
                    if (datos.cruces.isEmpty()) {
                        EstadoVacio(Icons.Outlined.SwapHoriz, "Sin cruces registrados", "Use el botón \"Nuevo cruce\" para registrar el primero.")
                    } else {
                        EstadoVacio(Icons.Outlined.SearchOff, "Sin resultados", "Ningún cruce coincide con la búsqueda o el filtro.")
                    }
                }
            }
            items(lista, key = { it.idCruce }) { cruce ->
                FilaCruce(cruce) { seleccionado = cruce }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { nav.abrir(Sub.NUEVO_CRUCE) },
            icon = { Icon(Icons.Outlined.Add, null) },
            text = { Text("Nuevo cruce") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    seleccionado?.let { cruce ->
        // Si el cruce se actualizó (por ejemplo, con una declaración), se muestra la versión nueva.
        val actual = datos.cruces.firstOrNull { it.idCruce == cruce.idCruce } ?: cruce
        DetalleCruce(vm, datos, actual) { seleccionado = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleCruce(vm: SmcftViewModel, datos: Datos, cruce: CruceInfo, alCerrar: () -> Unit) {
    val c = coloresEstado
    val entrada = cruce.tipoMovimiento == "ENTRADA"
    var viajeros by remember { mutableStateOf<List<ViajeroInfo>?>(null) }
    var items by remember { mutableStateOf<List<ItemInfo>?>(null) }
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(cruce.idCruce, version) {
        viajeros = runCatching { vm.consultar { viajeros(cruce.idCruce) } }.getOrDefault(emptyList())
        items = runCatching { vm.consultar { items(cruce.idCruce) } }.getOrDefault(emptyList())
    }

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCirculo(
                    if (entrada) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                    if (entrada) c.exito else c.alerta, if (entrada) c.exitoSuave else c.alertaSuave, 52.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cruce #${cruce.idCruce}", style = MaterialTheme.typography.headlineSmall)
                    Texto2(if (entrada) "Entrada al país" else "Salida del país")
                }
                ChipEstado(cruce.estado)
            }

            Tarjeta {
                FilaDato(Icons.Outlined.Place, "Paso fronterizo", cruce.nombrePaso)
                FilaDato(Icons.Outlined.CalendarMonth, "Fecha y hora", cruce.fechaHora.texto())
                if (cruce.patente != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsCar, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("Vehículo", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Patente(cruce.patente!!)
                    }
                } else {
                    FilaDato(Icons.Outlined.DirectionsWalk, "Tipo de cruce", "Peatonal")
                }
                FilaDato(Icons.Outlined.AttachMoney, "Valor declarado (FN_VALOR_DECLARADO)", usd(cruce.valorDeclarado))
            }

            Text("Viajeros", style = MaterialTheme.typography.titleMedium)
            when (val lista = viajeros) {
                null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> Tarjeta(relleno = 12.dp) {
                    if (lista.isEmpty()) Texto2("Sin viajeros registrados.")
                    lista.forEachIndexed { i, v ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(v.nombre, 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(v.nombre, style = MaterialTheme.typography.bodyLarge)
                                Texto2("RUT ${v.rut}")
                            }
                            ChipEstado(v.rol)
                        }
                    }
                }
            }

            Text("Declaración Aduana / SAG", style = MaterialTheme.typography.titleMedium)
            when (val lista = items) {
                null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> Tarjeta(relleno = 12.dp) {
                    if (lista.isEmpty()) Texto2("No hay ítems declarados en este cruce.")
                    lista.forEach { it ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconoCirculo(Icons.Outlined.Inventory2, c.info, c.infoSuave, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.descripcion, style = MaterialTheme.typography.bodyLarge)
                                Texto2("Cantidad: ${it.cantidad}")
                            }
                            Text(usd(it.valorUsd), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (lista.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row {
                            Text("Total", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(usd(lista.sumOf { it.valorUsd }), style = MaterialTheme.typography.titleSmall, color = c.exito)
                        }
                    }
                }
            }

            NuevoItem(vm, datos, cruce.idCruce) { version++ }
        }
    }
}

@Composable
private fun NuevoItem(vm: SmcftViewModel, datos: Datos, idCruce: Long, alGuardar: () -> Unit) {
    var tipo by remember { mutableStateOf("ADUANA") }
    var categoria by remember { mutableStateOf(datos.categorias.firstOrNull()?.idCategoria) }
    var descripcion by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var valor by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    val cant = cantidad.trim().toIntOrNull()
    val monto = valor.trim().replace(',', '.').toDoubleOrNull()
    val valido = categoria != null && descripcion.isNotBlank() && cant != null && cant > 0 && monto != null && monto >= 0

    Tarjeta(titulo = "Agregar ítem declarado", icono = Icons.Outlined.ReceiptLong) {
        Filtros(listOf("ADUANA", "SAG"), tipo, { it }, { tipo = it })
        Filtros(datos.categorias.map { it.idCategoria }, categoria ?: -1, { id -> legible(datos.categorias.first { it.idCategoria == id }.nombre) }, { categoria = it })
        Campo("Descripción", descripcion, { descripcion = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Campo("Cantidad", cantidad, { cantidad = it }, Modifier.weight(1f), tipo = KeyboardType.Number)
            Campo("Valor total US$", valor, { valor = it }, Modifier.weight(1f), tipo = KeyboardType.Decimal)
        }
        BotonPrincipal("Agregar a la declaración", habilitado = valido, guardando = guardando, icono = Icons.Outlined.Add, alPulsar = {
            guardando = true
            vm.ejecutar(
                exito = { id: Long -> "Declaración #$id registrada" },
                alTerminar = { ok ->
                    guardando = false
                    if (ok) {
                        descripcion = ""; valor = ""; cantidad = "1"
                        alGuardar()
                    }
                },
            ) { registrarDeclaracion(idCruce, tipo, categoria!!, descripcion.trim(), cant!!, monto!!) }
        })
    }
}
