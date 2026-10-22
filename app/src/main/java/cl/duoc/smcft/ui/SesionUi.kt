package cl.duoc.smcft.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.core.seguridad.Sesion

/** Usuario conectado, disponible para todas las pantallas. */
val LocalSesion = staticCompositionLocalOf<Sesion?> { null }

/** true si el usuario conectado tiene el [permiso]. */
@Composable
fun puede(permiso: Permiso): Boolean = LocalSesion.current?.puede(permiso) == true
