package com.ajtecnology.smartvest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajtecnology.smartvest.R
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.LoginRequest
import com.ajtecnology.smartvest.data.RegisterRequest
import com.ajtecnology.smartvest.theme.BodyText
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
    val wrongCredentials = stringResource(R.string.login_wrong_credentials)
    val rateLimited = stringResource(R.string.login_rate_limited)
    val labError = stringResource(R.string.login_lab_error)
    val noConnection = stringResource(R.string.login_no_connection)
    val passwordFocus = remember { FocusRequester() }
    fun submit() {
        if (isLoading) return
        if (username.isBlank() || password.isBlank()) {
            errorMessage = missingFields
            return
        }
        if (password.length < 8) {
            errorMessage = shortPassword
            return
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
                onFailure = {
                    errorMessage = when (classifyLoginFailure(it.message)) {
                        LoginFailureKind.WRONG_CREDENTIALS -> wrongCredentials
                        LoginFailureKind.RATE_LIMITED -> rateLimited
                        LoginFailureKind.LAB_ERROR -> labError
                        LoginFailureKind.NO_CONNECTION -> noConnection
                    }
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.mipmap.ic_smartvest), contentDescription = null, modifier = Modifier.size(52.dp))
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Night)
                Text(stringResource(R.string.brand_descriptor), style = MaterialTheme.typography.bodyMedium, color = MetaText)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = if (isRegisterMode) stringResource(R.string.register_title) else stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = Night
        )
        Text(
            text = if (isRegisterMode) stringResource(R.string.register_subtitle) else stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = BodyText,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
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
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage!!, color = SosRed, fontSize = 16.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { submit() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(14.dp),
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
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.lab_access_note), style = MaterialTheme.typography.bodyMedium, color = MetaText)
    }
}
