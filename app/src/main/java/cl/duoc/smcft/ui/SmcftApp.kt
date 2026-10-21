package cl.duoc.smcft.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.ui.pantallas.PantallaAuditoria
import cl.duoc.smcft.ui.pantallas.PantallaCruces
import cl.duoc.smcft.ui.pantallas.PantallaDocumentos
import cl.duoc.smcft.ui.pantallas.PantallaInicio
import cl.duoc.smcft.ui.pantallas.PantallaPersonas
import cl.duoc.smcft.ui.pantallas.PantallaPlazos
import cl.duoc.smcft.ui.pantallas.PantallaRegistrarCruce
import cl.duoc.smcft.ui.pantallas.PantallaReportes
import kotlinx.coroutines.launch

enum class Pantalla(val titulo: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    REGISTRAR_CRUCE("Registrar cruce", Icons.Filled.Add),
    CRUCES("Cruces y declaraciones", Icons.Filled.List),
    PLAZOS("Plazos de admisión", Icons.Filled.DateRange),
    REPORTES("Reportes", Icons.Filled.Search),
    DOCUMENTOS("Documentos", Icons.Filled.AccountBox),
    AUDITORIA("Auditoría", Icons.Filled.Info),
    PERSONAS("Personas y vehículos", Icons.Filled.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmcftApp(vm: SmcftViewModel = viewModel()) {
    val tablas by vm.tablas.collectAsStateWithLifecycle()
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.INICIO) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "SMCFT · Gestión de Frontera",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                Pantalla.entries.forEach { p ->
                    NavigationDrawerItem(
                        label = { Text(p.titulo) },
                        icon = { Icon(p.icono, contentDescription = null) },
                        selected = p == pantalla,
                        onClick = {
                            pantalla = p
                            scope.launch { drawer.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pantalla.titulo) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (pantalla) {
                    Pantalla.INICIO -> PantallaInicio(vm, tablas) { pantalla = it }
                    Pantalla.REGISTRAR_CRUCE -> PantallaRegistrarCruce(vm, tablas) { pantalla = Pantalla.CRUCES }
                    Pantalla.CRUCES -> PantallaCruces(vm, tablas)
                    Pantalla.PLAZOS -> PantallaPlazos(vm, tablas)
                    Pantalla.REPORTES -> PantallaReportes(vm, tablas)
                    Pantalla.DOCUMENTOS -> PantallaDocumentos(vm, tablas)
                    Pantalla.AUDITORIA -> PantallaAuditoria(tablas)
                    Pantalla.PERSONAS -> PantallaPersonas(vm, tablas)
                }
            }
        }
    }
}
