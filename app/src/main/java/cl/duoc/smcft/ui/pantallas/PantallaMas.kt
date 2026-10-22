package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.ui.LocalSesion
import cl.duoc.smcft.ui.Navegacion
import cl.duoc.smcft.ui.Sub
import cl.duoc.smcft.ui.colorDeRol
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.DialogoConfirmar
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion
import cl.duoc.smcft.ui.componentes.VarianteBoton

private data class Entrada(val sub: Sub, val descripcion: String, val icono: ImageVector, val color: Color, val fondo: Color)

@Composable
fun PantallaMas(vm: SmcftViewModel, ajustes: Ajustes, nav: Navegacion) {
    val c = coloresEstado
    val sesion = LocalSesion.current!!
    var confirmarSalida by remember { mutableStateOf(false) }

    val gestion = buildList {
        add(Entrada(Sub.PERSONAS, "Personas registradas y autorizaciones de menores", Icons.Outlined.Groups, c.exito, c.exitoSuave))
        add(Entrada(Sub.VEHICULOS, "Vehículos particulares y diplomáticos", Icons.Outlined.DirectionsCar, c.violeta, c.violetaSuave))
        add(Entrada(Sub.REPORTES, "Cruces, viajeros y montos por paso", Icons.Outlined.Assessment, c.info, c.infoSuave))
        if (sesion.puede(Permiso.VER_AUDITORIA)) add(Entrada(Sub.AUDITORIA, "Historial de cambios en documentos", Icons.Outlined.HistoryEdu, c.neutro, c.neutroSuave))
    }
    val sistema = buildList {
        if (sesion.puede(Permiso.ADMINISTRAR)) {
            add(Entrada(Sub.USUARIOS, "Cuentas, perfiles e inicios de sesión", Icons.Outlined.ManageAccounts, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
            add(Entrada(
                Sub.CONEXION,
                if (ajustes.modo == Modo.ORACLE) "Conectado a Oracle Cloud" else "Modo demostración (datos en el teléfono)",
                if (ajustes.modo == Modo.ORACLE) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid,
                c.alerta, c.alertaSuave,
            ))
        }
        add(Entrada(Sub.ACERCA, "El proyecto y sus objetos de base de datos", Icons.Outlined.Info, c.neutro, c.neutroSuave))
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val (color, fondo) = colorDeRol(sesion.tipo)
            Tarjeta(alPulsar = { nav.abrir(Sub.CUENTA) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(sesion.nombre, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sesion.nombre, style = MaterialTheme.typography.titleMedium)
                        Texto2(sesion.email)
                        Pildora(sesion.tipo.nombre, color, fondo, iconoDeRol(sesion.tipo))
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { TituloSeccion("Gestión") }
        items(gestion) { FilaMenu(it, nav) }
        item { TituloSeccion("Sistema") }
        items(sistema) { FilaMenu(it, nav) }
        item {
            Spacer(Modifier.width(1.dp))
            Boton("Cerrar sesión", { confirmarSalida = true }, variante = VarianteBoton.CONTORNO, icono = Icons.AutoMirrored.Outlined.Logout)
        }
    }

    if (confirmarSalida) {
        DialogoConfirmar(
            titulo = "¿Cerrar sesión?",
            mensaje = "Volverá a la pantalla de inicio de sesión.",
            textoConfirmar = "Cerrar sesión",
            alConfirmar = vm::cerrarSesion,
            alCerrar = { confirmarSalida = false },
        )
    }
}

@Composable
private fun FilaMenu(e: Entrada, nav: Navegacion) {
    Tarjeta(alPulsar = { nav.abrir(e.sub) }, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(e.icono, e.color, e.fondo)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.sub.titulo, style = MaterialTheme.typography.titleSmall)
                Texto2(e.descripcion)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
