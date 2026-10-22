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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.repo.NuevaPersona
import cl.duoc.smcft.core.repo.PersonaInfo
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.BarraBusqueda
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Campo
import cl.duoc.smcft.ui.componentes.CampoFecha
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.Filtros
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.legible
import cl.duoc.smcft.ui.componentes.texto
import java.time.LocalDate

private enum class FiltroPersona(val texto: String) { TODOS("Todos"), ADULTOS("Adultos"), MENORES("Menores"), SIN_AUTORIZACION("Menores sin autorización") }

/** Valida el formato y el dígito verificador de un RUT chileno (12345678-9). */
fun rutValido(rut: String): Boolean {
    val limpio = rut.replace(".", "").uppercase()
    val partes = limpio.split("-")
    if (partes.size != 2 || partes[0].isEmpty() || partes[0].any { !it.isDigit() } || partes[1].length != 1) return false
    var suma = 0
    var factor = 2
    for (d in partes[0].reversed()) {
        suma += (d - '0') * factor
        factor = if (factor == 7) 2 else factor + 1
    }
    val dv = when (val r = 11 - suma % 11) { 11 -> "0"; 10 -> "K"; else -> r.toString() }
    return dv == partes[1]
}

@Composable
fun PantallaPersonas(vm: SmcftViewModel, datos: Datos) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroPersona.TODOS) }
    var nueva by remember { mutableStateOf(false) }
    var autorizar by remember { mutableStateOf<PersonaInfo?>(null) }

    val lista = datos.personas
        .filter {
            when (filtro) {
                FiltroPersona.TODOS -> true
                FiltroPersona.ADULTOS -> !it.esMenor
                FiltroPersona.MENORES -> it.esMenor
                FiltroPersona.SIN_AUTORIZACION -> it.esMenor && !it.autorizacionVigente
            }
        }
        .filter { busqueda.isBlank() || it.nombreCompleto.contains(busqueda, true) || it.rut.contains(busqueda, true) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BarraBusqueda(busqueda, { busqueda = it }, "Buscar por nombre o RUT")
                    Filtros(FiltroPersona.entries, filtro, { it.texto }, { filtro = it })
                    Texto2("${lista.size} de ${datos.personas.size} personas")
                }
            }
            if (lista.isEmpty()) item { EstadoVacio(Icons.Outlined.SearchOff, "Sin resultados", "No hay personas que coincidan con la búsqueda.") }
            items(lista, key = { it.idPersona }) { p -> FilaPersona(p) { autorizar = p } }
        }
        ExtendedFloatingActionButton(
            onClick = { nueva = true },
            icon = { Icon(Icons.Outlined.PersonAdd, null) },
            text = { Text("Nueva persona") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (nueva) NuevaPersonaHoja(vm) { nueva = false }
    autorizar?.let { AutorizacionHoja(vm, datos, it) { autorizar = null } }
}

@Composable
private fun FilaPersona(p: PersonaInfo, alAutorizar: () -> Unit) {
    val c = coloresEstado
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.nombreCompleto, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.nombreCompleto, style = MaterialTheme.typography.titleSmall)
                Texto2("RUT ${p.rut}")
                Texto2("${p.edad} años · nacido el ${p.fechaNacimiento.texto()}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pildora(legible(p.nacionalidad), c.neutro, c.neutroSuave, Icons.Outlined.Flag)
            when {
                !p.esMenor -> Pildora("Adulto", c.info, c.infoSuave, Icons.Outlined.Person)
                p.autorizacionVigente -> Pildora("Autorización vigente", c.exito, c.exitoSuave, Icons.Outlined.VerifiedUser)
                else -> Pildora("Menor sin autorización", c.peligro, c.peligroSuave, Icons.Outlined.ChildCare)
            }
            Spacer(Modifier.weight(1f))
            if (p.esMenor && !p.autorizacionVigente) {
                FilledTonalButton(onClick = alAutorizar) { Text("Autorizar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaPersonaHoja(vm: SmcftViewModel, alCerrar: () -> Unit) {
    var rut by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("CHILENA") }
    var nacimiento by remember { mutableStateOf<LocalDate?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val errorRut = if (rut.isNotBlank() && !rutValido(rut)) "RUT inválido (formato 12345678-9)" else null
    val valido = rut.isNotBlank() && errorRut == null && nombres.isNotBlank() && apellidos.isNotBlank() && nacimiento != null

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registrar viajero", style = MaterialTheme.typography.headlineSmall)
            Campo("RUT", rut, { rut = it.trim().uppercase() }, icono = Icons.Outlined.Badge, error = errorRut, ayuda = "Sin puntos y con guion")
            Campo("Nombres", nombres, { nombres = it })
            Campo("Apellidos", apellidos, { apellidos = it })
            Texto2("Nacionalidad")
            Filtros(listOf("CHILENA", "ARGENTINA", "PERUANA", "BOLIVIANA", "OTRA"), nacionalidad, { legible(it) }, { nacionalidad = it })
            CampoFecha("Fecha de nacimiento", nacimiento, { nacimiento = it })
            nacimiento?.let {
                if (it.plusYears(18).isAfter(LocalDate.now())) {
                    Texto2("Es menor de edad: necesitará una autorización vigente para cruzar.")
                }
            }
            BotonPrincipal("Guardar persona", icono = Icons.Outlined.Save, habilitado = valido, guardando = guardando, alPulsar = {
                guardando = true
                vm.ejecutar(
                    exito = { _: Long -> "${nombres.trim()} registrado(a)" },
                    alTerminar = { ok -> guardando = false; if (ok) alCerrar() },
                ) { registrarPersona(NuevaPersona(rut, nombres.trim(), apellidos.trim(), nacionalidad, nacimiento!!)) }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutorizacionHoja(vm: SmcftViewModel, datos: Datos, menor: PersonaInfo, alCerrar: () -> Unit) {
    val adultos = datos.personas.filter { !it.esMenor }
    var representante by remember { mutableStateOf(adultos.firstOrNull()?.idPersona) }
    var numero by remember { mutableStateOf("AUT-") }
    var guardando by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Autorización de viaje", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(menor.nombreCompleto)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(menor.nombreCompleto, style = MaterialTheme.typography.titleSmall)
                    Texto2("${menor.edad} años · RUT ${menor.rut}")
                }
            }
            Texto2("Se registrará el documento notarial y una AUTORIZACION_MENOR en estado VIGENTE.")
            Texto2("Representante")
            Filtros(adultos.map { it.idPersona }, representante ?: -1, { id -> adultos.first { it.idPersona == id }.nombreCompleto }, { representante = it })
            Campo("N° documento notarial", numero, { numero = it.uppercase() }, icono = Icons.Outlined.Gavel)
            BotonPrincipal(
                "Registrar autorización",
                icono = Icons.Outlined.VerifiedUser,
                habilitado = representante != null && numero.length > 4,
                guardando = guardando,
                alPulsar = {
                    guardando = true
                    vm.ejecutar(
                        exito = { _: Long -> "${menor.nombres} ya puede cruzar" },
                        alTerminar = { ok -> guardando = false; if (ok) alCerrar() },
                    ) { registrarAutorizacion(menor.idPersona, representante!!, numero.trim()) }
                },
            )
        }
    }
}
