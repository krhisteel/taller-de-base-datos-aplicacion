package cl.duoc.smcft.core.repo

import cl.duoc.smcft.core.db.SmcftException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64

/**
 * Cliente HTTP mínimo para el módulo REST `smcft.api` publicado con ORDS (sql/ords/02_api_rest.sql).
 *
 * - Autenticación OAuth 2.0 con client credentials (sql/ords/03_seguridad_oauth.sql). Si
 *   [clientId] está vacío no se envía token (módulo sin proteger).
 * - Las respuestas de error de los procesos ({"error_code", "error_msg"}) se convierten en
 *   [SmcftException] con el mismo código de Oracle.
 *
 * @param baseUrl URL del esquema, por ejemplo https://xxxx.adb.sa-santiago-1.oraclecloudapps.com/ords/smcft/
 */
class ClienteOrds(
    baseUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val timeoutMs: Int = 20_000,
) {
    private val base = baseUrl.trim().trimEnd('/') + "/"
    private val json = Json { ignoreUnknownKeys = true }

    private var token: String? = null
    private var tokenExpira: Long = 0

    /** GET de una consulta de una fila (json/item). */
    fun objeto(ruta: String): JsonObject = parsear(solicitar("GET", api(ruta), null)).jsonObject

    /** GET de una consulta de varias filas (json/collection). Sigue la paginación de ORDS si existe. */
    fun lista(ruta: String): List<JsonObject> {
        val filas = mutableListOf<JsonObject>()
        var offset = 0
        while (true) {
            val url = api(ruta) + if (offset > 0) "?offset=$offset" else ""
            val pagina = parsear(solicitar("GET", url, null)).jsonObject
            val items = pagina["items"]?.jsonArray?.map { it.jsonObject }.orEmpty()
            filas += items
            val hayMas = pagina["hasMore"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!hayMas || items.isEmpty()) return filas
            offset += items.size
        }
    }

    /**
     * POST/PUT/DELETE a un proceso. Siempre envía un cuerpo JSON (ORDS rechaza los cuerpos vacíos).
     * Devuelve el valor numérico de [resultado] (el ID creado o la cantidad de filas).
     */
    fun proceso(metodo: String, ruta: String, cuerpo: Map<String, Any?> = emptyMap(), resultado: String): Long {
        val respuesta = parsear(solicitar(metodo, api(ruta), aJson(cuerpo))).jsonObject
        val codigo = respuesta["error_code"]?.jsonPrimitive?.longOrNull
        if (codigo != null && codigo != 0L) {
            val mensaje = respuesta["error_msg"]?.jsonPrimitive?.contentOrNull.orEmpty()
            throw SmcftException(codigo.toInt(), mensaje.substringAfter(": ", mensaje).trim())
        }
        return respuesta[resultado]?.jsonPrimitive?.longOrNull
            ?: throw ErrorConexion("La respuesta del servidor no trae '$resultado'.")
    }

    // ---------------------------------------------------------------- HTTP

    private fun api(ruta: String) = base + "api/" + ruta

    private fun parsear(texto: String): JsonElement =
        try {
            json.parseToJsonElement(texto)
        } catch (e: Exception) {
            throw ErrorConexion("La respuesta del servidor no es JSON. Revise la URL.", e)
        }

    private fun solicitar(metodo: String, url: String, cuerpo: String?, reintentar: Boolean = true): String {
        val conexion = abrir(url, metodo)
        try {
            if (clientId.isNotBlank()) conexion.setRequestProperty("Authorization", "Bearer ${obtenerToken()}")
            conexion.setRequestProperty("Accept", "application/json")
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexion.responseCode
            val texto = leer(conexion, codigo)
            return when {
                codigo in 200..299 -> texto
                codigo == 401 && reintentar && clientId.isNotBlank() -> {
                    token = null
                    solicitar(metodo, url, cuerpo, reintentar = false)
                }
                codigo == 401 || codigo == 403 -> throw ErrorConexion("Acceso denegado ($codigo). Revise el client_id y el client_secret.")
                codigo == 404 -> throw ErrorConexion("No se encontró $url (404). Revise la URL y que el módulo smcft.api esté publicado.")
                else -> throw ErrorConexion("El servidor respondió $codigo: ${mensajeOrds(texto)}")
            }
        } catch (e: IOException) {
            throw ErrorConexion("No se pudo conectar con el servidor: ${e.message}", e)
        } finally {
            conexion.disconnect()
        }
    }

    private fun obtenerToken(): String {
        token?.takeIf { System.currentTimeMillis() < tokenExpira }?.let { return it }

        val conexion = abrir(base + "oauth/token", "POST")
        try {
            val credenciales = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(Charsets.UTF_8))
            conexion.setRequestProperty("Authorization", "Basic $credenciales")
            conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conexion.doOutput = true
            conexion.outputStream.use { it.write("grant_type=${URLEncoder.encode("client_credentials", "UTF-8")}".toByteArray()) }
            val codigo = conexion.responseCode
            val texto = leer(conexion, codigo)
            if (codigo !in 200..299) {
                throw ErrorConexion("No se pudo obtener el token OAuth ($codigo). Revise el client_id y el client_secret.")
            }
            val respuesta = parsear(texto).jsonObject
            val nuevo = respuesta["access_token"]?.jsonPrimitive?.contentOrNull
                ?: throw ErrorConexion("El servidor no entregó un access_token.")
            val segundos = respuesta["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600
            token = nuevo
            tokenExpira = System.currentTimeMillis() + (segundos - 60) * 1000
            return nuevo
        } catch (e: IOException) {
            throw ErrorConexion("No se pudo conectar con el servidor: ${e.message}", e)
        } finally {
            conexion.disconnect()
        }
    }

    private fun abrir(url: String, metodo: String): HttpURLConnection {
        val conexion = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw ErrorConexion("URL inválida: $url", e)
        }
        conexion.requestMethod = metodo
        conexion.connectTimeout = timeoutMs
        conexion.readTimeout = timeoutMs
        conexion.useCaches = false
        return conexion
    }

    private fun leer(conexion: HttpURLConnection, codigo: Int): String {
        val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        return flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun mensajeOrds(texto: String): String =
        runCatching { json.parseToJsonElement(texto).jsonObject["message"]?.jsonPrimitive?.contentOrNull }
            .getOrNull() ?: texto.take(200)

    private fun aJson(valores: Map<String, Any?>): String =
        JsonObject(valores.mapValues { (_, v) ->
            when (v) {
                null -> JsonNull
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        }).toString()
}

// ---------------------------------------------------------------- lectura de columnas

internal fun JsonObject.texto(k: String): String = this[k]?.jsonPrimitive?.contentOrNull.orEmpty()
internal fun JsonObject.textoONulo(k: String): String? = this[k]?.jsonPrimitive?.contentOrNull
internal fun JsonObject.entero(k: String): Long = this[k]?.jsonPrimitive?.longOrNull ?: 0
internal fun JsonObject.decimal(k: String): Double = this[k]?.jsonPrimitive?.doubleOrNull ?: 0.0
internal fun JsonObject.fecha(k: String): LocalDate? = textoONulo(k)?.let { LocalDate.parse(it.take(10)) }
internal fun JsonObject.fechaHora(k: String): LocalDateTime? = textoONulo(k)?.let { LocalDateTime.parse(it.take(19)) }
