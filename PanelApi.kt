package com.davidlg.iptvplayer.data

import android.content.Context
import android.provider.Settings
import android.content.res.Configuration
import com.davidlg.iptvplayer.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Cliente de la API pública del Panel DAVIDLG. No accede directamente a SQLite/MySQL. */
class PanelApi(private val context: Context) {
    data class Session(val token: String, val userName: String, val email: String)
    class PanelApiException(val code: String, val httpStatus: Int, message: String) : IllegalStateException(message)
    data class Catalog(val channels: List<Channel>, val vod: List<Channel>, val epgUrls: List<Pair<String, String>> = emptyList())

    fun deviceId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?.takeIf { it.isNotBlank() } ?: "android-${context.packageName}"

    private fun devicePayload(): JSONObject {
        val uiType = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        val platform = when {
            uiType == Configuration.UI_MODE_TYPE_TELEVISION -> "android_tv"
            context.resources.configuration.smallestScreenWidthDp >= 600 -> "android_tablet"
            else -> "android_mobile"
        }
        return JSONObject()
            .put("device", deviceId())
            .put("platform", platform)
            .put("devname", android.os.Build.MODEL.take(80))
            .put("model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".take(100))
            .put("os_version", "Android ${android.os.Build.VERSION.RELEASE}".take(60))
            .put("app_version", BuildConfig.VERSION_NAME.take(40))
    }

    fun login(baseUrl: String, email: String, password: String): Session {
        val body = devicePayload().put("email", email.trim()).put("password", password)
        return sessionRequest(baseUrl, "login", body)
    }

    fun register(baseUrl: String, name: String, email: String, password: String): Session {
        val body = devicePayload().put("name", name.trim()).put("email", email.trim()).put("password", password)
        return sessionRequest(baseUrl, "register", body)
    }

    fun redeem(baseUrl: String, token: String, code: String) {
        request(baseUrl, "redeem", JSONObject().put("code", code.trim().uppercase()), token)
    }

    private fun sessionRequest(baseUrl: String, action: String, body: JSONObject): Session {
        val json = request(baseUrl, action, body, null)
        val token = json.optString("token")
        if (token.isBlank()) throw IllegalStateException(json.optString("error", "El panel no devolvió un token de sesión"))
        val user = json.optJSONObject("user") ?: JSONObject()
        return Session(token, user.optString("name", "Cliente"), user.optString("email", ""))
    }

    fun validateSession(baseUrl: String, token: String) {
        request(baseUrl, "me", null, token)
        // La autenticación de sesión no depende de la vigencia del paquete ni del bloqueo de catálogo.
        // Así la cuenta puede entrar, canjear renovación y seguir usando fuentes locales;
        // el endpoint content aplica cloud_block/expired al catálogo remoto.
    }

    fun content(baseUrl: String, token: String): Catalog {
        val json = request(baseUrl, "content", null, token)
        val channels = parseItems(json.optJSONArray("channels"), false)
        val vod = parseItems(json.optJSONArray("vod"), true)
        val epg = ArrayList<Pair<String, String>>()
        val sources = json.optJSONArray("epg")
        if (sources != null) for (i in 0 until sources.length()) {
            val source = sources.optJSONObject(i) ?: continue
            val url = source.optString("url").trim()
            if (url.startsWith("http://", true) || url.startsWith("https://", true)) epg += ("panel_epg_${source.optString("id", i.toString())}" to url)
        }
        return Catalog(channels, vod, epg)
    }

    fun logout(baseUrl: String, token: String) { request(baseUrl, "logout", JSONObject(), token) }

    private fun parseItems(array: org.json.JSONArray?, isVod: Boolean): List<Channel> {
        if (array == null) return emptyList()
        val result = ArrayList<Channel>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optLong("id", index.toLong()).toString()
            val name = item.optString("name", "Contenido ${index + 1}")
            val group = item.optString("group", "General").ifBlank { "General" }
            val drm = item.optJSONObject("drm") ?: JSONObject()
            val type = drm.optString("type", "none").lowercase()
            val key = drm.optString("key", "")
            val license = drm.optString("license", "")
            val props = linkedMapOf<String, String>()
            listOf("media_type", "description", "poster", "backdrop", "year", "genre", "duration", "rating", "season", "episode", "series_id", "external_id").forEach { key ->
                item.optString(key).takeIf { it.isNotBlank() }?.let { props[key] = it }
            }
            when {
                type.contains("widevine") -> {
                    props["inputstream.adaptive.manifest_type"] = "mpd"
                    props["inputstream.adaptive.license_type"] = "com.widevine.alpha"
                    if (license.isNotBlank()) props["inputstream.adaptive.license_key"] = license
                }
                type.contains("clear") -> {
                    props["inputstream.adaptive.manifest_type"] = "mpd"
                    props["inputstream.adaptive.license_type"] = "clearkey"
                    if (key.isNotBlank()) props["inputstream.adaptive.license_key"] = key
                    if (key.contains(':')) {
                        val parts = key.split(':', limit = 2)
                        props["kid"] = parts[0]
                        props["key"] = parts[1]
                    }
                }
            }
            item.optString("ua").takeIf { it.isNotBlank() }?.let { props["user_agent"] = it }
            item.optString("ref").takeIf { it.isNotBlank() }?.let { props["referer"] = it }
            result += Channel(
                id = "panel_$id",
                number = index + 1,
                name = name,
                tvgId = item.optString("tvg_id", ""),
                tvgName = name,
                group = group,
                logo = item.optString("logo", ""),
                url = item.optString("url", ""),
                props = props,
                isVod = isVod,
                sourceId = "panel",
                sourceName = "Panel DAVIDLG"
            )
        }
        return result.filter { it.url.isNotBlank() }
    }

    private fun request(baseUrl: String, action: String, body: JSONObject?, token: String?): JSONObject {
        var root = baseUrl.trim().trimEnd('/')
        if (!root.substringAfterLast('/').contains(".php", ignoreCase = true)) root += "/app.php"
        require(root.startsWith("https://", true) || root.startsWith("http://", true)) {
            "La dirección del panel debe comenzar con https:// o http://"
        }
        // En algunos hostings PHP/CGI no expone HTTP_AUTHORIZATION a PHP.
        // Enviamos el token también como ?t= para ser compatibles con app.php,
        // que admite ambos mecanismos. Se conserva Bearer como respaldo.
        val separator = if (root.contains("?")) "&" else "?"
        val query = buildString {
            append("r=").append(URLEncoder.encode(action, "UTF-8"))
            if (!token.isNullOrBlank()) {
                append("&t=").append(URLEncoder.encode(token, "UTF-8"))
            }
        }
        val connection = (URL("$root$separator$query").openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (status !in 200..299) throw PanelApiException(json.optString("code"), status, json.optString("error").ifBlank { "Error HTTP $status al conectar con el panel" })
            return json
        } finally {
            connection.disconnect()
        }
    }
}
