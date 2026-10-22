package cl.duoc.smcft.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.Ajustes
import cl.duoc.smcft.Modo
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.seguridad.CuentaPrueba
import cl.duoc.smcft.core.seguridad.Seguridad
import cl.duoc.smcft.core.seguridad.TipoUsuario
import cl.duoc.smcft.ui.colorDeRol
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Aviso
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Texto2

fun iconoDeRol(tipo: TipoUsuario): ImageVector = when (tipo) {
    TipoUsuario.ADMINISTRADOR -> Icons.Outlined.AdminPanelSettings
    TipoUsuario.OFICIAL_PDI -> Icons.Outlined.Badge
    TipoUsuario.FUNCIONARIO_ADUANA -> Icons.Outlined.Inventory2
    TipoUsuario.INSPECTOR_SAG -> Icons.Outlined.Spa
    TipoUsuario.SUPERVISOR -> Icons.Outlined.Assessment
}

@Composable
fun PantallaLogin(vm: SmcftViewModel, ajustes: Ajustes, alConfigurar: () -> Unit) {
    val c = coloresEstado
    var email by remember { mutableStateOf(vm.ultimoCorreo) }
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }
    var ingresando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun ingresar() {
        if (email.isBlank() || clave.isBlank() || ingresando) return
        ingresando = true
        error = null
        vm.iniciarSesion(email, clave) { e -> ingresando = false; error = e }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.degradadoInicio, c.degradadoMedio, c.degradadoFin))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Marca
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SMCFT", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Sistema de Monitoreo y Control\nFronterizo Terrestre",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            // Formulario
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
                        Texto2("Ingrese con su correo institucional para acceder a las funciones de su perfil.")
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); error = null },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Outlined.Mail, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it; error = null },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { verClave = !verClave }) {
                                Icon(if (verClave) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    if (verClave) "Ocultar contraseña" else "Mostrar contraseña")
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { ingresar() }),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnimatedVisibility(error != null) {
                        Aviso(error.orEmpty(), Icons.Outlined.ErrorOutline, c.peligro, c.peligroSuave)
                    }
                    Boton(
                        "Ingresar",
                        alPulsar = ::ingresar,
                        icono = Icons.AutoMirrored.Outlined.Login,
                        habilitado = email.isNotBlank() && clave.isNotBlank(),
                        cargando = ingresando,
                    )
                }
            }

            // Cuentas de prueba
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cuentas de prueba", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Toque un perfil para completar el correo y la contraseña.",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Seguridad.CUENTAS_PRUEBA.forEach { cuenta ->
                        FilaCuenta(cuenta, seleccionada = email.equals(cuenta.email, true)) {
                            email = cuenta.email
                            clave = cuenta.clave
                            error = null
                        }
                    }
                }
            }

            // Servidor
            Row(verticalAlignment = Alignment.CenterVertically) {
                val oracle = ajustes.modo == Modo.ORACLE
                Pildora(
                    if (oracle) "Servidor: Oracle Cloud" else "Modo demostración",
                    Color.White, Color.White.copy(alpha = 0.16f),
                    if (oracle) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = alConfigurar) {
                    Icon(Icons.Outlined.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Configurar", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FilaCuenta(cuenta: CuentaPrueba, seleccionada: Boolean, alElegir: () -> Unit) {
    val (color, fondo) = colorDeRol(cuenta.tipo)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (seleccionada) Color.White else Color.White.copy(alpha = 0.92f))
            .clickable(onClick = alElegir)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(fondo), contentAlignment = Alignment.Center) {
            Icon(iconoDeRol(cuenta.tipo), null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cuenta.tipo.nombre, style = MaterialTheme.typography.titleSmall, color = Color(0xFF111A24))
            Text(cuenta.email, style = MaterialTheme.typography.bodySmall, color = Color(0xFF526070))
        }
        if (seleccionada) Pildora("Elegida", color, fondo)
    }
}
