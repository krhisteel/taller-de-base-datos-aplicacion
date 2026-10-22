package cl.duoc.smcft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.ui.componentes.Aviso
import cl.duoc.smcft.ui.componentes.Boton
import cl.duoc.smcft.ui.componentes.Texto2

/** Explicación de una pantalla: para qué sirve, cómo se usa y qué hace por dentro. */
data class ContenidoAyuda(
    val titulo: String,
    val resumen: String,
    val pasos: List<String>,
    val consejo: String? = null,
    val tecnico: String? = null,
)

object TextosAyuda {
    val INICIO = ContenidoAyuda(
        "Panel de inicio",
        "Muestra el estado del paso fronterizo de un vistazo y los accesos a las tareas de su perfil.",
        listOf(
            "Las tarjetas de números resumen los cruces, viajeros, vehículos y plazos. Tóquelas para ir a cada sección.",
            "Si hay plazos vencidos o por vencer, aparece un aviso con los vehículos afectados.",
            "En \"Mis tareas\" están las acciones que su perfil puede realizar.",
        ),
        consejo = "Deslice la pantalla hacia abajo para actualizar los datos.",
        tecnico = "El número de plazos vencidos lo calcula la función FN_TOTAL_PLAZOS_VENCIDOS.",
    )
    val CRUCES = ContenidoAyuda(
        "Cruces fronterizos",
        "Lista de todas las entradas y salidas registradas en los pasos fronterizos.",
        listOf(
            "Use el buscador para encontrar un cruce por paso, patente o número.",
            "Filtre por entradas, salidas, vehiculares o peatonales.",
            "Toque un cruce para ver sus viajeros y lo que declararon en Aduana o SAG.",
            "Dentro del detalle puede agregar ítems a la declaración si su perfil lo permite.",
        ),
        tecnico = "El valor declarado de cada cruce lo calcula FN_VALOR_DECLARADO dentro de la consulta.",
    )
    val NUEVO_CRUCE = ContenidoAyuda(
        "Registrar un cruce",
        "Registra a las personas (y su vehículo, si corresponde) que entran o salen del país.",
        listOf(
            "Elija el paso fronterizo y si es una entrada o una salida.",
            "Elija el vehículo, o \"Peatonal\" si cruzan a pie.",
            "Marque a los viajeros. Si hay vehículo, toque la estrella para indicar quién conduce.",
            "Revise el resumen: le avisa si algo impedirá el registro, por ejemplo un menor sin autorización.",
            "Toque \"Registrar cruce\".",
        ),
        consejo = "Al registrar la salida de un vehículo, el sistema crea solo su plazo de admisión temporal.",
        tecnico = "Usa PKG_GESTION_FRONTERA.sp_registrar_cruce. El plazo lo crea el trigger TRG_CRUCE_PLAZO_AI (90 días diplomáticos, 180 el resto).",
    )
    val PLAZOS = ContenidoAyuda(
        "Plazos de admisión temporal",
        "Controla cuántos días puede permanecer fuera del país cada vehículo que salió.",
        listOf(
            "Cada tarjeta muestra la patente, el tipo de plazo y una barra con el tiempo transcurrido.",
            "Verde: con tiempo. Ámbar: vence en 30 días o menos. Rojo: vencido.",
            "\"Actualizar estados\" marca como vencidos los plazos cuya fecha ya pasó.",
        ),
        tecnico = "Los estados los actualiza el procedimiento SP_ACTUALIZAR_PLAZOS.",
    )
    val DOCUMENTOS = ContenidoAyuda(
        "Documentos",
        "Cédulas, pasaportes, autorizaciones y documentos de vehículos.",
        listOf(
            "Busque por número, titular o tipo, y filtre por estado.",
            "Con el botón ⋮ de cada documento puede cambiar su estado o eliminarlo.",
            "\"Marcar vencidos\" actualiza de una vez todos los documentos con fecha de vencimiento pasada.",
            "Todos los cambios quedan registrados en la sección Auditoría.",
        ),
        tecnico = "Cada cambio dispara TRG_DOCUMENTO_AUDIT (uno por documento) y TRG_DOCUMENTO_SENTENCIA (uno por instrucción).",
    )
    val REPORTES = ContenidoAyuda(
        "Reportes estadísticos",
        "Resume la actividad de cada paso fronterizo en un período.",
        listOf(
            "Elija un período rápido (hoy, 7 o 30 días) o fechas exactas.",
            "Toque \"Generar reporte\".",
            "Toque un reporte para ver sus gráficos por paso: cruces, viajeros y monto declarado.",
        ),
        tecnico = "Usa el procedimiento SP_REPORTE_CRUCES. La validación privada validar_rango rechaza fechas invertidas (error -20020).",
    )
    val AUDITORIA = ContenidoAyuda(
        "Auditoría",
        "Historial automático de todos los cambios realizados sobre los documentos.",
        listOf(
            "\"Por documento\" muestra cada documento insertado, modificado o eliminado.",
            "\"Por instrucción\" muestra una entrada por cada operación, aunque haya afectado a varios documentos.",
            "Toque una auditoría para ver su línea de tiempo.",
        ),
        tecnico = "Registros de las tablas AUDITORIA y AUDITORIA_DETALLE creados por los triggers.",
    )
    val PERSONAS = ContenidoAyuda(
        "Viajeros",
        "Personas registradas en el sistema y autorizaciones de viaje de menores.",
        listOf(
            "Use \"Nueva persona\" para registrar a un viajero. El RUT se valida con su dígito verificador.",
            "Los menores sin autorización aparecen en rojo y no pueden cruzar.",
            "Toque \"Autorizar\" para registrar la autorización notarial de un menor.",
        ),
    )
    val VEHICULOS = ContenidoAyuda(
        "Vehículos",
        "Vehículos registrados y su tipo, que define el plazo de admisión temporal.",
        listOf(
            "Los vehículos diplomáticos tienen 90 días; los demás, 180.",
            "Si un vehículo está fuera del país, se muestran los días que le quedan.",
            "Use \"Nuevo vehículo\" para registrar uno.",
        ),
    )
    val USUARIOS = ContenidoAyuda(
        "Usuarios y accesos",
        "Cuentas que pueden usar la app y el registro de inicios de sesión.",
        listOf(
            "\"Usuarios\" muestra cada cuenta con su perfil y su último acceso.",
            "\"Accesos\" muestra los últimos intentos de inicio de sesión, correctos e incorrectos.",
        ),
        tecnico = "Tablas USUARIO, ROL, USUARIO_ROL y LOGIN_ATTEMPT. Las contraseñas se guardan cifradas con SHA-256.",
    )
    val CONEXION = ContenidoAyuda(
        "Conexión",
        "Define dónde se guardan los datos de la app.",
        listOf(
            "Modo demostración: los datos quedan en este teléfono. Ideal para practicar.",
            "Oracle Cloud: la app trabaja con la base de datos real del proyecto.",
            "Para Oracle, ingrese la URL, el client_id y el client_secret que entrega el script de instalación y toque \"Probar conexión\".",
        ),
        tecnico = "La conexión usa Oracle REST Data Services (ORDS) con OAuth 2.0.",
    )
}

/** Botón "?" que abre la ayuda de la pantalla. */
@Composable
fun BotonAyuda(ayuda: ContenidoAyuda) {
    var abierta by remember { mutableStateOf(false) }
    IconButton(onClick = { abierta = true }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Ayuda") }
    if (abierta) HojaAyuda(ayuda) { abierta = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAyuda(ayuda: ContenidoAyuda, alCerrar: () -> Unit) {
    val c = coloresEstado
    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(ayuda.titulo, style = MaterialTheme.typography.headlineSmall)
            Text(ayuda.resumen, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Cómo se usa", style = MaterialTheme.typography.titleMedium)
            ayuda.pasos.forEachIndexed { i, paso ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium) }
                    Spacer(Modifier.width(12.dp))
                    Text(paso, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                }
            }
            ayuda.consejo?.let { Aviso(it, Icons.Outlined.Lightbulb, c.alerta, c.alertaSuave) }
            ayuda.tecnico?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Texto2("Detalle técnico: $it")
                }
            }
            Boton("Entendido", alCerrar)
        }
    }
}
