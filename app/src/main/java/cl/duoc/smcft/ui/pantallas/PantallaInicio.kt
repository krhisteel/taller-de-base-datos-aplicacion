package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.AddRoad
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.CruceInfo
import cl.duoc.smcft.core.repo.Datos
import cl.duoc.smcft.ui.Navegacion
import cl.duoc.smcft.ui.Sub
import cl.duoc.smcft.ui.Tab
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.BarraValor
import cl.duoc.smcft.ui.componentes.ChipEstado
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Indicador
import cl.duoc.smcft.ui.componentes.Patente
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion
import cl.duoc.smcft.ui.componentes.texto
import cl.duoc.smcft.ui.componentes.usd
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PantallaInicio(vm: SmcftViewModel, datos: Datos, ajustes: Ajustes, nav: Navegacion) {
    val c = coloresEstado
    val r = datos.resumen
    val porVencer = datos.plazos.filter { it.estado == "ACTIVO" && it.diasRestantes in 0..30 }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Encabezado(ajustes) }

        // Indicadores principales (se superponen al encabezado)
        item {
            Column(
                Modifier.padding(horizontal = 16.dp).padding(top = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Indicador("Cruces registrados", r.cruces.toString(), Icons.Outlined.SwapHoriz, c.info, c.infoSuave,
                        Modifier.weight(1f), alPulsar = { nav.irATab(Tab.CRUCES) })
                    Indicador("Viajeros", r.personas.toString(), Icons.Outlined.Groups, c.exito, c.exitoSuave,
                        Modifier.weight(1f), alPulsar = { nav.abrir(Sub.PERSONAS) })
                    Indicador("Vehículos", r.vehiculos.toString(), Icons.Outlined.DirectionsCar, Color(0xFF7B4FB0), Color(0x267B4FB0),
                        Modifier.weight(1f), alPulsar = { nav.abrir(Sub.VEHICULOS) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Indicador("Plazos activos", r.plazosActivos.toString(), Icons.Outlined.Schedule, c.alerta, c.alertaSuave,
                        Modifier.weight(1f), alPulsar = { nav.irATab(Tab.PLAZOS) })
                    Indicador("Plazos vencidos", r.plazosVencidos.toString(), Icons.Outlined.WarningAmber, c.peligro, c.peligroSuave,
                        Modifier.weight(1f), alPulsar = { nav.irATab(Tab.PLAZOS) })
                    Indicador("Registros de auditoría", r.registrosAuditoria.toString(), Icons.Outlined.HistoryEdu, c.neutro, c.neutroSuave,
                        Modifier.weight(1f), alPulsar = { nav.abrir(Sub.AUDITORIA) })
                }
            }
        }

        // Alertas de plazos
        if (r.plazosVencidos > 0 || porVencer.isNotEmpty()) {
            item {
                Tarjeta(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCirculo(Icons.Outlined.WarningAmber, c.alerta, c.alertaSuave, 40.dp)
                        Spacer(Modifier.padding(6.dp))
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
                            Spacer(Modifier.padding(4.dp))
                            Texto2("vence el ${p.fechaLimite.toLocalDate().texto()}", Modifier.weight(1f))
                            Pildora("${p.diasRestantes} días", c.alerta, c.alertaSuave)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            vm.ejecutar({ n: Int -> "SP_ACTUALIZAR_PLAZOS: $n plazo(s) marcado(s) como VENCIDO" }) { actualizarPlazos() }
                        }) {
                            Icon(Icons.Outlined.Update, null)
                            Spacer(Modifier.padding(4.dp))
                            Text("Actualizar estados")
                        }
                        TextButton(onClick = { nav.irATab(Tab.PLAZOS) }) { Text("Ver plazos") }
                    }
                }
            }
        }

        // Accesos rápidos
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccionRapida("Nuevo cruce", Icons.Outlined.AddRoad, Modifier.weight(1f)) { nav.abrir(Sub.NUEVO_CRUCE) }
                AccionRapida("Reporte", Icons.Outlined.Assessment, Modifier.weight(1f)) { nav.abrir(Sub.REPORTES) }
                AccionRapida("Auditoría", Icons.Outlined.HistoryEdu, Modifier.weight(1f)) { nav.abrir(Sub.AUDITORIA) }
            }
        }

        // Gráficos
        if (datos.cruces.isNotEmpty()) {
            item {
                val porPaso = datos.cruces.groupingBy { it.nombrePaso }.eachCount()
                val maximo = (porPaso.values.maxOrNull() ?: 0).toDouble()
                val entradas = datos.cruces.count { it.tipoMovimiento == "ENTRADA" }
                val salidas = datos.cruces.size - entradas
                val totalUsd = datos.cruces.sumOf { it.valorDeclarado }
                Tarjeta(Modifier.padding(horizontal = 16.dp), titulo = "Movimiento por paso fronterizo", icono = Icons.Outlined.Assessment) {
                    datos.pasos.forEach { paso ->
                        BarraValor(paso.nombre, (porPaso[paso.nombre] ?: 0).toDouble(), maximo, MaterialTheme.colorScheme.secondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pildora("$entradas entradas", c.exito, c.exitoSuave, Icons.AutoMirrored.Outlined.Login)
                        Pildora("$salidas salidas", c.alerta, c.alertaSuave, Icons.AutoMirrored.Outlined.Logout)
                        Pildora(usd(totalUsd), c.info, c.infoSuave)
                    }
                }
            }
        }

        item {
            TituloSeccion("Últimos cruces", Modifier.padding(horizontal = 16.dp)) {
                TextButton(onClick = { nav.irATab(Tab.CRUCES) }) { Text("Ver todos") }
            }
        }
        if (datos.cruces.isEmpty()) {
            item {
                EstadoVacio(Icons.Outlined.SwapHoriz, "Aún no hay cruces", "Registre el primer cruce para ver aquí la actividad del paso fronterizo.") {
                    FilledTonalButton(onClick = { nav.abrir(Sub.NUEVO_CRUCE) }) { Text("Registrar cruce") }
                }
            }
        }
        items(datos.cruces.take(5), key = { it.idCruce }) { cruce ->
            FilaCruce(cruce, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { nav.irATab(Tab.CRUCES) }
        }
    }
}

@Composable
private fun Encabezado(ajustes: Ajustes) {
    val c = coloresEstado
    val hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CL")))
        .replaceFirstChar { it.titlecase() }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(c.degradadoInicio, c.degradadoFin)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hoy, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            val oracle = ajustes.modo == Modo.ORACLE
            Pildora(
                if (oracle) "Oracle Cloud" else "Modo demostración",
                Color.White,
                Color.White.copy(alpha = 0.18f),
                if (oracle) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid,
            )
        }
        Text("SMCFT", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Sistema de Monitoreo y Control Fronterizo Terrestre",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun AccionRapida(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, alPulsar: () -> Unit) {
    Tarjeta(modifier, alPulsar = alPulsar, relleno = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconoCirculo(icono, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary, 40.dp)
            Text(texto, style = MaterialTheme.typography.labelLarge)
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
            Spacer(Modifier.padding(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${if (entrada) "Entrada" else "Salida"} · ${cruce.nombrePaso}", style = MaterialTheme.typography.titleSmall)
                Texto2("Cruce #${cruce.idCruce} · ${cruce.fechaHora.texto()}")
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
