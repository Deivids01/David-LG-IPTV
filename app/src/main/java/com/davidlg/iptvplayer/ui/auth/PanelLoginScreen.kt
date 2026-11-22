package com.davidlg.iptvplayer.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.davidlg.iptvplayer.MainViewModel

@Composable
fun PanelLoginScreen(vm: MainViewModel) {
    val baseUrl = vm.prefs.panelBaseUrl()
    var email by rememberSaveable { mutableStateOf(vm.prefs.panelEmail()) }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    val busy by vm.panelBusy.collectAsState()
    val error by vm.panelError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text("DAVIDLG IPTV PLAYER", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Tu cuenta DavidLG en todos tus dispositivos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { register = false }) { Text("Iniciar sesión") }
                TextButton(onClick = { register = true }) { Text("Crear cuenta") }
            }
            if (register) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico") }, singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), label = { Text(if (register) "Contraseña (mínimo 6 caracteres)" else "Contraseña") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code, onValueChange = { code = it.uppercase() },
                modifier = Modifier.fillMaxWidth(), label = { Text("Código de activación (opcional)") }, singleLine = true
            )
            Spacer(Modifier.height(18.dp))
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = { vm.connectPanel(baseUrl, email, password, code, name, register) },
                enabled = !busy && email.isNotBlank() && password.length >= 6 && (!register || name.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                else Text(if (register) "Crear cuenta y continuar" else "Iniciar sesión")
            }
            Spacer(Modifier.height(12.dp))
            Text("Puedes usar tus propias listas sin contratar un paquete. Si tienes un código, introdúcelo para activar el servicio asociado a tu cuenta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
    }
}
