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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.Cargando
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.VarianteBoton
import cl.duoc.smcft.ui.pantallas.PantallaAcerca
import cl.duoc.smcft.ui.pantallas.PantallaAuditoria
import cl.duoc.smcft.ui.pantallas.PantallaConexion
import cl.duoc.smcft.ui.pantallas.PantallaCruces
import cl.duoc.smcft.ui.pantallas.PantallaCuenta
import cl.duoc.smcft.ui.pantallas.PantallaDocumentos
import cl.duoc.smcft.ui.pantallas.PantallaInicio
import cl.duoc.smcft.ui.pantallas.PantallaLogin
import cl.duoc.smcft.ui.pantallas.PantallaMas
import cl.duoc.smcft.ui.pantallas.PantallaNuevoCruce
import cl.duoc.smcft.ui.pantallas.PantallaPersonas
import cl.duoc.smcft.ui.pantallas.PantallaPlazos
import cl.duoc.smcft.ui.pantallas.PantallaReportes
import cl.duoc.smcft.ui.pantallas.PantallaUsuarios
import cl.duoc.smcft.ui.pantallas.PantallaVehiculos

/** Secciones de la barra inferior. */
enum class Tab(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector,
    val iconoActivo: ImageVector,
    val ayuda: ContenidoAyuda,
) {
    INICIO("Inicio", "", Icons.Outlined.Home, Icons.Filled.Home, TextosAyuda.INICIO),
    CRUCES("Cruces", "Entradas y salidas registradas", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz, TextosAyuda.CRUCES),
    PLAZOS("Plazos", "Admisión temporal de vehículos", Icons.Outlined.Schedule, Icons.Filled.Schedule, TextosAyuda.PLAZOS),
    DOCUMENTOS("Documentos", "Identidad, autorizaciones y vehículos", Icons.Outlined.Description, Icons.Filled.Description, TextosAyuda.DOCUMENTOS),
    MAS("Más", "Gestión, cuenta y configuración", Icons.Outlined.Apps, Icons.Filled.Apps, TextosAyuda.INICIO),
}

/** Pantallas que se abren encima de las secciones, con botón atrás. */
enum class Sub(val titulo: String, val descripcion: String, val ayuda: ContenidoAyuda?) {
    NUEVO_CRUCE("Registrar cruce", "Complete los 4 pasos y confirme", TextosAyuda.NUEVO_CRUCE),
    REPORTES("Reportes", "Actividad por paso fronterizo", TextosAyuda.REPORTES),
    AUDITORIA("Auditoría", "Historial de cambios en documentos", TextosAyuda.AUDITORIA),
    PERSONAS("Viajeros", "Personas y autorizaciones de menores", TextosAyuda.PERSONAS),
    VEHICULOS("Vehículos", "Particulares y diplomáticos", TextosAyuda.VEHICULOS),
    USUARIOS("Usuarios y accesos", "Cuentas y registro de inicios de sesión", TextosAyuda.USUARIOS),
    CUENTA("Mi cuenta", "Su perfil y sus permisos", null),
    CONEXION("Conexión", "Dónde se guardan los datos", TextosAyuda.CONEXION),
    ACERCA("Acerca del sistema", "Proyecto y objetos PL/SQL", null),
}

/** Acciones de navegación que las pantallas pueden pedir. */
class Navegacion(val irATab: (Tab) -> Unit, val abrir: (Sub) -> Unit, val volver: () -> Unit)

@Composable
fun SmcftApp(vm: SmcftViewModel = viewModel()) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var configurando by rememberSaveable { mutableStateOf(false) }

    val actual = sesion
    if (actual == null) {
        // Antes de iniciar sesión solo se puede configurar el servidor.
        BackHandler(enabled = configurando) { configurando = false }
        if (configurando) {
            PantallaSecundaria(Sub.CONEXION, alVolver = { configurando = false }) { PantallaConexion(vm, ajustes) }
        } else {
            PantallaLogin(vm, ajustes, alConfigurar = { configurando = true })
        }
        return
    }

    CompositionLocalProvider(LocalSesion provides actual) {
        AppPrincipal(vm, ajustes)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPrincipal(vm: SmcftViewModel, ajustes: Ajustes) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val sesion = LocalSesion.current!!
    var tab by rememberSaveable { mutableStateOf(Tab.INICIO) }
    var sub by rememberSaveable { mutableStateOf<Sub?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val nav = remember { Navegacion(irATab = { sub = null; tab = it }, abrir = { sub = it }, volver = { sub = null }) }

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    BackHandler(enabled = sub != null) { sub = null }
    BackHandler(enabled = sub == null && tab != Tab.INICIO) { tab = Tab.INICIO }

    val abierta = sub
    Scaffold(
        topBar = {
            when {
                abierta != null -> BarraSuperior(abierta.titulo, abierta.descripcion, alVolver = { sub = null }) {
                    abierta.ayuda?.let { BotonAyuda(it) }
                }
                tab != Tab.INICIO -> BarraSuperior(tab.titulo, tab.descripcion) {
                    BotonAyuda(tab.ayuda)
                    IconButton(onClick = vm::recargar) { Icon(Icons.Outlined.Refresh, "Actualizar") }
                    IconButton(onClick = { sub = Sub.CUENTA }) { Avatar(sesion.nombre, 32.dp) }
                }
            }
        },
        bottomBar = {
            if (abierta == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(if (t == tab) t.iconoActivo else t.icono, null) },
                            label = { Text(t.titulo) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // En Inicio el encabezado se dibuja detrás de la barra de estado.
        contentWindowInsets = if (abierta == null && tab == Tab.INICIO) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(Modifier.padding(relleno).fillMaxSize()) {
            val datos = estado.datos
            when {
                // Pantallas que no necesitan los datos operativos
                abierta == Sub.CONEXION -> PantallaConexion(vm, ajustes)
                abierta == Sub.ACERCA -> PantallaAcerca()
                abierta == Sub.CUENTA -> PantallaCuenta(vm)
                abierta == Sub.USUARIOS -> PantallaUsuarios(vm)
                abierta == null && tab == Tab.MAS -> PantallaMas(vm, ajustes, nav)

                datos == null && estado.cargando -> Cargando()
                datos == null -> ErrorCarga(estado.error, alReintentar = vm::recargar, alCerrarSesion = vm::cerrarSesion)

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
                        Contenido(vm, datos, ajustes, tab, abierta, nav)
                    }
                }
            }
            if (estado.cargando && estado.datos != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Barra superior con título, una línea que explica la pantalla y acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    descripcion: String,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (descripcion.isNotBlank()) {
                    Text(
                        descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (alVolver != null) IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
        },
        actions = { acciones() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** Pantalla con barra superior y botón atrás, fuera de la navegación principal. */
@Composable
private fun PantallaSecundaria(sub: Sub, alVolver: () -> Unit, contenido: @Composable () -> Unit) {
    Scaffold(
        topBar = { BarraSuperior(sub.titulo, sub.descripcion, alVolver) { sub.ayuda?.let { BotonAyuda(it) } } },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno -> Box(Modifier.padding(relleno).fillMaxSize()) { contenido() } }
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
private fun ErrorCarga(error: String?, alReintentar: () -> Unit, alCerrarSesion: () -> Unit) {
    EstadoVacio(
        icono = Icons.Outlined.CloudOff,
        titulo = "No se pudieron cargar los datos",
        mensaje = error ?: "Error desconocido",
    ) {
        Boton("Reintentar", alReintentar, icono = Icons.Outlined.Refresh)
        Boton("Cerrar sesión", alCerrarSesion, variante = VarianteBoton.CONTORNO)
    }
}
