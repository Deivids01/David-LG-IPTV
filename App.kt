package com.davidlg.iptvplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.ui.auth.PanelLoginScreen
import com.davidlg.iptvplayer.ui.classic.ClassicShell
import com.davidlg.iptvplayer.ui.ott.OttShell
import com.davidlg.iptvplayer.ui.player.PlayerScreen
import com.davidlg.iptvplayer.ui.profiles.ProfilesScreen
import com.davidlg.iptvplayer.ui.settings.SettingsScreen

@Composable
fun App(vm: MainViewModel) {
    val vs by vm.vodSettings.collectAsState()
    when (vm.screen) {
        Screen.Login -> PanelLoginScreen(vm)
        Screen.AuthChecking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        Screen.Profiles -> ProfilesScreen(vm)
        Screen.Player -> PlayerScreen(vm)
        Screen.Settings -> SettingsScreen(vm) { vm.screen = Screen.Main }
        Screen.Main -> if (vs.str("style", "ott") == "classic") ClassicShell(vm) else OttShell(vm)
    }
}
