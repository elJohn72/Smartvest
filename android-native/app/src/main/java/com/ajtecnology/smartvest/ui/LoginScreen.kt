package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.LoginRequest
import com.ajtecnology.smartvest.data.RegisterRequest
import com.ajtecnology.smartvest.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    apiClient: ApiClient,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Registro SmartVest" else stringResource(R.string.login_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Night
        )
        Text(
            text = if (isRegisterMode) "Crea una cuenta controlada de laboratorio" else stringResource(R.string.login_subtitle),
            fontSize = 14.sp,
            color = MetaText,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = SosRed,
                fontSize = 13.sp
            )
        }

        if (successMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage!!,
                color = Signal,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Por favor ingrese usuario y contraseña."
                    return@Button
                }
                if (password.length < 8) {
                    errorMessage = "La contraseña debe tener al menos 8 caracteres."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                successMessage = null

                scope.launch {
                    if (isRegisterMode) {
                        val res = apiClient.register(RegisterRequest(username.trim(), password))
                        isLoading = false
                        res.fold(
                            onSuccess = {
                                successMessage = "Cuenta creada. Iniciando sesión..."
                                // Auto login tras registro exitoso
                                val loginRes = apiClient.login(LoginRequest(username.trim(), password))
                                loginRes.fold(
                                    onSuccess = { onLoginSuccess() },
                                    onFailure = { errorMessage = "Cuenta creada pero falló login automático." }
                                )
                            },
                            onFailure = { errorMessage = it.localizedMessage ?: "Fallo al registrar usuario." }
                        )
                    } else {
                        val res = apiClient.login(LoginRequest(username.trim(), password))
                        isLoading = false
                        res.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { errorMessage = it.localizedMessage ?: "Fallo de autenticación." }
                        )
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Signal)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Paper, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (isRegisterMode) "Crear Cuenta de Laboratorio" else stringResource(R.string.login_button),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                errorMessage = null
                successMessage = null
            }
        ) {
            Text(
                text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "¿Nuevo en el laboratorio? Registrarse",
                color = Signal,
                fontSize = 14.sp
            )
        }
    }
}
