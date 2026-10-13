package com.ajtecnology.smartvest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.SessionStore

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

        setContent {
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val selectedProfile by viewModel.selectedProfile.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Vincular el polling exclusivamente al ciclo de vida visible (ON_RESUME / ON_PAUSE)
            DisposableEffect(lifecycleOwner, selectedProfile) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            val devId = selectedProfile?.deviceId
                            if (!devId.isNullOrBlank()) {
                                viewModel.startPolling(devId)
                            }
                        }
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            viewModel.stopPolling()
                        }
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.stopPolling()
                }
            }

            if (isLoggedIn) {
                MonitorScreen(
                    viewModel = viewModel,
                    onLogoutClick = {
                        viewModel.logout()
                    }
                )
            } else {
                LoginScreen(
                    apiClient = apiClient,
                    onLoginSuccess = {
                        viewModel.onLoginSuccess()
                    }
                )
            }
        }
    }
}
