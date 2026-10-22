package cl.duoc.smcft.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.smcft.core.seguridad.TipoUsuario

/*
 * Paleta institucional SMCFT
 *   Azul institucional  #0B3D6B  (marca, botones principales, navegación)
 *   Verde azulado       #0E7C7B  (acento, gráficos)
 *   Ámbar               #B26A00  (advertencias)
 *   Rojo                #B42318  (errores, vencidos)
 *   Verde               #15803D  (correcto, vigente)
 *   Grises azulados para fondos y textos secundarios
 */

/** Colores de estado que se usan en píldoras, íconos e indicadores. */
@Immutable
data class ColoresEstado(
    val exito: Color, val exitoSuave: Color,
    val alerta: Color, val alertaSuave: Color,
    val peligro: Color, val peligroSuave: Color,
    val info: Color, val infoSuave: Color,
    val neutro: Color, val neutroSuave: Color,
    val violeta: Color, val violetaSuave: Color,
    val degradadoInicio: Color, val degradadoMedio: Color, val degradadoFin: Color,
)

private val EstadoClaro = ColoresEstado(
    exito = Color(0xFF15803D), exitoSuave = Color(0xFFDCF5E3),
    alerta = Color(0xFFB26A00), alertaSuave = Color(0xFFFFEFD3),
    peligro = Color(0xFFB42318), peligroSuave = Color(0xFFFDE4E1),
    info = Color(0xFF1D5FAE), infoSuave = Color(0xFFDDEAFB),
    neutro = Color(0xFF526070), neutroSuave = Color(0xFFE8EDF3),
    violeta = Color(0xFF6941C6), violetaSuave = Color(0xFFEDE7FB),
    degradadoInicio = Color(0xFF082A4D), degradadoMedio = Color(0xFF0B3D6B), degradadoFin = Color(0xFF0E7C7B),
)

private val EstadoOscuro = ColoresEstado(
    exito = Color(0xFF6EE7A0), exitoSuave = Color(0xFF123A24),
    alerta = Color(0xFFFFC266), alertaSuave = Color(0xFF3E2A08),
    peligro = Color(0xFFFF9C90), peligroSuave = Color(0xFF4A1712),
    info = Color(0xFF93C1FF), infoSuave = Color(0xFF15304F),
    neutro = Color(0xFFB2BFCD), neutroSuave = Color(0xFF252E39),
    violeta = Color(0xFFC4B2FF), violetaSuave = Color(0xFF2E2350),
    degradadoInicio = Color(0xFF061D35), degradadoMedio = Color(0xFF0A2F52), degradadoFin = Color(0xFF0B5E5D),
)

val LocalColoresEstado = staticCompositionLocalOf { EstadoClaro }

private val Claro = lightColorScheme(
    primary = Color(0xFF0B3D6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E5F5),
    onPrimaryContainer = Color(0xFF041C33),
    secondary = Color(0xFF0E7C7B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F0EE),
    onSecondaryContainer = Color(0xFF002726),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6C2),
    background = Color(0xFFF3F6FA),
    onBackground = Color(0xFF111A24),
    surface = Color(0xFFF3F6FA),
    onSurface = Color(0xFF111A24),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFD),
    surfaceContainer = Color(0xFFEDF1F6),
    surfaceContainerHigh = Color(0xFFE5EAF1),
    surfaceContainerHighest = Color(0xFFDDE3EB),
    surfaceVariant = Color(0xFFE2E8EF),
    onSurfaceVariant = Color(0xFF526070),
    outline = Color(0xFF8592A2),
    outlineVariant = Color(0xFFD5DCE5),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFDE4E1),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF9CC5F2),
    onPrimary = Color(0xFF042443),
    primaryContainer = Color(0xFF123F6B),
    onPrimaryContainer = Color(0xFFD7E5F5),
    secondary = Color(0xFF6FD6D2),
    onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF0B4F4E),
    onSecondaryContainer = Color(0xFFD0F0EE),
    tertiary = Color(0xFFFFC266),
    tertiaryContainer = Color(0xFF5C3700),
    background = Color(0xFF0C1117),
    onBackground = Color(0xFFE3E8EE),
    surface = Color(0xFF0C1117),
    onSurface = Color(0xFFE3E8EE),
    surfaceContainerLowest = Color(0xFF131A22),
    surfaceContainerLow = Color(0xFF151C25),
    surfaceContainer = Color(0xFF19212B),
    surfaceContainerHigh = Color(0xFF212A35),
    surfaceContainerHighest = Color(0xFF29333F),
    surfaceVariant = Color(0xFF29333F),
    onSurfaceVariant = Color(0xFFAAB6C4),
    outline = Color(0xFF6B7888),
    outlineVariant = Color(0xFF2E3946),
    error = Color(0xFFFF9C90),
    errorContainer = Color(0xFF4A1712),
)

private val base = Typography()

private val Tipografia = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = base.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmcftTema(contenido: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(LocalColoresEstado provides if (oscuro) EstadoOscuro else EstadoClaro) {
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

/** Color que identifica a cada tipo de usuario (insignias y avatares). */
@Composable
fun colorDeRol(tipo: TipoUsuario): Pair<Color, Color> {
    val c = coloresEstado
    return when (tipo) {
        TipoUsuario.ADMINISTRADOR -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        TipoUsuario.OFICIAL_PDI -> c.info to c.infoSuave
        TipoUsuario.FUNCIONARIO_ADUANA -> c.alerta to c.alertaSuave
        TipoUsuario.INSPECTOR_SAG -> c.exito to c.exitoSuave
        TipoUsuario.SUPERVISOR -> c.violeta to c.violetaSuave
    }
}
