package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.ui.Navegacion
import cl.duoc.smcft.ui.Sub
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion

private data class Entrada(val sub: Sub, val descripcion: String, val icono: ImageVector, val color: Color, val fondo: Color)

@Composable
fun PantallaMas(ajustes: Ajustes, nav: Navegacion) {
    val c = coloresEstado
    val gestion = listOf(
        Entrada(Sub.PERSONAS, "Registro de personas y autorizaciones de menores", Icons.Outlined.Groups, c.exito, c.exitoSuave),
        Entrada(Sub.VEHICULOS, "Vehículos particulares y diplomáticos", Icons.Outlined.DirectionsCar, Color(0xFF7B4FB0), Color(0x267B4FB0)),
        Entrada(Sub.REPORTES, "Cruces, viajeros y US$ declarados por paso", Icons.Outlined.Assessment, c.info, c.infoSuave),
        Entrada(Sub.AUDITORIA, "Historial de cambios sobre documentos", Icons.Outlined.HistoryEdu, c.neutro, c.neutroSuave),
    )
    val sistema = listOf(
        Entrada(
            Sub.CONEXION,
            if (ajustes.modo == Modo.ORACLE) "Conectado a Oracle mediante ORDS" else "Modo demostración (datos en el teléfono)",
            if (ajustes.modo == Modo.ORACLE) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid,
            c.alerta, c.alertaSuave,
        ),
        Entrada(Sub.ACERCA, "Objetos PL/SQL del informe y cómo los usa la app", Icons.Outlined.Info, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
    )

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSeccion("Gestión") }
        items(gestion) { FilaMenu(it, nav) }
        item { TituloSeccion("Sistema") }
        items(sistema) { FilaMenu(it, nav) }
    }
}

@Composable
private fun FilaMenu(e: Entrada, nav: Navegacion) {
    Tarjeta(alPulsar = { nav.abrir(e.sub) }, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(e.icono, e.color, e.fondo)
            Spacer(Modifier.width(14.dp))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(e.sub.titulo, style = MaterialTheme.typography.titleSmall)
                Texto2(e.descripcion)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
