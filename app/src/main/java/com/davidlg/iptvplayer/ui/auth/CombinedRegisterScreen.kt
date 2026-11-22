package com.davidlg.iptvplayer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ajusta el import de R según el paquete final de tu app si es necesario:
// import com.davidlg.iptvplayer.R

private val DavidlgFontFamily = FontFamily(
    // Descomenta estas líneas cuando verifiques R.font.poppins_* en tu módulo :app
    // Font(R.font.poppins_regular, FontWeight.Normal),
    // Font(R.font.poppins_medium, FontWeight.Medium),
    // Font(R.font.poppins_bold, FontWeight.Bold)
)

@Composable
fun CombinedRegisterScreen(
    logoResId: Int? = null,
    versionLabel: String = "Versión: 1.0.0_prd Copyright © 2026",
    onBackClick: () -> Unit = {},
    onUseActivationCode: () -> Unit = {},
    onRegisterSubmit: (name: String, emailOrUser: String, password: String, activationCode: String?) -> Unit = { _, _, _, _ -> },
    onSwitchToLogin: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    serverError: String = "",
    loading: Boolean = false
) {
    var displayName by remember { mutableStateOf("") }
    var emailOrUser by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    var showActivationField by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverErrorDismissed by remember { mutableStateOf(false) }
    val visibleError = errorMessage ?: if (serverErrorDismissed) null else serverError.takeIf { it.isNotBlank() }

    val focusManager = LocalFocusManager.current

    val cyanAccent = Color(0xFF00E5D4)
    val darkSurface = Color(0xFF060B0E)
    val inputBg = Color(0xFF23272A)
    val errorAccent = Color(0xFFEF5350)

    // Base oscura + destello cian inferior (estilo izzi go) + degradado cálido lateral derecho (estilo Claro video)
    val baseAndTealBg = Brush.verticalGradient(
        0.0f to darkSurface,
        0.72f to Color(0xFF070D10),
        1.0f to Color(0xFF0B2527)
    )
    val rightCrimsonGlow = Brush.horizontalGradient(
        0.0f to Color.Transparent,
        0.65f to Color.Transparent,
        1.0f to Color(0xFF7A141A).copy(alpha = 0.45f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseAndTealBg)
            .background(rightCrimsonGlow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera superior: botón atrás izquierda + marca derecha (estilo Claro video + acento cian)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FocusableCircleBackButton(onClick = onBackClick)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (logoResId != null) {
                        Image(
                            painter = painterResource(id = logoResId),
                            contentDescription = "DAVIDLG",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DAVIDLG",
                            fontFamily = DavidlgFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "IPTV PLAYER",
                            fontFamily = DavidlgFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = cyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Contenedor principal adaptable para móvil, tablet y Android TV
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Crea tu cuenta",
                    fontFamily = DavidlgFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OPCIÓN 1: Activación rápida
                Text(
                    text = "Opción 1",
                    fontFamily = DavidlgFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Regístrate con tu código de activación:",
                    fontFamily = DavidlgFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                FocusablePillButton(
                    text = "USAR CÓDIGO DE ACTIVACIÓN",
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    focusBorderColor = cyanAccent,
                    onClick = {
                        showActivationField = true
                        onUseActivationCode()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.14f)
                )
                Spacer(modifier = Modifier.height(22.dp))

                // OPCIÓN 2: Registro con correo / usuario y contraseña
                Text(
                    text = "Opción 2",
                    fontFamily = DavidlgFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Crea tu cuenta con tu correo y contraseña:",
                    fontFamily = DavidlgFontFamily,
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                val nameError = errorMessage != null && displayName.isBlank()
                val emailError = errorMessage != null && emailOrUser.isBlank()
                val passwordError = errorMessage != null && password.length < 6
                val confirmError = errorMessage != null && confirmPassword != password

                StackedAuthInput(
                    label = "Nombre",
                    value = displayName,
                    onValueChange = { displayName = it; errorMessage = null; serverErrorDismissed = true },
                    isError = nameError,
                    errorText = if (nameError) "Escribe tu nombre." else null,
                    focusBorderColor = cyanAccent,
                    errorBorderColor = errorAccent,
                    containerColor = inputBg,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                StackedAuthInput(
                    label = "Correo electrónico",
                    value = emailOrUser,
                    onValueChange = {
                        emailOrUser = it
                        errorMessage = null; serverErrorDismissed = true
                    },
                    isError = emailError,
                    errorText = if (emailError) "Este campo es obligatorio." else null,
                    focusBorderColor = cyanAccent,
                    errorBorderColor = errorAccent,
                    containerColor = inputBg,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                StackedAuthInput(
                    label = "Contraseña",
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null; serverErrorDismissed = true
                    },
                    isError = passwordError,
                    errorText = if (passwordError) "Mínimo 6 caracteres." else null,
                    focusBorderColor = cyanAccent,
                    errorBorderColor = errorAccent,
                    containerColor = inputBg,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "◉" else "◎",
                                color = Color(0xFFCBD5E1),
                                fontSize = 19.sp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                StackedAuthInput(
                    label = "Confirmar contraseña",
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null; serverErrorDismissed = true
                    },
                    isError = confirmError,
                    errorText = if (confirmError) "Las contraseñas no coinciden." else null,
                    focusBorderColor = cyanAccent,
                    errorBorderColor = errorAccent,
                    containerColor = inputBg,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (showActivationField) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { focusManager.clearFocus() }
                    )
                )

                AnimatedVisibility(visible = showActivationField) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StackedAuthInput(
                            label = "Código de activación",
                            value = activationCode,
                            onValueChange = { activationCode = it },
                            isError = false,
                            errorText = null,
                            focusBorderColor = cyanAccent,
                            errorBorderColor = errorAccent,
                            containerColor = inputBg,
                            leadingIcon = {
                                Text(
                                    text = "⌑",
                                    color = cyanAccent,
                                    fontSize = 20.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!showActivationField) {
                    FocusableLinkText(
                        prefix = "+ ",
                        actionText = "Tengo un código de activación",
                        accentColor = cyanAccent,
                        onClick = { showActivationField = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (visibleError != null && !emailError && !passwordError && !confirmError && !nameError) {
                    Text(
                        text = visibleError,
                        fontFamily = DavidlgFontFamily,
                        fontSize = 12.sp,
                        color = errorAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón principal en cian vibrante (estilo izzi go)
                FocusablePillButton(
                    text = if (loading) "CREANDO CUENTA…" else "Crear cuenta",
                    containerColor = cyanAccent,
                    contentColor = Color(0xFF041012),
                    focusBorderColor = Color.White,
                    loading = loading,
                    onClick = {
                        when {
                            loading -> Unit
                            displayName.isBlank() -> errorMessage = "Escribe tu nombre."
                            emailOrUser.isBlank() -> errorMessage = "El correo es obligatorio."
                            password.length < 6 -> errorMessage = "Mínimo 6 caracteres."
                            confirmPassword != password -> errorMessage = "Las contraseñas no coinciden."
                            else -> {
                                errorMessage = null; serverErrorDismissed = true
                                onRegisterSubmit(
                                    displayName.trim(),
                                    emailOrUser.trim(),
                                    password,
                                    activationCode.trim().ifEmpty { null }
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                FocusableLinkText(
                    prefix = "¿Ya tienes cuenta? ",
                    actionText = "Inicia sesión aquí",
                    accentColor = cyanAccent,
                    onClick = onSwitchToLogin
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Pie de pantalla: Términos, Privacidad y Versión
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Términos de Uso",
                        fontFamily = DavidlgFontFamily,
                        fontSize = 12.sp,
                        color = cyanAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onTermsClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Política de privacidad",
                        fontFamily = DavidlgFontFamily,
                        fontSize = 12.sp,
                        color = cyanAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onPrivacyClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = versionLabel,
                    fontFamily = DavidlgFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StackedAuthInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorText: String?,
    focusBorderColor: Color,
    errorBorderColor: Color,
    containerColor: Color,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> errorBorderColor
            isFocused -> focusBorderColor
            else -> Color.White.copy(alpha = 0.08f)
        },
        label = "inputBorder"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = isError,
            label = {
                Text(
                    text = label,
                    fontFamily = DavidlgFontFamily,
                    fontSize = 13.sp
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = DavidlgFontFamily,
                fontSize = 15.sp,
                color = Color.White
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                errorContainerColor = containerColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                errorBorderColor = borderColor,
                focusedLabelColor = focusBorderColor,
                unfocusedLabelColor = Color(0xFFCBD5E1),
                errorLabelColor = errorBorderColor,
                cursorColor = focusBorderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )

        if (errorText != null) {
            Text(
                text = errorText,
                fontFamily = DavidlgFontFamily,
                fontSize = 11.sp,
                color = errorBorderColor,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun FocusablePillButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    focusBorderColor: Color,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isFocused) BorderStroke(2.dp, focusBorderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontFamily = DavidlgFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun FocusableLinkText(
    prefix: String,
    actionText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = prefix,
            fontFamily = DavidlgFontFamily,
            fontSize = 13.sp,
            color = Color(0xFFE2E8F0)
        )
        Text(
            text = actionText,
            fontFamily = DavidlgFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = accentColor
        )
    }
}

@Composable
private fun FocusableCircleBackButton(
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color(0xFF00E5D4) else Color.White.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .size(40.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}