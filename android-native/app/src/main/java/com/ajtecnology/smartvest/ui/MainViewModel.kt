package com.ajtecnology.smartvest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ajtecnology.smartvest.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

sealed interface MonitorUiState {
    data object Loading : MonitorUiState
    data object NoData : MonitorUiState
    data class Ready(val reading: TelemetryReading, val isStale: Boolean) : MonitorUiState
    data class NoConnection(val message: String) : MonitorUiState
    data object SessionExpired : MonitorUiState
}

class MainViewModel(
    private val apiClient: ApiClient,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(sessionStore.getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _profiles = MutableStateFlow<List<ProfileDto>>(emptyList())
    val profiles: StateFlow<List<ProfileDto>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<ProfileDto?>(null)
    val selectedProfile: StateFlow<ProfileDto?> = _selectedProfile.asStateFlow()

    private val _monitorState = MutableStateFlow<MonitorUiState>(MonitorUiState.Loading)
    val monitorState: StateFlow<MonitorUiState> = _monitorState.asStateFlow()

    private val _history = MutableStateFlow<List<TelemetryHistoryItem>>(emptyList())
    val history: StateFlow<List<TelemetryHistoryItem>> = _history.asStateFlow()

    private val _qrShareData = MutableStateFlow<ShareCreateResponse?>(null)
    val qrShareData: StateFlow<ShareCreateResponse?> = _qrShareData.asStateFlow()

    private var pollingJob: Job? = null
    private var consecutiveErrors = 0

    init {
        if (_isLoggedIn.value) {
            loadProfiles()
        }
    }

    fun onLoginSuccess() {
        _isLoggedIn.value = true
        loadProfiles()
    }

    fun logout() {
        stopPolling()
        viewModelScope.launch {
            try {
                apiClient.logout()
            } catch (e: Exception) {
                // Ignore logout network error, clear session locally
            } finally {
                sessionStore.clearSession()
                _isLoggedIn.value = false
                _selectedProfile.value = null
                _profiles.value = emptyList()
            }
        }
    }

    fun loadProfiles() {
        viewModelScope.launch {
            apiClient.getProfiles().fold(
                onSuccess = { list ->
                    _profiles.value = list
                    if (_selectedProfile.value == null && list.isNotEmpty()) {
                        selectProfile(list.first())
                    }
                },
                onFailure = { error ->
                    if (error.message == "UNAUTHORIZED") {
                        _monitorState.value = MonitorUiState.SessionExpired
                        _isLoggedIn.value = false
                    }
                }
            )
        }
    }

    fun selectProfile(profile: ProfileDto) {
        _selectedProfile.value = profile
        stopPolling()
        if (!profile.deviceId.isNullOrBlank()) {
            startPolling(profile.deviceId)
            loadHistory(profile.deviceId)
        } else {
            _monitorState.value = MonitorUiState.NoData
        }
    }

    fun createProfile(name: String, phone: String?, blood: String?, obs: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val req = ProfileCreateRequest(
                presentationName = name,
                emergencyPhone = phone,
                bloodType = blood,
                medicalObservations = obs
            )
            apiClient.createProfile(req).onSuccess {
                loadProfiles()
                onComplete()
            }
        }
    }

    fun pairDevice(deviceId: String, pairingCode: String, onComplete: (Boolean) -> Unit) {
        val current = _selectedProfile.value ?: return
        val trimmedDevice = deviceId.trim()
        val trimmedCode = pairingCode.trim()
        if (trimmedDevice.isEmpty() || trimmedCode.isEmpty()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            apiClient.pairDevice(current.id, trimmedDevice, trimmedCode).fold(
                onSuccess = { success ->
                    if (success) loadProfiles()
                    onComplete(success)
                },
                onFailure = { onComplete(false) }
            )
        }
    }

    fun generateQrShare(profileId: String, allowedFields: List<String> = listOf("presentationName", "emergencyPhone", "bloodType")) {
        viewModelScope.launch {
            apiClient.createQrShare(profileId, allowedFields).fold(
                onSuccess = { _qrShareData.value = it },
                onFailure = { /* Log o manejar error */ }
            )
        }
    }

    fun revokeQrShare(profileId: String, shareId: String) {
        viewModelScope.launch {
            apiClient.revokeQrShare(profileId, shareId).onSuccess {
                _qrShareData.value = null
            }
        }
    }

    fun loadHistory(deviceId: String) {
        viewModelScope.launch {
            apiClient.getTelemetryHistory(deviceId, 20).onSuccess {
                _history.value = it
            }
        }
    }

    fun startPolling(deviceId: String) {
        if (pollingJob?.isActive == true) return
        consecutiveErrors = 0

        pollingJob = viewModelScope.launch {
            while (isActive) {
                val result = apiClient.getTelemetryState(deviceId)
                result.fold(
                    onSuccess = { reading ->
                        consecutiveErrors = 0
                        if (reading == null) {
                            _monitorState.value = MonitorUiState.NoData
                        } else {
                            val now = System.currentTimeMillis()
                            val isStale = (now - reading.lastUpdate) > 30_000L
                            _monitorState.value = MonitorUiState.Ready(reading, isStale)
                        }
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        if (error.message == "UNAUTHORIZED") {
                            _monitorState.value = MonitorUiState.SessionExpired
                            _isLoggedIn.value = false
                            return@launch
                        }
                        consecutiveErrors++
                        _monitorState.value = MonitorUiState.NoConnection(error.localizedMessage ?: "Fallo de red")
                    }
                )

                // Polling adaptativo con backoff exponencial ante fallos:
                // Normal: 2000ms | SOS activo: 1000ms | Fallo de red: min(15000ms, 2000ms * 2^fallos)
                val current = _monitorState.value
                val delayMs = if (consecutiveErrors > 0) {
                    min(15_000L, 2_000L * (1 shl min(consecutiveErrors, 3)))
                } else if (current is MonitorUiState.Ready && current.reading.sosActive) {
                    1_000L
                } else {
                    2_000L
                }
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

class MainViewModelFactory(
    private val apiClient: ApiClient,
    private val sessionStore: SessionStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(apiClient, sessionStore) as T
    }
}
