package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddRoad
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.CruceInfo
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.core.seguridad.Sesion
import cl.duoc.smcft.ui.BotonAyuda
import cl.duoc.smcft.ui.LocalSesion
import cl.duoc.smcft.ui.Navegacion
import cl.duoc.smcft.ui.Sub
import cl.duoc.smcft.ui.Tab
import cl.duoc.smcft.ui.TextosAyuda
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.BarraValor
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Indicador
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion
import cl.duoc.smcft.ui.componentes.VarianteBoton
import cl.duoc.smcft.ui.componentes.texto
import cl.duoc.smcft.ui.componentes.usd
import cl.duoc.smcft.ui.puede
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Acceso directo del panel. */
private data class Tarea(val titulo: String, val descripcion: String, val icono: ImageVector, val accion: (Navegacion) -> Unit)

/** Tareas que se muestran a cada perfil según sus permisos. */
private fun tareasDe(sesion: Sesion): List<Tarea> = buildList {
    if (sesion.puede(Permiso.REGISTRAR_CRUCE)) add(Tarea("Registrar cruce", "Entrada o salida de viajeros", Icons.Outlined.AddRoad) { it.abrir(Sub.NUEVO_CRUCE) })
    if (sesion.puede(Permiso.GESTIONAR_PERSONAS)) add(Tarea("Viajeros", "Registro y autorización de menores", Icons.Outlined.Groups) { it.abrir(Sub.PERSONAS) })
    if (sesion.puede(Permiso.DECLARAR_ADUANA)) add(Tarea("Declarar en Aduana", "Ítems y valores de un cruce", Icons.Outlined.Inventory2) { it.irATab(Tab.CRUCES) })
    if (sesion.puede(Permiso.DECLARAR_SAG)) add(Tarea("Declarar en SAG", "Control sanitario de un cruce", Icons.Outlined.Spa) { it.irATab(Tab.CRUCES) })
    if (sesion.puede(Permiso.GESTIONAR_VEHICULOS)) add(Tarea("Vehículos", "Registro de vehículos", Icons.Outlined.DirectionsCar) { it.abrir(Sub.VEHICULOS) })
    if (sesion.puede(Permiso.GESTIONAR_PLAZOS)) add(Tarea("Plazos", "Control de admisión temporal", Icons.Outlined.Schedule) { it.irATab(Tab.PLAZOS) })
    if (sesion.puede(Permiso.GESTIONAR_DOCUMENTOS)) add(Tarea("Documentos", "Pasaportes, cédulas y permisos", Icons.Outlined.Description) { it.irATab(Tab.DOCUMENTOS) })
    if (sesion.puede(Permiso.GENERAR_REPORTES)) add(Tarea("Reportes", "Estadísticas por paso", Icons.Outlined.Assessment) { it.abrir(Sub.REPORTES) })
    if (sesion.puede(Permiso.VER_AUDITORIA)) add(Tarea("Auditoría", "Historial de cambios", Icons.Outlined.HistoryEdu) { it.abrir(Sub.AUDITORIA) })
    if (sesion.puede(Permiso.ADMINISTRAR)) add(Tarea("Usuarios", "Cuentas y accesos", Icons.Outlined.ManageAccounts) { it.abrir(Sub.USUARIOS) })
    if (isEmpty()) add(Tarea("Cruces", "Consultar entradas y salidas", Icons.Outlined.SwapHoriz) { it.irATab(Tab.CRUCES) })
}

@Composable
fun PantallaInicio(vm: SmcftViewModel, datos: Datos, ajustes: Ajustes, nav: Navegacion) {
    val c = coloresEstado
    val sesion = LocalSesion.current!!
    val r = datos.resumen
    val porVencer = datos.plazos.filter { it.estado == "ACTIVO" && it.diasRestantes in 0..30 }
    var mostrarConsejo by rememberSaveable { mutableStateOf(true) }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Encabezado(sesion, ajustes, nav) }

        // Indicadores (se superponen al encabezado)
        item {
            Column(Modifier.offset(y = (-36).dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Indicador("Cruces", r.cruces.toString(), Icons.Outlined.SwapHoriz, c.info, c.infoSuave, Modifier.weight(1f)) { nav.irATab(Tab.CRUCES) }
                    Indicador("Viajeros", r.personas.toString(), Icons.Outlined.Groups, c.exito, c.exitoSuave, Modifier.weight(1f)) { nav.abrir(Sub.PERSONAS) }
                    Indicador("Vehículos", r.vehiculos.toString(), Icons.Outlined.DirectionsCar, c.violeta, c.violetaSuave, Modifier.weight(1f)) { nav.abrir(Sub.VEHICULOS) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Indicador("Plazos activos", r.plazosActivos.toString(), Icons.Outlined.Schedule, c.alerta, c.alertaSuave, Modifier.weight(1f)) { nav.irATab(Tab.PLAZOS) }
                    Indicador("Plazos vencidos", r.plazosVencidos.toString(), Icons.Outlined.WarningAmber, c.peligro, c.peligroSuave, Modifier.weight(1f)) { nav.irATab(Tab.PLAZOS) }
                    Indicador("Documentos", datos.documentos.size.toString(), Icons.Outlined.Description, c.neutro, c.neutroSuave, Modifier.weight(1f)) { nav.irATab(Tab.DOCUMENTOS) }
                }
            }
        }

        // Consejo para nuevos usuarios
        if (mostrarConsejo) {
            item {
                Row(
                    Modifier.offset(y = (-24).dp).padding(horizontal = 16.dp).fillMaxWidth()
                        .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "¿Primera vez? Toque el botón ? de cada sección para ver cómo funciona.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { mostrarConsejo = false }) {
                        Icon(Icons.Outlined.Close, "Cerrar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Mis tareas
        item {
            Column(Modifier.offset(y = (-16).dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TituloSeccion("Mis tareas")
                tareasDe(sesion).chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fila.forEach { t -> TarjetaTarea(t, Modifier.weight(1f)) { t.accion(nav) } }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Alertas de plazos
        if (r.plazosVencidos > 0 || porVencer.isNotEmpty()) {
            item {
                Tarjeta(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCirculo(Icons.Outlined.WarningAmber, c.alerta, c.alertaSuave, 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Atención con los plazos", style = MaterialTheme.typography.titleMedium)
                            Texto2(
                                buildList {
                                    if (r.plazosVencidos > 0) add("${r.plazosVencidos} vencido(s)")
                                    if (porVencer.isNotEmpty()) add("${porVencer.size} vence(n) en 30 días o menos")
                                }.joinToString(" · ")
                            )
                        }
                    }
                    porVencer.take(3).forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Patente(p.patente)
                            Spacer(Modifier.width(10.dp))
                            Texto2("vence el ${p.fechaLimite.toLocalDate().texto()}", Modifier.weight(1f))
                            Pildora("${p.diasRestantes} días", c.alerta, c.alertaSuave)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (puede(Permiso.GESTIONAR_PLAZOS)) {
                            Boton("Actualizar estados", {
                                vm.ejecutar({ n: Int -> if (n == 0) "No había plazos por actualizar" else "Se marcaron $n plazo(s) como vencidos" }) { actualizarPlazos() }
                            }, Modifier.weight(1f), VarianteBoton.SECUNDARIO, Icons.Outlined.Update, compacto = true)
                        }
                        Boton("Ver plazos", { nav.irATab(Tab.PLAZOS) }, Modifier.weight(1f), VarianteBoton.CONTORNO, compacto = true)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Gráfico
        if (datos.cruces.isNotEmpty()) {
            item {
                val porPaso = datos.cruces.groupingBy { it.nombrePaso }.eachCount()
                val maximo = (porPaso.values.maxOrNull() ?: 0).toDouble()
                val entradas = datos.cruces.count { it.tipoMovimiento == "ENTRADA" }
                val salidas = datos.cruces.size - entradas
                Tarjeta(Modifier.padding(horizontal = 16.dp), titulo = "Actividad por paso", subtitulo = "Total de cruces registrados", icono = Icons.Outlined.Assessment) {
                    datos.pasos.forEach { paso ->
                        BarraValor(paso.nombre, (porPaso[paso.nombre] ?: 0).toDouble(), maximo, MaterialTheme.colorScheme.secondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pildora("$entradas entradas", c.exito, c.exitoSuave, Icons.AutoMirrored.Outlined.Login)
                        Pildora("$salidas salidas", c.alerta, c.alertaSuave, Icons.AutoMirrored.Outlined.Logout)
                        Pildora(usd(datos.cruces.sumOf { it.valorDeclarado }), c.info, c.infoSuave)
                    }
                }
            }
        }

        item {
            TituloSeccion("Últimos cruces", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                TextButton(onClick = { nav.irATab(Tab.CRUCES) }) { Text("Ver todos") }
            }
        }
        if (datos.cruces.isEmpty()) {
            item {
                EstadoVacio(Icons.Outlined.SwapHoriz, "Aún no hay cruces", "Cuando se registre un cruce, aparecerá aquí.") {
                    if (puede(Permiso.REGISTRAR_CRUCE)) Boton("Registrar el primero", { nav.abrir(Sub.NUEVO_CRUCE) }, icono = Icons.Outlined.AddRoad)
                }
            }
        }
        items(datos.cruces.take(5), key = { it.idCruce }) { cruce ->
            FilaCruce(cruce, Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) { nav.irATab(Tab.CRUCES) }
        }
    }
}

@Composable
private fun Encabezado(sesion: Sesion, ajustes: Ajustes, nav: Navegacion) {
    val c = coloresEstado
    val hora = LocalTime.now().hour
    val saludo = when (hora) { in 5..11 -> "Buenos días"; in 12..19 -> "Buenas tardes"; else -> "Buenas noches" }
    val hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CL"))).replaceFirstChar { it.titlecase() }
    val oracle = ajustes.modo == Modo.ORACLE
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(c.degradadoInicio, c.degradadoMedio, c.degradadoFin)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SMCFT", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Control Fronterizo Terrestre", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
            Box { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) { BotonAyuda(TextosAyuda.INICIO) } }
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.White).clickable { nav.abrir(Sub.CUENTA) },
                contentAlignment = Alignment.Center,
            ) { Avatar(sesion.nombre, 38.dp) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$saludo,", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
            Text(sesion.nombre.substringBefore(" "), color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pildora(sesion.tipo.nombre, Color.White, Color.White.copy(alpha = 0.18f), iconoDeRol(sesion.tipo))
            Pildora(if (oracle) "Oracle Cloud" else "Demostración", Color.White, Color.White.copy(alpha = 0.18f),
                if (oracle) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid)
        }
        Text(hoy, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TarjetaTarea(t: Tarea, modifier: Modifier, alPulsar: () -> Unit) {
    Tarjeta(modifier, alPulsar = alPulsar, relleno = 14.dp) {
        IconoCirculo(t.icono, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary, 40.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Texto2(t.descripcion, maxLineas = 2)
        }
    }
}

/** Fila resumida de un cruce (se usa en Inicio y en la lista de cruces). */
@Composable
fun FilaCruce(cruce: CruceInfo, modifier: Modifier = Modifier, alPulsar: () -> Unit) {
    val c = coloresEstado
    val entrada = cruce.tipoMovimiento == "ENTRADA"
    Tarjeta(modifier, alPulsar = alPulsar, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(
                if (entrada) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                if (entrada) c.exito else c.alerta,
                if (entrada) c.exitoSuave else c.alertaSuave,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${if (entrada) "Entrada" else "Salida"} · ${cruce.nombrePaso}", style = MaterialTheme.typography.titleSmall)
                Texto2("Cruce N° ${cruce.idCruce} · ${cruce.fechaHora.texto()}")
            }
            ChipEstado(cruce.estado)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cruce.patente != null) Patente(cruce.patente!!) else Pildora("Peatonal", c.neutro, c.neutroSuave)
            Pildora("${cruce.cantViajeros} viajero(s)", c.info, c.infoSuave, Icons.Outlined.Groups)
            if (cruce.valorDeclarado > 0) Pildora(usd(cruce.valorDeclarado), c.exito, c.exitoSuave)
        }
    }
}
