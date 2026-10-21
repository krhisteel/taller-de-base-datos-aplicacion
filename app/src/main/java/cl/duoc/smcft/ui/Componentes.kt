package cl.duoc.smcft.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
val FORMATO_FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

fun LocalDate.texto(): String = format(FORMATO_FECHA)
fun LocalDateTime.texto(): String = format(FORMATO_FECHA_HORA)

fun parsearFecha(texto: String): LocalDate? =
    runCatching { LocalDate.parse(texto.trim(), FORMATO_FECHA) }.getOrNull()

fun usd(valor: Double): String = "USD %,.2f".format(valor)

@Composable
fun Tarjeta(titulo: String? = null, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

/** Fila desplazable de chips para elegir una opción. */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
) {
    Text(etiqueta, style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { op ->
            FilterChip(selected = op == seleccionado, onClick = { alElegir(op) }, label = { Text(texto(op)) })
        }
    }
}

@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    numerico: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier,
    )
}

@Composable
fun Estado(estado: String) {
    val color = when (estado) {
        "ACTIVO", "VIGENTE", "AUTORIZADO", "GENERADO", "COMPLETADA" -> Color(0xFF2E7D32)
        "VENCIDO", "ANULADO", "RECHAZADO" -> Color(0xFFC62828)
        else -> Color(0xFF8C5A1E)
    }
    AssistChip(onClick = {}, label = { Text(estado, color = color) })
}

@Composable
fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
