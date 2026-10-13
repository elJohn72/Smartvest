package com.ajtecnology.smartvest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajtecnology.smartvest.data.ApiClient
import com.ajtecnology.smartvest.data.TelemetryReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface MonitorUiState {
    data object Loading : MonitorUiState
    data object NoData : MonitorUiState
    data class Ready(val reading: TelemetryReading, val isStale: Boolean) : MonitorUiState
    data class NoConnection(val message: String) : MonitorUiState
}

class MonitorViewModel(
    private val apiClient: ApiClient,
    private val deviceId: String = "VEST-001"
) : ViewModel() {

    private val _uiState = MutableStateFlow<MonitorUiState>(MonitorUiState.Loading)
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                val result = apiClient.getTelemetryState(deviceId)
                result.fold(
                    onSuccess = { reading ->
                        if (reading == null) {
                            _uiState.value = MonitorUiState.NoData
                        } else {
                            val now = System.currentTimeMillis()
                            val isStale = (now - reading.lastUpdate) > 30_000L
                            _uiState.value = MonitorUiState.Ready(reading, isStale)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = MonitorUiState.NoConnection(error.localizedMessage ?: "Fallo de red")
                    }
                )

                // Si SOS está activo polling cada 1s; caso contrario cada 2s
                val current = _uiState.value
                val delayMs = if (current is MonitorUiState.Ready && current.reading.sosActive) 1000L else 2000L
                delay(delayMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
