package com.davidlg.iptvplayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidlg.iptvplayer.data.AppPrefs
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.data.IptvRepository
import com.davidlg.iptvplayer.data.JsonKv
import com.davidlg.iptvplayer.data.Playlist
import com.davidlg.iptvplayer.data.PanelApi
import com.davidlg.iptvplayer.data.Profile
import com.davidlg.iptvplayer.data.TmdbRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Pantallas de nivel superior. Main = OttShell o ClassicShell según el estilo del perfil. */
enum class Screen { Login, AuthChecking, Profiles, Main, Settings, Player }

/** Pestañas de la barra superior del estilo OTT (igual que .ottTab de la versión web). */
enum class OttTab { Start, Movie, Series, Anime, Live, Epg, Fav }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = AppPrefs(app)
    private val repo = IptvRepository(app)
    private val panelApi = PanelApi(app)

    // ---------------- Navegación ----------------
    var screen by mutableStateOf(if (prefs.panelToken().isBlank()) Screen.Login else Screen.AuthChecking)
    /** "launch" = al abrir la app (Atrás sale); "switch" = cambio de perfil desde dentro (Atrás vuelve). */
    var profileMode by mutableStateOf("launch")
    var pinPromptProfile by mutableStateOf<Profile?>(null)
    var pinBusy by mutableStateOf(false)
    var pinError by mutableStateOf("")
    var pairingCode by mutableStateOf("")
    var pairingStatus by mutableStateOf("")
    var pairingBusy by mutableStateOf(false)
    var ottTab by mutableStateOf(OttTab.Start)
    var pendingVodDetailsId by mutableStateOf<String?>(null)
        private set
    var returnTo: Screen = Screen.Main
    var current by mutableStateOf<Channel?>(null)

    /** Abre el administrador de perfiles desde Ajustes sin perder la pantalla de retorno. */
    fun showProfileManager() {
        profileMode = "manage"
        returnTo = screen
        screen = Screen.Profiles
    }

    fun openVodDetails(channel: Channel) {
        pendingVodDetailsId = channel.id
        ottTab = when {
            channel.group.contains("anime", true) -> OttTab.Anime
            channel.group.contains("serie", true) || channel.url.contains("/series/", true) ||
                Regex("\\bs\\d{1,2}\\s?e\\d{1,3}\\b|temporada|season|episodio", RegexOption.IGNORE_CASE).containsMatchIn(channel.name) -> OttTab.Series
            else -> OttTab.Movie
        }
    }
    fun consumePendingVodDetails() { pendingVodDetailsId = null }

    private suspend fun syncProfilesWithPanel() {
        val base = prefs.panelBaseUrl(); val token = prefs.panelToken()
        if (token.isBlank()) return
        try {
            val remote = withContext(Dispatchers.IO) { panelApi.remoteProfiles(base, token) }
            val local = profiles.value
            if (remote.isEmpty()) {
                local.forEach { p -> withContext(Dispatchers.IO) { panelApi.saveRemoteProfile(base, token, p.id, p.name, p.av) } }
            } else {
                val remoteById = remote.associateBy { it.id }
                val merged = local.map { p -> remoteById[p.id]?.let { p.copy(name = it.name, av = it.avatar, hasPin = it.hasPin) } ?: p }.toMutableList()
                remote.filter { r -> merged.none { it.id == r.id } }.forEach { merged.add(Profile(r.id, r.name, r.avatar, r.hasPin)) }
                merged.take(AppPrefs.MAX_PROFILES).forEach { p ->
                    if (remoteById[p.id] == null) withContext(Dispatchers.IO) { panelApi.saveRemoteProfile(base, token, p.id, p.name, p.av) }
                }
                profiles.value = merged.take(AppPrefs.MAX_PROFILES)
                val activeId = prefs.activeProfileId.takeIf { id -> profiles.value.any { it.id == id } } ?: profiles.value.first().id
                prefs.saveProfiles(profiles.value, activeId)
                activeProfile.value = profiles.value.first { it.id == activeId }
            }
            profiles.value.forEach { p ->
                val (remoteState, revision) = withContext(Dispatchers.IO) { panelApi.readProfileState(base, token, p.id) }
                if (revision > 0) prefs.importProfileState(p.id, remoteState)
                else withContext(Dispatchers.IO) { panelApi.writeProfileState(base, token, p.id, prefs.exportProfileState(p.id), 0) }
            }
            refreshProfileState()
        } catch (e: Exception) {
            panelError.value = "No se pudieron sincronizar los perfiles: ${e.message ?: "error de conexión"}"
        }
    }

    fun syncCurrentProfileToPanel() = syncProfileToPanel(prefs.activeProfileId)

    private fun syncProfileToPanel(profileId: String) {
        if (!panelConnected.value) return
        viewModelScope.launch {
            val base = prefs.panelBaseUrl(); val token = prefs.panelToken()
            val p = profiles.value.firstOrNull { it.id == profileId } ?: return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    panelApi.saveRemoteProfile(base, token, p.id, p.name, p.av)
                    val (_, revision) = panelApi.readProfileState(base, token, p.id)
                    panelApi.writeProfileState(base, token, p.id, prefs.exportProfileState(p.id), revision)
                }
            }.onFailure { panelError.value = "No se pudo guardar el perfil en la nube: ${it.message}" }
        }
    }

    // ---------------- Perfiles ----------------
    val profiles = MutableStateFlow(prefs.profiles())
    val activeProfile = MutableStateFlow(profiles.value.first { it.id == prefs.activeProfileId })

    fun showProfileSwitcher() { profileMode = "switch"; screen = Screen.Profiles }

    fun requestProfileSelection(id: String) {
        val profile = profiles.value.firstOrNull { it.id == id } ?: return
        if (profile.hasPin && id != prefs.activeProfileId) { pinPromptProfile = profile; pinError = "" }
        else selectProfile(id)
    }

    fun verifyAndSelectProfile(pin: String) {
        val profile = pinPromptProfile ?: return
        viewModelScope.launch {
            pinBusy = true; pinError = ""
            try {
                val ok = withContext(Dispatchers.IO) { panelApi.verifyProfilePin(prefs.panelBaseUrl(), prefs.panelToken(), profile.id, pin) }
                if (ok) { pinPromptProfile = null; selectProfile(profile.id) } else pinError = "PIN incorrecto"
            } catch (e: Exception) { pinError = e.message ?: "No se pudo verificar el PIN" }
            finally { pinBusy = false }
        }
    }

    fun setProfilePin(id: String, pin: String) {
        if (pin.isBlank() || !panelConnected.value) return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val profile = profiles.value.firstOrNull { it.id == id } ?: return@withContext
                panelApi.saveRemoteProfile(prefs.panelBaseUrl(), prefs.panelToken(), id, profile.name, profile.av)
                panelApi.setProfilePin(prefs.panelBaseUrl(), prefs.panelToken(), id, pin)
            } }
                .onSuccess {
                    profiles.value = profiles.value.map { if (it.id == id) it.copy(hasPin = true) else it }
                    prefs.saveProfiles(profiles.value, prefs.activeProfileId)
                    if (activeProfile.value.id == id) activeProfile.value = profiles.value.first { it.id == id }
                }.onFailure { panelError.value = "No se pudo guardar el PIN: ${it.message}" }
        }
    }

    fun removeProfilePin(id: String, pin: String) {
        if (pin.isBlank() || !panelConnected.value) return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { panelApi.removeProfilePin(prefs.panelBaseUrl(), prefs.panelToken(), id, pin) } }
                .onSuccess {
                    profiles.value = profiles.value.map { if (it.id == id) it.copy(hasPin = false) else it }
                    prefs.saveProfiles(profiles.value, prefs.activeProfileId)
                    if (activeProfile.value.id == id) activeProfile.value = profiles.value.first { it.id == id }
                }.onFailure { panelError.value = "No se pudo quitar el PIN: ${it.message}" }
        }
    }

    fun cancelPinPrompt() { pinPromptProfile = null; pinError = "" }

    fun selectProfile(id: String) {
        prefs.setActiveProfile(id)
        syncProfileToPanel(id)
        activeProfile.value = profiles.value.first { it.id == id }
        refreshProfileState()
        // Evitar que la guía del perfil anterior se muestre mientras carga la nueva.
        EpgStore.clear()
        // Reconstruir canales/EPG usando exclusivamente las fuentes del perfil seleccionado.
        reload(false)
        ottTab = OttTab.Start
        screen = if (profileMode == "manage") returnTo else Screen.Main
        profileMode = "switch"
    }

    fun addProfile(name: String, av: Int): Boolean {
        if (profiles.value.size >= AppPrefs.MAX_PROFILES) return false
        val id = "p" + UUID.randomUUID().toString().replace("-", "").take(12)
        profiles.value = profiles.value + Profile(id, name.ifBlank { "Perfil" }, av)
        prefs.saveProfiles(profiles.value, prefs.activeProfileId)
        syncProfileToPanel(id)
        return true
    }

    fun editProfile(id: String, name: String, av: Int) {
        profiles.value = profiles.value.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }, av = av) else it }
        prefs.saveProfiles(profiles.value, prefs.activeProfileId)
        if (activeProfile.value.id == id) activeProfile.value = profiles.value.first { it.id == id }
        syncProfileToPanel(id)
    }

    /** No se puede eliminar el último perfil. */
    fun deleteProfile(id: String): Boolean {
        if (profiles.value.size <= 1) return false
        prefs.purgeProfile(id)
        if (panelConnected.value) viewModelScope.launch { withContext(Dispatchers.IO) { runCatching { panelApi.deleteRemoteProfile(prefs.panelBaseUrl(), prefs.panelToken(), id) } } }
        val left = profiles.value.filter { it.id != id }
        profiles.value = left
        val act = if (prefs.activeProfileId == id) left[0].id else prefs.activeProfileId
        prefs.saveProfiles(left, act)
        activeProfile.value = left.first { it.id == act }
        refreshProfileState()
        return true
    }

    // ---------------- Ajustes por perfil (mismas claves que la web) ----------------
    /** davidlgVodSettings: style ("ott"|"classic"), theme, fastZap ("on"|"off"), titles, tmdbKey, sort... */
    val vodSettings = MutableStateFlow(prefs.getKv("davidlgVodSettings"))
    /** playbackPrefs: clock, audio, subs, quality, fps, channelQuality. */
    val playbackPrefs = MutableStateFlow(prefs.getKv("playbackPrefs"))
    val recents = MutableStateFlow(prefs.recents())

    val isClassic: Boolean get() = vodSettings.value.str("style", "ott") == "classic"

    fun putVodSetting(k: String, v: Any?) {
        val n = vodSettings.value.with(k, v)
        prefs.putKv("davidlgVodSettings", n); vodSettings.value = n
        syncProfileToPanel(prefs.activeProfileId)
    }

    fun putPlaybackPref(k: String, v: Any?) {
        val n = playbackPrefs.value.with(k, v)
        prefs.putKv("playbackPrefs", n); playbackPrefs.value = n
        syncProfileToPanel(prefs.activeProfileId)
    }

    private fun refreshProfileState() {
        vodSettings.value = prefs.getKv("davidlgVodSettings")
        playbackPrefs.value = prefs.getKv("playbackPrefs")
        recents.value = prefs.recents()
        // Al cambiar de perfil, recargar sus listas IPTV, fuentes activas y filtro.
        playlists.value = prefs.playlists()
        activeIds.value = prefs.activeIds().ifEmpty { prefs.playlists().take(1).map { it.id } }
        sourceFilter.value = prefs.sourceFilter().takeIf { it == "all" || playlists.value.any { p -> p.id == it } } ?: "all"
        applyFilter()
        vodMetadataVersion++
    }

    // ---------------- Listas y canales ----------------
    val playlists = MutableStateFlow(prefs.playlists())
    val activeIds = MutableStateFlow(prefs.activeIds().ifEmpty { prefs.playlists().take(1).map { it.id } })
    /** "all" o el id de una lista (filtro de fuente del inicio). */
    val sourceFilter = MutableStateFlow(prefs.sourceFilter())

    /** Todos los canales en vivo / VOD de las listas activas (sin filtro de fuente). */
    val allLive = MutableStateFlow<List<Channel>>(emptyList())
    val allVod = MutableStateFlow<List<Channel>>(emptyList())
    /** Con el filtro de fuente aplicado. Los números de canal se recalculan sobre esta lista. */
    val live = MutableStateFlow<List<Channel>>(emptyList())
    val vod = MutableStateFlow<List<Channel>>(emptyList())

    val status = MutableStateFlow("")
    val errors = MutableStateFlow<List<String>>(emptyList())
    val epgVersion: StateFlow<Int> = EpgStore.version

    private var byId: Map<String, Channel> = emptyMap()
    private val tvgByPlaylist = HashMap<String, String>()

    val panelBusy = MutableStateFlow(false)
    val panelError = MutableStateFlow("")
    val panelAccount = MutableStateFlow(prefs.panelEmail())
    val panelConnected = MutableStateFlow(prefs.panelToken().isNotBlank())

    private var panelMonitorJob: Job? = null

    init {
        if (panelConnected.value) {
            viewModelScope.launch {
                val initialToken = prefs.panelToken()
                try {
                    withContext(Dispatchers.IO) { panelApi.validateSession(prefs.panelBaseUrl(), initialToken) }
                } catch (e: Exception) {
                    val failure = e as? PanelApi.PanelApiException
                    allLive.value = allLive.value.filterNot { it.sourceId == "panel" }
                    allVod.value = allVod.value.filterNot { it.sourceId == "panel" }
                    stopPanelPlaybackIfNeeded()
                    if (failure?.code in setOf("full_block", "device_blocked", "auth", "account_suspended")) {
                        prefs.clearPanelSession(); panelConnected.value = false; panelAccount.value = ""
                        screen = Screen.Login
                        panelError.value = failure?.message ?: "La cuenta o el dispositivo ya no están autorizados."
                    } else {
                        screen = Screen.Profiles
                        if (failure?.code == "cloud_block" || failure?.code == "expired") panelError.value = failure?.message ?: "Catálogo del panel no disponible."
                    }
                }
                if (screen == Screen.AuthChecking) screen = Screen.Profiles
                syncProfilesWithPanel()
                reload(false)
                startPanelMonitor()
            }
        }
    }

    private fun startPanelMonitor() {
        if (panelMonitorJob?.isActive == true) return
        panelMonitorJob = viewModelScope.launch {
            var panelWasUnavailable = false
            var monitorCycles = 0
            while (panelConnected.value) {
                delay(120_000)
                monitorCycles++
                val token = prefs.panelToken()
                if (token.isBlank()) continue
                try {
                    withContext(Dispatchers.IO) { panelApi.validateSession(prefs.panelBaseUrl(), token) }
                    if (panelWasUnavailable) {
                        panelWasUnavailable = false
                        panelError.value = ""
                        reload(false)
                    } else if (monitorCycles % 3 == 0) {
                        // Refresca el catálogo del panel cada ~6 minutos sin descargar listas locales innecesariamente.
                        reload(false)
                    }
                } catch (e: Exception) {
                    panelWasUnavailable = true
                    allLive.value = allLive.value.filterNot { it.sourceId == "panel" }
                    allVod.value = allVod.value.filterNot { it.sourceId == "panel" }
                    stopPanelPlaybackIfNeeded()
                    applyFilter()
                    val failure = e as? PanelApi.PanelApiException
                    if (failure?.code in setOf("full_block", "device_blocked", "auth", "account_suspended")) {
                        prefs.clearPanelSession(); panelConnected.value = false; panelAccount.value = ""
                        current = null
                        screen = Screen.Login
                        panelError.value = failure?.message ?: "La cuenta o este dispositivo ya no están autorizados."
                    } else if (failure?.code == "cloud_block" || failure?.code == "expired") {
                        panelError.value = failure?.message ?: "El catálogo del panel no está disponible para esta cuenta."
                    }
                }
            }
        }
    }

    fun connectPanel(baseUrl: String, email: String, password: String, activationCode: String = "", name: String = "", register: Boolean = false) {
        viewModelScope.launch {
            panelBusy.value = true
            panelError.value = ""
            try {
                val session = withContext(Dispatchers.IO) {
                    val created = if (register) panelApi.register(baseUrl, name, email, password)
                    else panelApi.login(baseUrl, email, password)
                    // Confirmar que el token recién emitido realmente autentica
                    // antes de guardarlo y cambiar a la pantalla de perfiles.
                    panelApi.validateSession(baseUrl, created.token)
                    created
                }
                prefs.savePanelSession(baseUrl, session.token, session.email.ifBlank { email })
                panelAccount.value = session.email.ifBlank { session.userName }
                panelConnected.value = true
                screen = Screen.Profiles
                if (activationCode.isNotBlank()) {
                    try { withContext(Dispatchers.IO) { panelApi.redeem(baseUrl, session.token, activationCode) } }
                    catch (codeError: Exception) { panelError.value = codeError.message ?: "No se pudo canjear el código" }
                }
                syncProfilesWithPanel()
                reload(true)
                startPanelMonitor()
            } catch (e: Exception) {
                panelError.value = e.message ?: "No se pudo conectar con el panel"
            } finally { panelBusy.value = false }
        }
    }

    fun startDevicePairing(baseUrl: String = prefs.panelBaseUrl()) {
        if (pairingBusy) return
        viewModelScope.launch {
            pairingBusy = true; pairingStatus = "Generando código seguro…"; pairingCode = ""
            try {
                val challenge = withContext(Dispatchers.IO) { panelApi.createQrChallenge(baseUrl) }
                pairingCode = challenge.code
                pairingStatus = "Introduce este código en un dispositivo donde ya hayas iniciado sesión."
                while (System.currentTimeMillis() < challenge.expires && pairingBusy) {
                    delay(2000)
                    val result = withContext(Dispatchers.IO) { panelApi.pollQrChallenge(baseUrl, challenge.code) }
                    when (result.status) {
                        "authorized" -> {
                            if (result.token.isBlank()) throw IllegalStateException("El panel no entregó el token del dispositivo vinculado")
                            prefs.savePanelSession(baseUrl, result.token, result.email)
                            panelAccount.value = result.email.ifBlank { result.userName }
                            panelConnected.value = true
                            pairingStatus = "Dispositivo vinculado correctamente"
                            pairingBusy = false; screen = Screen.Profiles
                            syncProfilesWithPanel(); reload(true); startPanelMonitor(); return@launch
                        }
                        "denied", "expired" -> throw IllegalStateException("El código venció o fue rechazado")
                    }
                }
                if (pairingBusy) pairingStatus = "El código venció. Genera uno nuevo."
            } catch (e: Exception) { pairingStatus = e.message ?: "No se pudo vincular el dispositivo" }
            finally { pairingBusy = false }
        }
    }

    fun cancelDevicePairing() { pairingBusy = false; pairingCode = ""; pairingStatus = "Vinculación cancelada" }

    fun authorizeDeviceCode(code: String) {
        viewModelScope.launch {
            panelBusy.value = true; panelError.value = ""
            try {
                withContext(Dispatchers.IO) { panelApi.authorizeQr(prefs.panelBaseUrl(), prefs.panelToken(), code.trim().uppercase()) }
                panelError.value = "Televisor autorizado. La pantalla se conectará automáticamente."
            } catch (e: Exception) { panelError.value = e.message ?: "No se pudo autorizar el código" }
            finally { panelBusy.value = false }
        }
    }

    fun disconnectPanel() {
        val token = prefs.panelToken()
        val base = prefs.panelBaseUrl()
        prefs.clearPanelSession()
        panelConnected.value = false
        panelAccount.value = ""
        viewModelScope.launch { if (token.isNotBlank()) withContext(Dispatchers.IO) { runCatching { panelApi.logout(base, token) } } }
        reload(false)
        screen = Screen.Login
    }

    fun reload(force: Boolean) {
        viewModelScope.launch {
            val all = playlists.value
            val act = activeIds.value.filter { id -> all.any { it.id == id } }.ifEmpty { all.take(1).map { it.id } }
            val pls = all.filter { it.id in act }
            status.value = "Sincronizando catálogos…"
            val errs = ArrayList<String>()
            val liveAcc = ArrayList<Channel>()
            val vodAcc = ArrayList<Channel>()
            var panelEpgUrls: List<Pair<String, String>> = emptyList()
            for (pl in pls) {
                try {
                    val r = withContext(Dispatchers.IO) { repo.loadPlaylist(pl, force) }
                    tvgByPlaylist[pl.id] = r.urlTvg
                    for (c0 in r.channels) {
                        val c = c0.copy(sourceName = pl.name)
                        if (c.isVod) vodAcc.add(c) else liveAcc.add(c)
                    }
                } catch (e: Exception) {
                    errs.add("${pl.name}: ${e.message ?: e.javaClass.simpleName}")
                }
            }
            val panelToken = prefs.panelToken()
            if (panelToken.isNotBlank()) {
                try {
                    val remote = withContext(Dispatchers.IO) { panelApi.content(prefs.panelBaseUrl(), panelToken) }
                    liveAcc.addAll(remote.channels)
                    vodAcc.addAll(remote.vod)
                    panelEpgUrls = remote.epgUrls
                    panelError.value = ""
                } catch (e: Exception) {
                    stopPanelPlaybackIfNeeded()
                    val panelFailure = e as? PanelApi.PanelApiException
                    when (panelFailure?.code) {
                        "full_block", "device_blocked", "auth", "account_suspended" -> {
                            current = null
                            prefs.clearPanelSession()
                            panelConnected.value = false
                            panelAccount.value = ""
                            screen = Screen.Login
                            panelError.value = panelFailure.message ?: "La cuenta o este dispositivo ya no están autorizados."
                        }
                        "cloud_block", "expired" -> {
                            // La cuenta conserva el reproductor local, pero el catálogo del panel queda retirado.
                            stopPanelPlaybackIfNeeded()
                            panelError.value = panelFailure.message ?: "El catálogo del panel no está disponible para esta cuenta."
                        }
                    }
                    errs.add("Panel DAVIDLG: ${e.message ?: e.javaClass.simpleName}")
                }
            }
            allLive.value = liveAcc
            allVod.value = vodAcc
            errors.value = errs
            applyFilter()
            status.value = ""
            loadEpg(force, pls, panelEpgUrls)
        }
    }

    private fun applyFilter() {
        val f = sourceFilter.value
        val l = if (f == "all") allLive.value else allLive.value.filter { it.sourceId == f }
        val v = if (f == "all") allVod.value else allVod.value.filter { it.sourceId == f }
        live.value = l.mapIndexed { i, c -> c.copy(number = i + 1) }
        vod.value = v.mapIndexed { i, c -> c.copy(number = i + 1) }
        byId = (live.value + vod.value).associateBy { it.id }
    }

    fun setSourceFilter(id: String) {
        sourceFilter.value = id; prefs.saveSourceFilter(id); applyFilter(); syncProfileToPanel(prefs.activeProfileId)
    }

    fun cycleSource() {
        val opts = listOf("all") + activeIds.value + if (panelConnected.value) listOf("panel") else emptyList()
        val i = opts.indexOf(sourceFilter.value).coerceAtLeast(0)
        setSourceFilter(opts[(i + 1) % opts.size])
    }

    private fun loadEpg(force: Boolean, pls: List<Playlist>, panelUrls: List<Pair<String, String>> = emptyList()) {
        viewModelScope.launch {
            val urls = pls.mapNotNull { pl ->
                val u = pl.epgUrl.ifBlank { tvgByPlaylist[pl.id] ?: "" }
                if (u.isBlank()) null else pl.id to u
            } + panelUrls
            if (urls.isEmpty()) return@launch
            withContext(Dispatchers.IO) { repo.loadEpg(urls, force) }
        }
    }

    fun addPlaylist(p: Playlist) {
        playlists.value = playlists.value + p
        prefs.savePlaylists(playlists.value)
        if (activeIds.value.size < AppPrefs.MAX_PROFILES) { activeIds.value = activeIds.value + p.id; prefs.saveActiveIds(activeIds.value) }
        syncProfileToPanel(prefs.activeProfileId)
        reload(true)
    }

    fun updatePlaylist(p: Playlist) {
        playlists.value = playlists.value.map { if (it.id == p.id) p else it }
        prefs.savePlaylists(playlists.value)
        syncProfileToPanel(prefs.activeProfileId)
        reload(true)
    }

    fun removePlaylist(id: String) {
        playlists.value = playlists.value.filter { it.id != id }
        activeIds.value = activeIds.value.filter { it != id }
        prefs.savePlaylists(playlists.value); prefs.saveActiveIds(activeIds.value)
        if (sourceFilter.value == id) setSourceFilter("all")
        syncProfileToPanel(prefs.activeProfileId)
        reload(false)
    }

    /** Activa/desactiva una lista (máximo 5 activas, como en la web). */
    fun toggleActive(id: String) {
        val cur = activeIds.value
        activeIds.value = if (id in cur) cur.filter { it != id } else (cur + id).take(5)
        prefs.saveActiveIds(activeIds.value)
        syncProfileToPanel(prefs.activeProfileId)
        reload(false)
    }

    fun clearAllSources() {
        prefs.clearAllSources(); repo.clearCache()
        playlists.value = emptyList(); activeIds.value = emptyList(); sourceFilter.value = "all"
        syncProfileToPanel(prefs.activeProfileId)
        reload(false)
    }

    fun clearCacheAndReload() { repo.clearCache(); reload(true) }

    // ---------------- Recientes ----------------
    fun markWatched(c: Channel) {
        if (c.isVod) return
        val l = recents.value.toMutableList()
        l.remove(c.id); l.add(0, c.id)
        recents.value = l.take(30); prefs.saveRecents(recents.value)
        syncProfileToPanel(prefs.activeProfileId)
    }

    fun recentChannels(): List<Channel> = recents.value.mapNotNull { byId[it] }.filter { !it.isVod }
    fun channelById(id: String): Channel? = byId[id]

    // ---------------- Reproducción ----------------
    /** Canal seleccionado en la vista OTT; persiste al cambiar de motor o recomponer pantallas. */
    var selectedLiveChannelId by mutableStateOf<String?>(null)
        private set
    fun selectLiveChannel(channel: Channel?) { selectedLiveChannelId = channel?.id }
    var queue: List<Channel> = emptyList()
    /** Posición inicial (ms) para reanudar VOD; la consume el reproductor. */
    var startPositionMs: Long = 0L

    fun play(c: Channel, from: List<Channel>, startMs: Long = 0L) {
        if (screen != Screen.Player) returnTo = screen
        current = c; queue = from; startPositionMs = startMs
        if (!c.isVod) selectedLiveChannelId = c.id
        screen = Screen.Player
    }

    fun zap(delta: Int) {
        val q = queue; val c = current ?: return
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == c.id }.let { if (it < 0) 0 else it }
        current = q[((i + delta) % q.size + q.size) % q.size]
        current?.takeIf { !it.isVod }?.let { selectedLiveChannelId = it.id }
    }

    fun closePlayer() { screen = returnTo }

    private fun stopPanelPlaybackIfNeeded() {
        if (current?.sourceId == "panel") {
            current = null
            if (screen == Screen.Player) screen = returnTo
        }
    }

    // ---------------- Catálogo editorial VOD independiente de M3U ----------------
    val tmdbLookupStatus = MutableStateFlow("")

    fun lookupVodOnTmdb(channel: Channel) {
        val apiKey = vodSettings.value.str("tmdbKey", "").trim()
        if (apiKey.isBlank()) {
            tmdbLookupStatus.value = "Configura tu clave de TMDB en Ajustes > Catálogo."
            return
        }
        viewModelScope.launch {
            tmdbLookupStatus.value = "Buscando información…"
            try {
                val found = withContext(Dispatchers.IO) { TmdbRepository.search(channel.name, apiKey) }
                if (found.isNullOrEmpty()) tmdbLookupStatus.value = "No se encontró una coincidencia para este título."
                else {
                    val existing = vodMetadata(channel.id)
                    val merged = linkedMapOf<String, String>()
                    listOf("synopsis", "poster", "backdrop", "year", "genre", "rating", "duration").forEach { key ->
                        val currentValue = existing.optString(key, "")
                        if (currentValue.isNotBlank()) merged[key] = currentValue
                    }
                    found.forEach { (key, value) -> if (merged[key].isNullOrBlank()) merged[key] = value }
                    saveVodMetadata(channel.id, merged)
                    tmdbLookupStatus.value = "Información de TMDB guardada. Puedes editarla cuando quieras."
                }
            } catch (e: Exception) {
                tmdbLookupStatus.value = "No se pudo consultar TMDB: ${e.message ?: "error de conexión"}"
            }
        }
    }

    var vodMetadataVersion by mutableStateOf(0)
        private set

    /** Devuelve los metadatos locales que el usuario haya editado para este título. */
    fun vodMetadata(id: String): org.json.JSONObject = try {
        org.json.JSONObject(prefs.getString("davidlgVodMetadata", "{}")).optJSONObject(id)
            ?: org.json.JSONObject()
    } catch (_: Exception) { org.json.JSONObject() }

    fun saveVodMetadata(id: String, fields: Map<String, String>) {
        val root = try { org.json.JSONObject(prefs.getString("davidlgVodMetadata", "{}")) }
            catch (_: Exception) { org.json.JSONObject() }
        val item = org.json.JSONObject()
        fields.forEach { (key, value) -> if (value.isNotBlank()) item.put(key, value.trim()) }
        root.put(id, item)
        prefs.putString("davidlgVodMetadata", root.toString())
        vodMetadataVersion++
        syncProfileToPanel(prefs.activeProfileId)
    }

    // ---------------- Progreso VOD (davidlgVodProgress) ----------------
    fun vodProgress(id: String): Long = try { prefs.getKv("davidlgVodProgress").json().optLong(id, 0L) } catch (_: Exception) { 0L }
    fun saveVodProgress(id: String, posMs: Long, durMs: Long) {
        val o = prefs.getKv("davidlgVodProgress").json()
        if (durMs > 0 && posMs > durMs * 0.95) o.remove(id) else o.put(id, posMs)
        prefs.putString("davidlgVodProgress", o.toString())
        syncProfileToPanel(prefs.activeProfileId)
    }
}
