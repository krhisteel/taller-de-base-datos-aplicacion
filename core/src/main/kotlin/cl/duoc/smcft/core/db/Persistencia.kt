package cl.duoc.smcft.core.db

import kotlinx.serialization.json.Json
import java.io.File

/** Guarda el estado confirmado (COMMIT) de la base en un archivo JSON. */
class ArchivoJson(private val archivo: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun cargar(): Tablas? =
        archivo.takeIf { it.exists() }?.let { runCatching { json.decodeFromString(Tablas.serializer(), it.readText()) }.getOrNull() }

    fun guardar(tablas: Tablas) {
        val tmp = File(archivo.parentFile, archivo.name + ".tmp")
        tmp.writeText(json.encodeToString(Tablas.serializer(), tablas))
        if (!tmp.renameTo(archivo)) {
            archivo.delete()
            tmp.renameTo(archivo)
        }
    }
}
