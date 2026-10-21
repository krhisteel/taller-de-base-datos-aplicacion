package cl.duoc.smcft.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF1E4D8C),
    secondary = Color(0xFF3A6EA5),
    tertiary = Color(0xFF8C5A1E),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    secondary = Color(0xFFB7C8E6),
    tertiary = Color(0xFFF0BD8B),
)

@Composable
fun SmcftTema(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}
