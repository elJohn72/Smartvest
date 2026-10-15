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
    data class NoConnection(
        val message: String,
        val lastReading: TelemetryReading? = null
    ) : MonitorUiState
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

    private val _monitorState = MutableStateFlow<MonitorUiState>(MonitorUiState.NoData)
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
            } catch (_: Exception) {
                sessionStore.clearSession()
            } finally {
                clearLocalSession()
            }
        }
    }

    fun loadProfiles() {
        viewModelScope.launch { refreshProfiles() }
    }

    fun selectProfile(profile: ProfileDto) {
        _selectedProfile.value = profile
        _qrShareData.value = null
        beginWatch(profile)
    }

    fun createProfile(
        name: String,
        phone: String?,
        blood: String?,
        obs: String?,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val req = profileRequest(name, phone, blood, obs)
            apiClient.createProfile(req).fold(
                onSuccess = { created ->
                    refreshProfiles(created.id)
                    onComplete(true)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun updateProfile(
        name: String,
        phone: String?,
        blood: String?,
        obs: String?,
        onComplete: (Boolean) -> Unit
    ) {
        val profileId = _selectedProfile.value?.id
        if (profileId.isNullOrBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            apiClient.updateProfile(profileId, profileRequest(name, phone, blood, obs)).fold(
                onSuccess = {
                    refreshProfiles(profileId)
                    onComplete(true)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun deleteProfile(onComplete: (Boolean) -> Unit) {
        val profileId = _selectedProfile.value?.id
        if (profileId.isNullOrBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            apiClient.deleteProfile(profileId).fold(
                onSuccess = {
                    _selectedProfile.value = null
                    _qrShareData.value = null
                    stopPolling()
                    _history.value = emptyList()
                    _monitorState.value = MonitorUiState.NoData
                    refreshProfiles()
                    onComplete(true)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun pairDevice(deviceId: String, pairingCode: String, onComplete: (Boolean) -> Unit) {
        val current = _selectedProfile.value
        val trimmedDevice = deviceId.trim()
        val trimmedCode = pairingCode.trim()
        if (current == null || trimmedDevice.isEmpty() || trimmedCode.isEmpty()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            apiClient.pairDevice(current.id, trimmedDevice, trimmedCode).fold(
                onSuccess = { success ->
                    if (success) refreshProfiles(current.id)
                    onComplete(success)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun generateQrShare(
        profileId: String,
        allowedFields: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val previous = _qrShareData.value
            apiClient.createQrShare(profileId, allowedFields).fold(
                onSuccess = { created ->
                    _qrShareData.value = created
                    if (previous != null && previous.shareId != created.shareId) {
                        apiClient.revokeQrShare(profileId, previous.shareId)
                    }
                    onComplete(true)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun revokeQrShare(profileId: String, shareId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            apiClient.revokeQrShare(profileId, shareId).fold(
                onSuccess = {
                    if (_qrShareData.value?.shareId == shareId) {
                        _qrShareData.value = null
                    }
                    onComplete(true)
                },
                onFailure = { error ->
                    if (!handleAuthFailure(error)) onComplete(false)
                }
            )
        }
    }

    fun startPolling(deviceId: String) {
        if (pollingJob?.isActive == true) return
        consecutiveErrors = 0
        var lastSequence = Long.MIN_VALUE

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
                            _monitorState.value = MonitorUiState.Ready(
                                reading = reading,
                                isStale = isReadingStale(now, reading.lastUpdate)
                            )
                            if (reading.sequence != lastSequence) {
                                lastSequence = reading.sequence
                                apiClient.getTelemetryHistory(deviceId, 20).fold(
                                    onSuccess = { _history.value = it },
                                    onFailure = { error ->
                                        if (handleAuthFailure(error)) return@launch
                                    }
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        if (handleAuthFailure(error)) return@launch
                        consecutiveErrors++
                        val previous = currentReading()
                        _monitorState.value = MonitorUiState.NoConnection(
                            message = "SIN CONEXIÓN",
                            lastReading = previous
                        )
                    }
                )

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

    private suspend fun refreshProfiles(preferredId: String? = null) {
        val prefer = preferredId ?: _selectedProfile.value?.id
        apiClient.getProfiles().fold(
            onSuccess = { list ->
                val target = prefer ?: list.firstOrNull()?.id
                applyProfileList(list, target)
            },
            onFailure = { error ->
                if (!handleAuthFailure(error) && _profiles.value.isEmpty()) {
                    _monitorState.value = MonitorUiState.NoConnection(
                        message = error.message ?: "SIN CONEXIÓN"
                    )
                }
            }
        )
    }

    private fun applyProfileList(list: List<ProfileDto>, targetId: String?) {
        _profiles.value = list
        val match = targetId?.let { id -> list.find { it.id == id } } ?: list.firstOrNull()
        if (match == null) {
            _selectedProfile.value = null
            _qrShareData.value = null
            stopPolling()
            _history.value = emptyList()
            _monitorState.value = MonitorUiState.NoData
            return
        }

        val previous = _selectedProfile.value
        val deviceChanged = previous?.id != match.id || previous.deviceId != match.deviceId
        _selectedProfile.value = match
        if (match.deviceId.isNullOrBlank()) {
            stopPolling()
            _history.value = emptyList()
            if (previous?.id != match.id) _qrShareData.value = null
            _monitorState.value = MonitorUiState.NoData
            return
        }
        if (deviceChanged || pollingJob?.isActive != true) {
            if (previous?.id != match.id) _qrShareData.value = null
            beginWatch(match)
        }
    }

    private fun beginWatch(profile: ProfileDto) {
        stopPolling()
        if (profile.deviceId.isNullOrBlank()) {
            _history.value = emptyList()
            _monitorState.value = MonitorUiState.NoData
            return
        }
        _monitorState.value = MonitorUiState.Loading
        startPolling(profile.deviceId)
    }

    private fun currentReading(): TelemetryReading? = when (val state = _monitorState.value) {
        is MonitorUiState.Ready -> state.reading
        is MonitorUiState.NoConnection -> state.lastReading
        else -> null
    }

    private fun handleAuthFailure(error: Throwable): Boolean {
        if (error.message != "UNAUTHORIZED") return false
        stopPolling()
        clearLocalSession()
        return true
    }

    private fun clearLocalSession() {
        _isLoggedIn.value = false
        _selectedProfile.value = null
        _profiles.value = emptyList()
        _history.value = emptyList()
        _qrShareData.value = null
        _monitorState.value = MonitorUiState.NoData
    }

    private fun profileRequest(
        name: String,
        phone: String?,
        blood: String?,
        obs: String?
    ) = ProfileCreateRequest(
        presentationName = name.trim(),
        emergencyPhone = phone?.trim()?.ifBlank { null },
        bloodType = blood?.trim()?.ifBlank { null },
        medicalObservations = obs?.trim()?.ifBlank { null }
    )
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
