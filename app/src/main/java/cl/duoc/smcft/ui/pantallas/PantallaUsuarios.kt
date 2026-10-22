package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.SmcftViewModel
import cl.duoc.smcft.core.repo.AccesoInfo
import cl.duoc.smcft.core.repo.UsuarioInfo
import cl.duoc.smcft.core.seguridad.TipoUsuario
import cl.duoc.smcft.ui.colorDeRol
import cl.duoc.smcft.ui.coloresEstado
import cl.duoc.smcft.ui.componentes.Avatar
import cl.duoc.smcft.ui.componentes.Cargando
import cl.duoc.smcft.ui.componentes.EstadoVacio
import cl.duoc.smcft.ui.componentes.IconoCirculo
import cl.duoc.smcft.ui.componentes.Indicador
import cl.duoc.smcft.ui.componentes.Pildora
import cl.duoc.smcft.ui.componentes.Tarjeta
import cl.duoc.smcft.ui.componentes.Texto2
import cl.duoc.smcft.ui.componentes.texto

@Composable
fun PantallaUsuarios(vm: SmcftViewModel) {
    val c = coloresEstado
    var vista by rememberSaveable { mutableStateOf(0) }
    var usuarios by remember { mutableStateOf<List<UsuarioInfo>?>(null) }
    var accesos by remember { mutableStateOf<List<AccesoInfo>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            usuarios = vm.consultar { usuarios() }
            accesos = vm.consultar { accesos() }
        }.onFailure { error = it.message }
    }

    val u = usuarios
    val a = accesos
    if (u == null || a == null) {
        if (error != null) EstadoVacio(Icons.Outlined.ErrorOutline, "No se pudo cargar", error!!) else Cargando()
        return
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Indicador("Usuarios activos", u.count { it.activo }.toString(), Icons.Outlined.Groups, MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                Indicador("Accesos correctos", a.count { it.exitoso }.toString(), Icons.Outlined.CheckCircle, c.exito, c.exitoSuave, Modifier.weight(1f))
                Indicador("Intentos fallidos", a.count { !it.exitoso }.toString(), Icons.Outlined.ErrorOutline, c.peligro, c.peligroSuave, Modifier.weight(1f))
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Usuarios" to Icons.Outlined.ManageAccounts, "Accesos" to Icons.Outlined.Login).forEachIndexed { i, (texto, icono) ->
                    SegmentedButton(
                        selected = vista == i,
                        onClick = { vista = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = { androidx.compose.material3.Icon(icono, null) },
                    ) { Text(texto) }
                }
            }
        }
        if (vista == 0) {
            items(u, key = { "u${it.idUsuario}" }) { FilaUsuario(it) }
        } else {
            if (a.isEmpty()) item { EstadoVacio(Icons.Outlined.Login, "Sin accesos registrados", "Aquí aparecerán los inicios de sesión.") }
            items(a, key = { "a${it.idIntento}" }) { FilaAcceso(it) }
        }
    }
}

@Composable
private fun FilaUsuario(u: UsuarioInfo) {
    val c = coloresEstado
    val tipo = TipoUsuario.entries.firstOrNull { it.codigo == u.rol }
    Tarjeta(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.nombre, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(u.nombre, style = MaterialTheme.typography.titleSmall)
                Texto2(u.email)
                Texto2(u.ultimoAcceso?.let { "Último acceso: ${it.texto()}" } ?: "Sin accesos registrados")
            }
            if (!u.activo) Pildora("Inactivo", c.peligro, c.peligroSuave)
        }
        if (tipo != null) {
            val (color, fondo) = colorDeRol(tipo)
            Pildora(tipo.nombre, color, fondo, iconoDeRol(tipo))
        } else {
            Pildora(u.rol, c.neutro, c.neutroSuave)
        }
    }
}

@Composable
private fun FilaAcceso(a: AccesoInfo) {
    val c = coloresEstado
    Tarjeta(relleno = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(
                if (a.exitoso) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                if (a.exitoso) c.exito else c.peligro,
                if (a.exitoso) c.exitoSuave else c.peligroSuave,
                38.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.nombre ?: "Correo no registrado", style = MaterialTheme.typography.titleSmall)
                Texto2(listOfNotNull(a.email, a.fecha.texto()).joinToString(" · "))
            }
            Pildora(if (a.exitoso) "Correcto" else "Fallido", if (a.exitoso) c.exito else c.peligro, if (a.exitoso) c.exitoSuave else c.peligroSuave)
        }
    }
}
