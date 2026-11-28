package com.davidlg.iptvplayer.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.BuildConfig
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

private val Night = Color(0xFF070B16)
private val Panel = Color(0xFF111A2C)
private val Blue = Color(0xFF238BFF)
private val Cyan = Color(0xFF35D5F2)
private val Muted = Color(0xFF9EACC3)

@Composable
fun PanelLoginScreen(vm: MainViewModel) {
    val baseUrl = vm.prefs.panelBaseUrl()
    var email by rememberSaveable { mutableStateOf(vm.prefs.panelEmail()) }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val pairingCode = vm.pairingCode
    val pairingStatus = vm.pairingStatus
    val pairingBusy = vm.pairingBusy
    val pairingBitmap = remember(pairingCode) { pairingCode.takeIf { it.isNotBlank() }?.let { createPairingQr(it) } }
    val busy by vm.panelBusy.collectAsState()
    val error by vm.panelError.collectAsState()

    // La pantalla de registro usa el diseño nativo premium proporcionado,
    // mientras que el inicio de sesión conserva la conexión existente al panel.
    if (register) {
        CombinedRegisterScreen(
            versionLabel = "DAVIDLG IPTV PLAYER · ${BuildConfig.VERSION_NAME}",
            serverError = error,
            loading = busy,
            onBackClick = { register = false; vm.panelError.value = "" },
            onUseActivationCode = { },
            onRegisterSubmit = { displayName, registerEmail, registerPassword, activation ->
                vm.connectPanel(baseUrl, registerEmail, registerPassword, activation.orEmpty(), displayName, true)
            },
            onSwitchToLogin = { register = false; vm.panelError.value = "" }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF071426), Night, Color(0xFF101329)))
        )
    ) {
        val wide = maxWidth >= 760.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().padding(start = 54.dp, end = 34.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    BrandMark()
                    Spacer(Modifier.height(46.dp))
                    Text("TODO TU\nENTRETENIMIENTO.", color = Color.White, fontSize = 39.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(16.dp))
                    Text("Una sola cuenta. Tu contenido y tus dispositivos, siempre conectados.", color = Muted, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.widthIn(max = 440.dp))
                    Spacer(Modifier.height(34.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeaturePill("TV en vivo")
                        FeaturePill("Películas")
                        FeaturePill("Series")
                    }
                }
            }
            Box(
                modifier = Modifier.weight(if (wide) 0.9f else 1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 34.dp else 22.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 490.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xF018263D), Color(0xF00C1425))))
                        .border(1.dp, Color(0x334C8EDB), RoundedCornerShape(28.dp))
                        .padding(if (wide) 34.dp else 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (!wide) {
                        BrandMark()
                        Spacer(Modifier.height(28.dp))
                    }
                    Text(if (register) "Crea tu cuenta" else "Qué gusto tenerte de vuelta", color = Color.White, fontSize = if (wide) 27.sp else 25.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(if (register) "Regístrate para disfrutar DavidLG en tus dispositivos." else "Inicia sesión para continuar disfrutando.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF091222)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthTab("Iniciar sesión", !register, Modifier.weight(1f)) { register = false; vm.panelError.value = "" }
                        AuthTab("Crear cuenta", register, Modifier.weight(1f)) { register = true; vm.panelError.value = "" }
                    }
                    Spacer(Modifier.height(22.dp))

                    if (register) {
                        FieldLabel("NOMBRE")
                        PremiumField(name, { name = it }, "Cómo te llamas")
                        Spacer(Modifier.height(14.dp))
                    }
                    FieldLabel("CORREO ELECTRÓNICO")
                    PremiumField(email, { email = it }, "tucorreo@ejemplo.com", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("CONTRASEÑA")
                    PremiumField(password, { password = it }, "Mínimo 6 caracteres", visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation())
                    TextButton(onClick = { showPassword = !showPassword }, modifier = Modifier.align(Alignment.End)) {
                        Text(if (showPassword) "Ocultar contraseña" else "Mostrar contraseña", color = Cyan, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    FieldLabel("CÓDIGO DE ACTIVACIÓN · OPCIONAL")
                    PremiumField(code, { code = it.uppercase() }, "Introduce tu código si tienes uno")

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Surface(color = Color(0xFF3A1923), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(error, color = Color(0xFFFFB7C2), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(13.dp))
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { vm.connectPanel(baseUrl, email, password, code, name, register) },
                        enabled = !busy && email.isNotBlank() && password.length >= 6 && (!register || name.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White, disabledContainerColor = Color(0xFF34445C), disabledContentColor = Color(0xFF9BA9BC))
                    ) {
                        if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(21.dp))
                        else Text(if (register) "CREAR CUENTA Y CONTINUAR" else "INICIAR SESIÓN", fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { if (pairingBusy) vm.cancelDevicePairing() else vm.startDevicePairing(baseUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (pairingBusy) "Cancelar vinculación" else "Vincular este televisor con otro dispositivo", color = Cyan, fontSize = 13.sp)
                    }
                    if (pairingCode.isNotBlank()) {
                        Surface(color = Color(0xFF071222), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x5535D5F2), RoundedCornerShape(16.dp))) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ESCANEA O INTRODUCE EL CÓDIGO", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                pairingBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "Código QR temporal para vincular el televisor", contentScale = ContentScale.Fit, modifier = Modifier.width(180.dp).height(180.dp).clip(RoundedCornerShape(10.dp))) }
                                Text(pairingCode.chunked(5).joinToString(" "), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                                Text(pairingStatus, color = Cyan, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                    Text("Tu cuenta funciona en tus dispositivos compatibles. También puedes usar tus propias listas locales sin contratar un paquete.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("DAVIDLG IPTV PLAYER  •  EXPERIENCIA MULTIPANTALLA", color = Color(0xFF657994), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

private fun createPairingQr(value: String): Bitmap? = runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 420, 420, hints)
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
        pixels[y * matrix.width + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}.getOrNull()

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Box(modifier = Modifier.width(42.dp).height(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Blue, Cyan))), contentAlignment = Alignment.Center) {
            Text("D", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("DAVIDLG", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text("IPTV PLAYER", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
        }
    }
}

@Composable
private fun FeaturePill(label: String) {
    Surface(color = Color(0x22238BFF), shape = CircleShape, modifier = Modifier.border(1.dp, Color(0x444AA9FF), CircleShape)) {
        Text(label, color = Color(0xFFB9D9FF), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(43.dp), shape = RoundedCornerShape(11.dp), color = if (selected) Color(0xFF1C6FD0) else Color.Transparent) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Muted, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Color(0xFF9FB6D3), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.15.sp)
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun PremiumField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visual: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = Color(0xFF71829A), fontSize = 14.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visual,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color(0xFFE8F0FC),
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Color(0xFF33445F),
            cursorColor = Cyan,
            focusedContainerColor = Color(0xFF0A1424),
            unfocusedContainerColor = Color(0xFF0A1424)
        )
    )
}
