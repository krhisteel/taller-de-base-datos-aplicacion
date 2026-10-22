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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.TituloSeccion

private data class Objeto(val nombre: String, val tipo: String, val uso: String)

private val OBJETOS = listOf(
    Objeto("PKG_GESTION_FRONTERA", "Package", "Agrupa toda la lógica del módulo fronterizo. Su elemento privado validar_rango valida las fechas de los reportes."),
    Objeto("sp_registrar_cruce", "Procedimiento del package", "Pantalla Registrar cruce: valida vehículo, RUT y autorización de menores (-20002, -20004, -20010)."),
    Objeto("SP_REPORTE_CRUCES", "Procedimiento con parámetros", "Pantalla Reportes: cruces, viajeros y US$ declarados por paso."),
    Objeto("SP_ACTUALIZAR_PLAZOS", "Procedimiento sin parámetros", "Botón \"Actualizar estados\" en Inicio y Plazos."),
    Objeto("FN_VALOR_DECLARADO", "Función con parámetros", "Valor declarado de cada cruce, usada dentro del SELECT de la lista de cruces."),
    Objeto("FN_TOTAL_PLAZOS_VENCIDOS", "Función sin parámetros", "Indicador \"Plazos vencidos\" del panel de inicio."),
    Objeto("TRG_CRUCE_PLAZO_AI", "Trigger de fila", "Crea el plazo de 90 o 180 días al registrar la salida de un vehículo."),
    Objeto("TRG_DOCUMENTO_AUDIT", "Trigger de fila", "Un detalle de auditoría por cada documento insertado, modificado o eliminado."),
    Objeto("TRG_DOCUMENTO_SENTENCIA", "Trigger de sentencia", "Una auditoría por cada instrucción sobre DOCUMENTO, sin importar cuántas filas afecte."),
)

@Composable
fun PantallaAcerca() {
    val c = coloresEstado
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Tarjeta(titulo = "SMCFT", subtitulo = "Sistema de Monitoreo y Control Fronterizo Terrestre", icono = Icons.Outlined.School) {
                Texto2("Taller de Base de Datos · Evaluación Parcial N°2")
                Texto2("Jael Reyes · Benjamin Riquelme Pozo · Aileen Oyaneder")
                Texto2("Registro y fiscalización de personas y vehículos en pasos fronterizos terrestres: cruces, declaraciones, plazos de admisión temporal y auditoría.")
            }
        }
        item { TituloSeccion("Objetos PL/SQL del informe") }
        items(OBJETOS) { o ->
            val (icono, color, fondo) = when {
                o.tipo.startsWith("Trigger") -> Triple(Icons.Outlined.Bolt, c.alerta, c.alertaSuave)
                o.tipo.startsWith("Función") -> Triple(Icons.Outlined.Functions, c.exito, c.exitoSuave)
                o.tipo == "Package" -> Triple(Icons.Outlined.Inventory2, c.info, c.infoSuave)
                else -> Triple(Icons.Outlined.Code, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
            }
            FilaObjeto(o, icono, color, fondo)
        }
    }
}

@Composable
private fun FilaObjeto(o: Objeto, icono: ImageVector, color: androidx.compose.ui.graphics.Color, fondo: androidx.compose.ui.graphics.Color) {
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            IconoCirculo(icono, color, fondo, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(o.nombre, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                Pildora(o.tipo, color, fondo)
                Texto2(o.uso)
            }
        }
    }
}
