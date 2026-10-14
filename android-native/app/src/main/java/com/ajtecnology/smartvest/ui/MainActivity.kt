package com.ajtecnology.smartvest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.SessionStore
import com.ajtecnology.smartvest.theme.SmartVestTheme

class MainActivity : ComponentActivity() {

    private lateinit var sessionStore: SessionStore
    private lateinit var apiClient: ApiClient

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(apiClient, sessionStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SessionStore(applicationContext)
        apiClient = ApiClient(sessionStore = sessionStore)
        window.statusBarColor = android.graphics.Color.parseColor("#0F172A")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            SmartVestTheme {
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val selectedProfile by viewModel.selectedProfile.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Vincular el polling exclusivamente al ciclo de vida visible (ON_RESUME / ON_PAUSE)
            DisposableEffect(lifecycleOwner, selectedProfile?.id, selectedProfile?.deviceId) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> startVisiblePolling()
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.stopPolling()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    startVisiblePolling()
                }
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.stopPolling()
                }
            }

            if (isLoggedIn) {
                MonitorScreen(
                    viewModel = viewModel,
                    onLogoutClick = { viewModel.logout() }
                )
            } else {
                LoginScreen(
                    apiClient = apiClient,
                    onLoginSuccess = { viewModel.onLoginSuccess() }
                )
            }
            }
        }
    }

    private fun startVisiblePolling() {
        val devId = viewModel.selectedProfile.value?.deviceId
        if (!devId.isNullOrBlank()) {
            viewModel.startPolling(devId)
        }
    }
}
