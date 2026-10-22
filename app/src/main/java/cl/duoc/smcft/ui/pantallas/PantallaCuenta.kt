package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.seguridad.Permiso
import cl.duoc.smcft.ui.LocalSesion
import cl.duoc.smcft.ui.colorDeRol
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.VarianteBoton

/** Descripción simple de cada permiso para el usuario. */
private fun textoPermiso(p: Permiso) = when (p) {
    Permiso.REGISTRAR_CRUCE -> "Registrar entradas y salidas"
    Permiso.DECLARAR_ADUANA -> "Agregar declaraciones de Aduana"
    Permiso.DECLARAR_SAG -> "Agregar declaraciones SAG"
    Permiso.GESTIONAR_PLAZOS -> "Actualizar plazos de admisión"
    Permiso.GESTIONAR_DOCUMENTOS -> "Crear, modificar y eliminar documentos"
    Permiso.GESTIONAR_PERSONAS -> "Registrar viajeros y autorizar menores"
    Permiso.GESTIONAR_VEHICULOS -> "Registrar vehículos"
    Permiso.GENERAR_REPORTES -> "Generar reportes estadísticos"
    Permiso.VER_AUDITORIA -> "Consultar la auditoría"
    Permiso.ADMINISTRAR -> "Administrar usuarios y la conexión"
}

@Composable
fun PantallaCuenta(vm: SmcftViewModel) {
    val c = coloresEstado
    val sesion = LocalSesion.current!!
    val (color, fondo) = colorDeRol(sesion.tipo)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Tarjeta {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(sesion.nombre, 84.dp)
                Text(sesion.nombre, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Texto2(sesion.email)
                Pildora(sesion.tipo.nombre, color, fondo, iconoDeRol(sesion.tipo))
                Text(sesion.tipo.descripcion, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Tarjeta(titulo = "Qué puedo hacer", subtitulo = "Todos los perfiles pueden consultar cruces, plazos y documentos") {
            Permiso.entries.forEach { p ->
                val si = sesion.puede(p)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (si) Icons.Outlined.CheckCircle else Icons.Outlined.RemoveCircleOutline, null,
                        tint = if (si) c.exito else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        textoPermiso(p),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (si) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        Boton("Cerrar sesión", vm::cerrarSesion, variante = VarianteBoton.CONTORNO, icono = Icons.AutoMirrored.Outlined.Logout)
    }
}
