package cl.duoc.smcft.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.ui.componentes.Cargando
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.pantallas.PantallaAcerca
import cl.duoc.smcft.ui.pantallas.PantallaAuditoria
import cl.duoc.smcft.ui.pantallas.PantallaConexion
import cl.duoc.smcft.ui.pantallas.PantallaCruces
import cl.duoc.smcft.ui.pantallas.PantallaDocumentos
import cl.duoc.smcft.ui.pantallas.PantallaInicio
import cl.duoc.smcft.ui.pantallas.PantallaMas
import cl.duoc.smcft.ui.pantallas.PantallaNuevoCruce
import cl.duoc.smcft.ui.pantallas.PantallaPersonas
import cl.duoc.smcft.ui.pantallas.PantallaPlazos
import cl.duoc.smcft.ui.pantallas.PantallaReportes
import cl.duoc.smcft.ui.pantallas.PantallaVehiculos

/** Secciones de la barra inferior. */
enum class Tab(val titulo: String, val icono: ImageVector, val iconoActivo: ImageVector) {
    INICIO("Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    CRUCES("Cruces", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz),
    PLAZOS("Plazos", Icons.Outlined.Schedule, Icons.Filled.Schedule),
    DOCUMENTOS("Documentos", Icons.Outlined.Description, Icons.Filled.Description),
    MAS("Más", Icons.Outlined.Apps, Icons.Filled.Apps),
}

/** Pantallas que se abren encima de las secciones, con botón atrás. */
enum class Sub(val titulo: String) {
    NUEVO_CRUCE("Registrar cruce"),
    REPORTES("Reportes estadísticos"),
    AUDITORIA("Auditoría"),
    PERSONAS("Viajeros"),
    VEHICULOS("Vehículos"),
    CONEXION("Conexión"),
    ACERCA("Acerca del sistema"),
}

/** Acciones de navegación que las pantallas pueden pedir. */
class Navegacion(val irATab: (Tab) -> Unit, val abrir: (Sub) -> Unit, val volver: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmcftApp(vm: SmcftViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.INICIO) }
    var sub by rememberSaveable { mutableStateOf<Sub?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val nav = remember { Navegacion(irATab = { sub = null; tab = it }, abrir = { sub = it }, volver = { sub = null }) }

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    BackHandler(enabled = sub != null) { sub = null }
    BackHandler(enabled = sub == null && tab != Tab.INICIO) { tab = Tab.INICIO }

    val actual = sub
    Scaffold(
        topBar = {
            when {
                actual != null -> TopAppBar(
                    title = { Text(actual.titulo) },
                    navigationIcon = { IconButton(onClick = { sub = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                tab != Tab.INICIO -> TopAppBar(
                    title = { Text(tab.titulo) },
                    actions = { IconButton(onClick = vm::recargar) { Icon(Icons.Outlined.Refresh, "Actualizar") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            if (actual == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(if (t == tab) t.iconoActivo else t.icono, null) },
                            label = { Text(t.titulo) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // En Inicio el encabezado se dibuja detrás de la barra de estado.
        contentWindowInsets = if (actual == null && tab == Tab.INICIO) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            val datos = estado.datos
            when {
                // Pantallas que no necesitan datos
                actual == Sub.CONEXION -> PantallaConexion(vm, ajustes)
                actual == Sub.ACERCA -> PantallaAcerca()
                actual == null && tab == Tab.MAS -> PantallaMas(ajustes, nav)

                datos == null && estado.cargando -> Cargando()
                datos == null -> ErrorCarga(estado.error, alReintentar = vm::recargar, alConfigurar = { sub = Sub.CONEXION })

                else -> PullToRefreshBox(
                    isRefreshing = estado.cargando,
                    onRefresh = vm::recargar,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (estado.error != null) {
                            Text(
                                "No se pudo actualizar: ${estado.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        Contenido(vm, datos, ajustes, tab, actual, nav)
                    }
                }
            }
            if (estado.cargando && estado.datos != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Contenido(vm: SmcftViewModel, datos: Datos, ajustes: Ajustes, tab: Tab, sub: Sub?, nav: Navegacion) {
    val clave: Any = sub ?: tab
    AnimatedContent(targetState = clave, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pantalla") { destino ->
        when (destino) {
            Sub.NUEVO_CRUCE -> PantallaNuevoCruce(vm, datos, alTerminar = { nav.irATab(Tab.CRUCES) })
            Sub.REPORTES -> PantallaReportes(vm, datos, ajustes)
            Sub.AUDITORIA -> PantallaAuditoria(datos)
            Sub.PERSONAS -> PantallaPersonas(vm, datos)
            Sub.VEHICULOS -> PantallaVehiculos(vm, datos)
            Tab.INICIO -> PantallaInicio(vm, datos, ajustes, nav)
            Tab.CRUCES -> PantallaCruces(vm, datos, nav)
            Tab.PLAZOS -> PantallaPlazos(vm, datos)
            Tab.DOCUMENTOS -> PantallaDocumentos(vm, datos)
            else -> Unit
        }
    }
}

@Composable
private fun ErrorCarga(error: String?, alReintentar: () -> Unit, alConfigurar: () -> Unit) {
    EstadoVacio(
        icono = Icons.Outlined.CloudOff,
        titulo = "No se pudieron cargar los datos",
        mensaje = error ?: "Error desconocido",
    ) {
        Button(onClick = alReintentar) { Text("Reintentar") }
        OutlinedButton(onClick = alConfigurar) { Text("Revisar la conexión") }
    }
}
