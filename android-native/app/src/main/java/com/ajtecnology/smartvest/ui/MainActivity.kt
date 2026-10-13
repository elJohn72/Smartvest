package com.ajtecnology.smartvest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.SessionStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sessionStore: SessionStore
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SessionStore(applicationContext)
        apiClient = ApiClient(sessionStore = sessionStore)

        setContent {
            var isLoggedIn by remember { mutableStateOf(sessionStore.getToken() != null) }
            val scope = rememberCoroutineScope()

            if (isLoggedIn) {
                val monitorViewModel = remember {
                    MonitorViewModel(apiClient).apply { startPolling() }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        monitorViewModel.stopPolling()
                    }
                }

                MonitorScreen(
                    viewModel = monitorViewModel,
                    onLogoutClick = {
                        monitorViewModel.stopPolling()
                        scope.launch {
                            apiClient.logout()
                            isLoggedIn = false
                        }
                    }
                )
            } else {
                LoginScreen(
                    apiClient = apiClient,
                    onLoginSuccess = {
                        isLoggedIn = true
                    }
                )
            }
        }
    }
}
