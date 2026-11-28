package com.davidlg.iptvplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Envoltorio comparable de un JSON de ajustes (para usarlo en StateFlow). */
data class JsonKv(val raw: String = "{}") {
    private val o: JSONObject = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    fun str(k: String, def: String = ""): String = if (o.has(k) && !o.isNull(k)) o.optString(k, def) else def
    fun bool(k: String, def: Boolean = false): Boolean = if (o.has(k)) o.optBoolean(k, def) else def
    fun int(k: String, def: Int = 0): Int = if (o.has(k)) o.optInt(k, def) else def
    fun has(k: String): Boolean = o.has(k) && !o.isNull(k)
    fun json(): JSONObject = JSONObject(raw)
    fun with(k: String, v: Any?): JsonKv { val n = JSONObject(raw); if (v == null) n.remove(k) else n.put(k, v); return JsonKv(n.toString()) }
}

/**
 * Almacenamiento con las mismas claves que la versión web. Igual que en profiles.js: el primer perfil
 * ("p1") conserva las claves de siempre y el resto usa el prefijo davidlgP:<id>: para las claves por perfil.
 */
class AppPrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("davidlg", Context.MODE_PRIVATE)

    companion object {
        /** Claves propias de cada perfil (PK en profiles.js). */
        val PER_PROFILE = setOf(
            "recentChannels", "davidlgVodFavorites", "davidlgVodProgress", "davidlgVodMetadata",
            "vod_playback_history", "playbackPrefs", "davidlgVodSettings",
            // Las fuentes y su selección pertenecen al perfil activo, no a toda la cuenta.
            "iptvPlaylists", "activeIptvPlaylists", "iptvHomeSourceFilter",
        )
        const val REG = "davidlgProfiles"
        const val MAX_PROFILES = 5
    }

    // ---------- Perfiles ----------
    var activeProfileId: String = "p1"
        private set

    init {
        activeProfileId = loadRegistry().second
        migrateLegacySharedPlaylists()
    }

    /**
     * Migración compatible: en versiones anteriores las listas eran globales.
     * Los perfiles que ya existían reciben una copia inicial para no perder acceso;
     * desde esta versión cada copia evoluciona de forma independiente.
     */
    private fun migrateLegacySharedPlaylists() {
        val legacyKeys = listOf("iptvPlaylists", "activeIptvPlaylists", "iptvHomeSourceFilter")
        val existingProfiles = loadRegistry().first
        val editor = sp.edit()
        var changed = false
        for (profile in existingProfiles) {
            if (profile.id == "p1") continue
            val prefix = "davidlgP:${profile.id}:"
            for (legacyKey in legacyKeys) {
                val scopedKey = prefix + legacyKey
                if (!sp.contains(scopedKey) && sp.contains(legacyKey)) {
                    when (val value = sp.all[legacyKey]) {
                        is String -> editor.putString(scopedKey, value)
                        is Int -> editor.putInt(scopedKey, value)
                        is Boolean -> editor.putBoolean(scopedKey, value)
                        is Long -> editor.putLong(scopedKey, value)
                        is Float -> editor.putFloat(scopedKey, value)
                    }
                    changed = true
                }
            }
        }
        if (changed) editor.apply()
    }

    private fun loadRegistry(): Pair<List<Profile>, String> {
        try {
            val o = JSONObject(sp.getString(REG, "") ?: "")
            val a = o.getJSONArray("list")
            val list = (0 until a.length()).map {
                val p = a.getJSONObject(it); Profile(p.getString("id"), p.optString("name", "Perfil"), p.optInt("av", 0), p.optBoolean("hasPin", false))
            }
            if (list.isNotEmpty()) {
                val act = o.optString("active", list[0].id)
                return list to (if (list.any { it.id == act }) act else list[0].id)
            }
        } catch (_: Exception) {}
        return listOf(Profile("p1", "Mi perfil", 0)) to "p1"
    }

    fun profiles(): List<Profile> = loadRegistry().first

    fun saveProfiles(list: List<Profile>, active: String) {
        val a = JSONArray()
        for (p in list) a.put(JSONObject().put("id", p.id).put("name", p.name).put("av", p.av).put("hasPin", p.hasPin))
        sp.edit().putString(REG, JSONObject().put("v", 1).put("list", a).put("active", active).toString()).apply()
        activeProfileId = active
    }

    /** Borra los datos propios de un perfil (davidlgP:<id>:*). */
    fun purgeProfile(id: String) {
        val pre = "davidlgP:$id:"
        val e = sp.edit()
        if (id == "p1") {
            // p1 conserva las claves históricas sin prefijo; al eliminarlo
            // se limpian únicamente sus preferencias por perfil.
            PER_PROFILE.forEach { e.remove(it) }
        } else {
            for (k in sp.all.keys) if (k.startsWith(pre)) e.remove(k)
        }
        e.apply()
    }

    fun setActiveProfile(id: String) { saveProfiles(profiles(), id) }

    // ---------- Acceso a claves (con separación por perfil) ----------
    private fun key(k: String) =
        if (k in PER_PROFILE && activeProfileId != "p1") "davidlgP:$activeProfileId:$k" else k

    fun getString(k: String, def: String = ""): String = sp.getString(key(k), def) ?: def
    fun putString(k: String, v: String) { sp.edit().putString(key(k), v).apply() }
    fun remove(k: String) { sp.edit().remove(key(k)).apply() }
    fun getKv(k: String): JsonKv = JsonKv(getString(k, "{}").ifBlank { "{}" })
    fun putKv(k: String, kv: JsonKv) = putString(k, kv.raw)

    // ---------- Listas independientes por perfil ----------
    fun playlists(): List<Playlist> = try {
        val a = JSONArray(getString("iptvPlaylists", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Playlist(o.getString("id"), o.optString("name", "Lista"), o.getString("m3uUrl"), o.optString("epgUrl", ""), o.optString("contentType", "mixed"))
        }
    } catch (_: Exception) { emptyList() }

    fun savePlaylists(list: List<Playlist>) {
        val a = JSONArray()
        for (p in list) a.put(JSONObject().put("id", p.id).put("name", p.name).put("m3uUrl", p.m3uUrl).put("epgUrl", p.epgUrl).put("contentType", p.contentType))
        putString("iptvPlaylists", a.toString())
    }

    fun activeIds(): List<String> = try {
        val a = JSONArray(getString("activeIptvPlaylists", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveActiveIds(ids: List<String>) { putString("activeIptvPlaylists", JSONArray(ids).toString()) }

    fun sourceFilter(): String = getString("iptvHomeSourceFilter", "all")
    fun saveSourceFilter(v: String) { putString("iptvHomeSourceFilter", v) }

    // ---------- Recientes (por perfil) ----------
    fun recents(): List<String> = try {
        val a = JSONArray(getString("recentChannels", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveRecents(l: List<String>) = putString("recentChannels", JSONArray(l).toString())

    // ---------- Cuenta del panel DAVIDLG ----------
    fun panelBaseUrl(): String = sp.getString("panelBaseUrl", "https://dtvstreaming.webredirect.org/panel/app.php") ?: "https://dtvstreaming.webredirect.org/panel/app.php"
    fun panelToken(): String = sp.getString("panelSessionToken", "") ?: ""
    fun panelEmail(): String = sp.getString("panelEmail", "") ?: ""
    fun savePanelSession(baseUrl: String, token: String, email: String) {
        sp.edit().putString("panelBaseUrl", baseUrl.trim()).putString("panelSessionToken", token).putString("panelEmail", email).apply()
    }
    fun exportProfileState(profileId: String): JSONObject {
        val out = JSONObject()
        PER_PROFILE.forEach { logical ->
            val stored = if (profileId == "p1") logical else "davidlgP:$profileId:$logical"
            val value = sp.all[stored]
            if (value != null) out.put(logical, value)
        }
        return out
    }

    fun importProfileState(profileId: String, state: JSONObject) {
        val edit = sp.edit()
        PER_PROFILE.forEach { logical ->
            if (!state.has(logical) || state.isNull(logical)) return@forEach
            val stored = if (profileId == "p1") logical else "davidlgP:$profileId:$logical"
            when (val value = state.opt(logical)) {
                is String -> edit.putString(stored, value)
                is Boolean -> edit.putBoolean(stored, value)
                is Int -> edit.putInt(stored, value)
                is Long -> edit.putLong(stored, value)
                is Double -> edit.putFloat(stored, value.toFloat())
            }
        }
        edit.apply()
    }

    fun clearPanelSession() { sp.edit().remove("panelSessionToken").remove("panelEmail").apply() }

    fun clearAllSources() {
        remove("iptvPlaylists"); remove("activeIptvPlaylists"); remove("iptvHomeSourceFilter")
    }
}
