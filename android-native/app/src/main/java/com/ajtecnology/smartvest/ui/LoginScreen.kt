package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.LoginRequest
import com.ajtecnology.smartvest.data.RegisterRequest
import com.ajtecnology.smartvest.theme.MetaText
import com.ajtecnology.smartvest.theme.Night
import com.ajtecnology.smartvest.theme.Paper
import com.ajtecnology.smartvest.theme.Signal
import com.ajtecnology.smartvest.theme.SosRed
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val missingFields = stringResource(R.string.login_missing_fields)
    val shortPassword = stringResource(R.string.login_short_password)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (isRegisterMode) stringResource(R.string.register_title) else stringResource(R.string.login_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Night
        )
        Text(
            text = if (isRegisterMode) stringResource(R.string.register_subtitle) else stringResource(R.string.login_subtitle),
            fontSize = 16.sp,
            color = MetaText,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.hide_password else R.string.show_password
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage!!, color = SosRed, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = missingFields
                    return@Button
                }
                if (password.length < 8) {
                    errorMessage = shortPassword
                    return@Button
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = if (isRegisterMode) {
                        apiClient.register(RegisterRequest(username.trim(), password)).fold(
                            onSuccess = { apiClient.login(LoginRequest(username.trim(), password)) },
                            onFailure = { Result.failure(it) }
                        )
                    } else {
                        apiClient.login(LoginRequest(username.trim(), password))
                    }
                    isLoading = false
                    result.fold(
                        onSuccess = { onLoginSuccess() },
                        onFailure = { errorMessage = it.message ?: shortPassword }
                    )
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Signal)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Paper, modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = if (isRegisterMode) stringResource(R.string.register_button) else stringResource(R.string.login_button),
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                errorMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = if (isRegisterMode) stringResource(R.string.switch_to_login) else stringResource(R.string.switch_to_register),
                color = Signal,
                fontSize = 16.sp
            )
        }
    }
}
