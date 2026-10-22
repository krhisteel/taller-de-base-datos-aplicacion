package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.BotonPrincipal
import cl.duoc.smcft.ui.componentes.Campo
import cl.duoc.smcft.ui.componentes.DialogoConfirmar
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import kotlinx.coroutines.launch

@Composable
fun PantallaConexion(vm: SmcftViewModel, actuales: Ajustes) {
    val c = coloresEstado
    val alcance = rememberCoroutineScope()
    var modo by remember { mutableStateOf(actuales.modo) }
    var url by remember { mutableStateOf(actuales.urlOrds) }
    var clientId by remember { mutableStateOf(actuales.clientId) }
    var secreto by remember { mutableStateOf(actuales.clientSecret) }
    var idUsuario by remember { mutableStateOf(actuales.idUsuarioReportes.toString()) }
    var probando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Result<String>?>(null) }
    var confirmarReinicio by remember { mutableStateOf(false) }

    val nuevos = Ajustes(modo, url.trim(), clientId.trim(), secreto.trim(), idUsuario.toLongOrNull() ?: 1)
    val urlValida = modo == Modo.DEMO || url.startsWith("http://") || url.startsWith("https://")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OpcionModo(
            seleccionado = modo == Modo.DEMO,
            icono = Icons.Outlined.PhoneAndroid,
            titulo = "Modo demostración",
            descripcion = "Los datos se guardan en el teléfono con las mismas reglas del informe. Funciona sin internet.",
        ) { modo = Modo.DEMO }
        OpcionModo(
            seleccionado = modo == Modo.ORACLE,
            icono = Icons.Outlined.Cloud,
            titulo = "Oracle Cloud (ORDS)",
            descripcion = "Usa el esquema SMCFT real: el package, los procedimientos, las funciones y los triggers se ejecutan en Oracle.",
        ) { modo = Modo.ORACLE }

        if (modo == Modo.ORACLE) {
            Tarjeta(titulo = "Datos de conexión", subtitulo = "Los entrega el script sql/ords/03_seguridad_oauth.sql", icono = Icons.Outlined.Link) {
                Campo("URL del esquema", url, { url = it }, icono = Icons.Outlined.Link, tipo = KeyboardType.Uri,
                    error = if (!urlValida && url.isNotBlank()) "Debe comenzar con https://" else null,
                    ayuda = "Ej: https://xxxx.adb.sa-santiago-1.oraclecloudapps.com/ords/smcft/")
                Campo("client_id", clientId, { clientId = it }, icono = Icons.Outlined.Person)
                Campo("client_secret", secreto, { secreto = it }, icono = Icons.Outlined.Key, secreto = true)
                Campo("ID de usuario para reportes", idUsuario, { idUsuario = it.filter(Char::isDigit) }, tipo = KeyboardType.Number,
                    ayuda = "ID_USUARIO que figura como generador (por defecto 1)")
                cl.duoc.smcft.ui.componentes.Boton(
                    "Probar conexión",
                    alPulsar = {
                        probando = true
                        alcance.launch {
                            resultado = vm.probarConexion(nuevos)
                            probando = false
                        }
                    },
                    variante = cl.duoc.smcft.ui.componentes.VarianteBoton.CONTORNO,
                    icono = Icons.Outlined.NetworkCheck,
                    habilitado = url.isNotBlank() && urlValida,
                    cargando = probando,
                )
                resultado?.let { r ->
                    r.onSuccess { Pildora(it, c.exito, c.exitoSuave) }
                    r.onFailure { Text(it.message ?: "Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        BotonPrincipal(
            "Guardar y usar",
            icono = Icons.Outlined.Save,
            habilitado = urlValida && (modo == Modo.DEMO || url.isNotBlank()),
            alPulsar = { vm.guardarAjustes(nuevos) },
        )

        if (actuales.modo == Modo.DEMO) {
            Tarjeta(titulo = "Datos de demostración", icono = Icons.Outlined.RestartAlt) {
                Texto2("Vuelve a los datos iniciales del informe y a las cuentas de prueba. Se borra todo lo registrado y se cierra la sesión.")
                cl.duoc.smcft.ui.componentes.Boton("Reiniciar datos", { confirmarReinicio = true },
                    variante = cl.duoc.smcft.ui.componentes.VarianteBoton.CONTORNO, icono = Icons.Outlined.RestartAlt)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Texto2("Las credenciales quedan guardadas solo en este teléfono.")
        }
    }

    if (confirmarReinicio) {
        DialogoConfirmar(
            titulo = "¿Reiniciar los datos?",
            mensaje = "Se borrarán los cruces, documentos y reportes registrados en el modo demostración.",
            textoConfirmar = "Reiniciar",
            peligroso = true,
            alConfirmar = vm::reiniciarDemo,
            alCerrar = { confirmarReinicio = false },
        )
    }
}

@Composable
private fun OpcionModo(seleccionado: Boolean, icono: ImageVector, titulo: String, descripcion: String, alElegir: () -> Unit) {
    val borde = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, borde),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = alElegir),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Texto2(descripcion)
            }
            RadioButton(selected = seleccionado, onClick = alElegir)
        }
    }
}
