package cl.duoc.smcft.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.smcft.ui.coloresEstado
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ======================================================================= formato

private val LOCALE_CL = Locale("es", "CL")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_CL)
private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", LOCALE_CL)
private val FORMATO_CORTO = DateTimeFormatter.ofPattern("dd-MM-yyyy")

fun LocalDate.texto(): String = format(FORMATO_FECHA)
fun LocalDate.textoCorto(): String = format(FORMATO_CORTO)
fun LocalDateTime.texto(): String = format(FORMATO_FECHA_HORA)

fun usd(valor: Double): String = "US$ " + String.format(LOCALE_CL, "%,.2f", valor)

fun numero(valor: Double): String =
    if (valor % 1.0 == 0.0) String.format(LOCALE_CL, "%,d", valor.toLong()) else String.format(LOCALE_CL, "%,.2f", valor)

/** "TIPO_DOCUMENTO" -> "Tipo documento" */
fun legible(codigo: String): String =
    codigo.replace('_', ' ').lowercase(LOCALE_CL).replaceFirstChar { it.titlecase(LOCALE_CL) }

// ======================================================================= estados

data class ParColores(val texto: Color, val fondo: Color)

@Composable
fun coloresDe(estado: String): ParColores {
    val c = coloresEstado
    return when (estado.uppercase()) {
        "ACTIVO", "VIGENTE", "AUTORIZADO", "GENERADO", "COMPLETADA", "OK", "ENTRADA", "CONDUCTOR" -> ParColores(c.exito, c.exitoSuave)
        "VENCIDO", "ANULADO", "RECHAZADO", "ERROR", "DELETE" -> ParColores(c.peligro, c.peligroSuave)
        "ABIERTA", "EN_PROCESO", "REVISADO", "SALIDA", "UPDATE" -> ParColores(c.alerta, c.alertaSuave)
        "INSERT", "DIPLOMATICO", "ACOMPANANTE" -> ParColores(c.info, c.infoSuave)
        else -> ParColores(c.neutro, c.neutroSuave)
    }
}

/** Píldora de color con el estado. */
@Composable
fun ChipEstado(estado: String, texto: String = legible(estado), icono: ImageVector? = null) {
    val col = coloresDe(estado)
    Pildora(texto, col.texto, col.fondo, icono)
}

@Composable
fun Pildora(texto: String, color: Color, fondo: Color, icono: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icono != null) Icon(icono, null, tint = color, modifier = Modifier.size(14.dp))
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

// ======================================================================= contenedores

/** Tarjeta blanca con esquinas redondeadas y título opcional. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    subtitulo: String? = null,
    icono: ImageVector? = null,
    accion: (@Composable () -> Unit)? = null,
    alPulsar: (() -> Unit)? = null,
    relleno: Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = MaterialTheme.shapes.large
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (alPulsar != null) Modifier.clip(forma).clickable(onClick = alPulsar) else Modifier),
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(relleno), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        IconoCirculo(icono, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, 36.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(titulo, style = MaterialTheme.typography.titleMedium)
                        if (subtitulo != null) Texto2(subtitulo)
                    }
                    accion?.invoke()
                }
            }
            contenido()
        }
    }
}

@Composable
fun IconoCirculo(icono: ImageVector, color: Color, fondo: Color, tamano: Dp = 44.dp) {
    Box(
        Modifier.size(tamano).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(tamano * 0.52f))
    }
}

/** Texto secundario (gris). */
@Composable
fun Texto2(texto: String, modifier: Modifier = Modifier, maxLineas: Int = Int.MAX_VALUE) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLineas,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        accion?.invoke(this)
    }
}

@Composable
fun FilaDato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, mensaje: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconoCirculo(icono, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, 72.dp)
        Spacer(Modifier.height(4.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Texto2(mensaje, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        accion?.invoke()
    }
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

// ======================================================================= identidad visual

private val PALETA_AVATAR = listOf(
    Color(0xFF1D5FA8), Color(0xFF13718A), Color(0xFF7B4FB0), Color(0xFFB45F06),
    Color(0xFF1E7B4F), Color(0xFFC0263B), Color(0xFF3C4A9E), Color(0xFF8A5A13),
)

/** Círculo con las iniciales del nombre. */
@Composable
fun Avatar(nombre: String, tamano: Dp = 44.dp) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    val color = PALETA_AVATAR[Math.floorMod(nombre.hashCode(), PALETA_AVATAR.size)]
    Box(
        Modifier.size(tamano).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales, color = color, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.36f).sp)
    }
}

/** Patente con estilo de placa chilena. */
@Composable
fun Patente(patente: String, grande: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1B1F24), RoundedCornerShape(6.dp))
            .padding(horizontal = if (grande) 12.dp else 8.dp, vertical = if (grande) 4.dp else 2.dp),
    ) {
        Text(
            patente.chunked(2).joinToString("·"),
            color = Color(0xFF1B1F24),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (grande) 18.sp else 13.sp,
            letterSpacing = 1.sp,
        )
    }
}

// ======================================================================= entrada de datos

@Composable
fun BarraBusqueda(valor: String, alCambiar: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (valor.isNotEmpty()) IconButton(onClick = { alCambiar("") }) { Icon(Icons.Outlined.Close, "Limpiar") }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
    )
}

/** Fila desplazable de chips de filtro. */
@Composable
fun <T> Filtros(
    opciones: List<T>,
    seleccionado: T,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(0.dp),
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(relleno),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opciones.forEach { op ->
            FilterChip(
                selected = op == seleccionado,
                onClick = { alElegir(op) },
                label = { Text(texto(op)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    tipo: KeyboardType = KeyboardType.Text,
    icono: ImageVector? = null,
    error: String? = null,
    secreto: Boolean = false,
    ayuda: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        modifier = modifier,
        leadingIcon = icono?.let { { Icon(it, null) } },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        visualTransformation = if (secreto) PasswordVisualTransformation() else VisualTransformation.None,
        shape = MaterialTheme.shapes.medium,
    )
}

/** Campo de fecha que abre el calendario de Material 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    alCambiar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = fecha?.textoCorto().orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
        )
        // Capa transparente que recibe el toque (el campo es de solo lectura).
        Box(
            Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { abierto = true },
        )
    }
    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { alCambiar(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

/** Variantes de botón: todas con la misma altura y forma para una interfaz consistente. */
enum class VarianteBoton { PRIMARIO, SECUNDARIO, CONTORNO, PELIGRO }

/** Botón estándar de la app. Muestra un indicador de progreso mientras [cargando]. */
@Composable
fun Boton(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    variante: VarianteBoton = VarianteBoton.PRIMARIO,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    compacto: Boolean = false,
) {
    val forma = MaterialTheme.shapes.medium
    val alto = if (compacto) 42.dp else 52.dp
    val relleno = PaddingValues(horizontal = if (compacto) 14.dp else 20.dp)
    val contenido: @Composable RowScope.() -> Unit = {
        when {
            cargando -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.material3.LocalContentColor.current)
            icono != null -> Icon(icono, null, Modifier.size(if (compacto) 18.dp else 20.dp))
        }
        if (cargando || icono != null) Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val activo = habilitado && !cargando
    when (variante) {
        VarianteBoton.PRIMARIO -> Button(alPulsar, modifier.height(alto), activo, forma, contentPadding = relleno, content = contenido)
        VarianteBoton.SECUNDARIO -> androidx.compose.material3.FilledTonalButton(alPulsar, modifier.height(alto), activo, forma, contentPadding = relleno, content = contenido)
        VarianteBoton.CONTORNO -> androidx.compose.material3.OutlinedButton(
            alPulsar, modifier.height(alto), activo, forma,
            border = BorderStroke(1.dp, if (activo) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
            contentPadding = relleno, content = contenido,
        )
        VarianteBoton.PELIGRO -> Button(
            alPulsar, modifier.height(alto), activo, forma,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            contentPadding = relleno, content = contenido,
        )
    }
}

/** Botón principal de ancho completo (atajo de [Boton]). */
@Composable
fun BotonPrincipal(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    habilitado: Boolean = true,
    guardando: Boolean = false,
    icono: ImageVector? = null,
) = Boton(texto, alPulsar, modifier, VarianteBoton.PRIMARIO, icono, habilitado, guardando)

/** Aviso para quien puede consultar una sección, pero no modificarla. */
@Composable
fun AvisoSoloLectura(texto: String = "Su perfil puede consultar esta sección, pero no modificarla.") {
    val c = coloresEstado
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(c.neutroSuave).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(androidx.compose.material.icons.Icons.Outlined.Visibility, null, tint = c.neutro, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = c.neutro)
    }
}

/** Mensaje destacado (información, advertencia o error). */
@Composable
fun Aviso(texto: String, icono: ImageVector, color: Color, fondo: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(fondo).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun DialogoConfirmar(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit,
    peligroso: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = { alConfirmar(); alCerrar() }) {
                Text(textoConfirmar, color = if (peligroso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

// ======================================================================= gráficos

/** Barra horizontal animada para comparar valores. */
@Composable
fun BarraValor(etiqueta: String, valor: Double, maximo: Double, color: Color, textoValor: String = numero(valor)) {
    val fraccion by animateFloatAsState(
        targetValue = if (maximo <= 0) 0f else (valor / maximo).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "barra",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(textoValor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraccion).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

/** Indicador de una cifra (tarjeta pequeña del panel). */
@Composable
fun Indicador(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    fondo: Color,
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(if (alPulsar != null) Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = alPulsar) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconoCirculo(icono, color, fondo, 38.dp)
            Text(valor, style = MaterialTheme.typography.headlineSmall)
            Texto2(titulo, maxLineas = 2)
        }
    }
}
