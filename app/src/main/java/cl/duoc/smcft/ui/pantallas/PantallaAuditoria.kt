package cl.duoc.smcft.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.smcft.core.db.Tablas
import cl.duoc.smcft.ui.Estado
import cl.duoc.smcft.ui.Etiqueta
import cl.duoc.smcft.ui.Tarjeta
import cl.duoc.smcft.ui.texto

@Composable
fun PantallaAuditoria(tablas: Tablas) {
    val auditorias = tablas.auditorias.sortedByDescending { it.idAuditoria }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Etiqueta("AUTOMATICA_TRIGGER: auditoría abierta con un detalle por documento (TRG_DOCUMENTO_AUDIT). TRIGGER_SENTENCIA: una por instrucción (TRG_DOCUMENTO_SENTENCIA).")
        }
        if (auditorias.isEmpty()) item { Etiqueta("Aún no hay registros. Modifique un documento para generarlos.") }
        items(auditorias, key = { it.idAuditoria }) { a ->
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${a.idAuditoria} · ${a.tipoAuditoria}", modifier = Modifier.weight(1f))
                    Estado(a.estado)
                }
                Etiqueta(a.descripcion)
                Etiqueta("Inicio: ${a.fechaInicio.texto()}" + (a.fechaCierre?.let { " · Cierre: ${it.texto()}" } ?: ""))
                val detalles = tablas.auditoriaDetalles.filter { it.idAuditoria == a.idAuditoria }.sortedByDescending { it.idDetalle }
                if (detalles.isNotEmpty()) {
                    HorizontalDivider()
                    Text("${detalles.size} detalle(s)")
                    detalles.forEach { d -> Etiqueta("• ${d.fechaAccion.texto()} · ${d.descripcion} · ${d.resultado}") }
                }
            }
        }
    }
}
