package cl.duoc.smcft.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colores de estado que se usan en chips, íconos e indicadores. */
@Immutable
data class ColoresEstado(
    val exito: Color,
    val exitoSuave: Color,
    val alerta: Color,
    val alertaSuave: Color,
    val peligro: Color,
    val peligroSuave: Color,
    val info: Color,
    val infoSuave: Color,
    val neutro: Color,
    val neutroSuave: Color,
    val degradadoInicio: Color,
    val degradadoFin: Color,
)

private val EstadoClaro = ColoresEstado(
    exito = Color(0xFF1E7B4F), exitoSuave = Color(0xFFDDF3E7),
    alerta = Color(0xFFB45F06), alertaSuave = Color(0xFFFFEBD1),
    peligro = Color(0xFFC0263B), peligroSuave = Color(0xFFFCE0E3),
    info = Color(0xFF1D5FA8), infoSuave = Color(0xFFDDE9F8),
    neutro = Color(0xFF5B6472), neutroSuave = Color(0xFFE9ECF1),
    degradadoInicio = Color(0xFF0E2A47), degradadoFin = Color(0xFF13718A),
)

private val EstadoOscuro = ColoresEstado(
    exito = Color(0xFF7BD8A6), exitoSuave = Color(0xFF16382A),
    alerta = Color(0xFFFFC27A), alertaSuave = Color(0xFF3D2A12),
    peligro = Color(0xFFFF9AA6), peligroSuave = Color(0xFF45161D),
    info = Color(0xFF9CC4FF), infoSuave = Color(0xFF16304F),
    neutro = Color(0xFFB4BCC8), neutroSuave = Color(0xFF2A3038),
    degradadoInicio = Color(0xFF0B2036), degradadoFin = Color(0xFF0F5568),
)

val LocalColoresEstado = staticCompositionLocalOf { EstadoClaro }

private val Claro = lightColorScheme(
    primary = Color(0xFF0E3B66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E4F7),
    onPrimaryContainer = Color(0xFF071D33),
    secondary = Color(0xFF13718A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEDF4),
    onSecondaryContainer = Color(0xFF032830),
    tertiary = Color(0xFFB45F06),
    tertiaryContainer = Color(0xFFFFE3C2),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFF4F6FA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color(0xFFEEF1F6),
    surfaceContainerHigh = Color(0xFFE7EBF1),
    surfaceVariant = Color(0xFFE1E6ED),
    onSurfaceVariant = Color(0xFF545E6B),
    outlineVariant = Color(0xFFD3D9E1),
    error = Color(0xFFC0263B),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFA9C9F2),
    onPrimary = Color(0xFF0A2540),
    primaryContainer = Color(0xFF173E66),
    onPrimaryContainer = Color(0xFFD5E4F7),
    secondary = Color(0xFF7FD0E2),
    onSecondary = Color(0xFF032830),
    secondaryContainer = Color(0xFF0F4E5E),
    onSecondaryContainer = Color(0xFFCDEDF4),
    tertiary = Color(0xFFFFC27A),
    tertiaryContainer = Color(0xFF5A3505),
    background = Color(0xFF0F1318),
    surface = Color(0xFF0F1318),
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceContainerLow = Color(0xFF151A20),
    surfaceContainer = Color(0xFF1A2027),
    surfaceContainerHigh = Color(0xFF222932),
    surfaceVariant = Color(0xFF2A313B),
    onSurfaceVariant = Color(0xFFB7C0CC),
    outlineVariant = Color(0xFF353D48),
    error = Color(0xFFFF9AA6),
)

private val base = Typography()

private val Tipografia = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmcftTema(contenido: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalColoresEstado provides if (oscuro) EstadoOscuro else EstadoClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = contenido,
        )
    }
}

/** Atajo para los colores de estado del tema actual. */
val coloresEstado: ColoresEstado
    @Composable get() = LocalColoresEstado.current
