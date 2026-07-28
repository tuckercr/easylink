package com.fangjet.launcher.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.pairing.PairingRepository
import com.fangjet.shared.PairingCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConnectFamilyUiState {
    /** Talking to Firebase (anonymous sign-in + publishing the code). */
    data object Preparing : ConnectFamilyUiState()

    /** Code on screen, waiting for a caregiver to type it in. */
    data class ShowingCode(
        /** Display-formatted, e.g. "123 456". */
        val code: String,
        val secondsLeft: Int,
    ) : ConnectFamilyUiState()

    /** A caregiver appeared in caregiverUids — pairing worked. */
    data object Connected : ConnectFamilyUiState()

    /** Code expired without being redeemed. Offer a retry. */
    data object Expired : ConnectFamilyUiState()

    /** Couldn't reach Firebase (offline, etc.). */
    data object Error : ConnectFamilyUiState()
}

@HiltViewModel
class ConnectFamilyViewModel @Inject constructor(
    private val repository: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectFamilyUiState>(ConnectFamilyUiState.Preparing)
    val uiState: StateFlow<ConnectFamilyUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null
    private var watcher: Job? = null

    init {
        start()
    }

    /** (Re)generates a code. Also the retry action for Expired/Error. */
    fun start() {
        ticker?.cancel()
        watcher?.cancel()
        _uiState.value = ConnectFamilyUiState.Preparing

        viewModelScope.launch {
            val pairing = try {
                repository.beginPairing()
            } catch (e: Exception) {
                _uiState.value = ConnectFamilyUiState.Error
                return@launch
            }

            val baseline = countBefore(pairing.linkId)
            watcher = viewModelScope.launch {
                repository.caregiverCount(pairing.linkId).collect { count ->
                    if (count > baseline) {
                        ticker?.cancel()
                        _uiState.value = ConnectFamilyUiState.Connected
                    }
                }
            }

            ticker = viewModelScope.launch {
                val formatted = PairingCode.formatForDisplay(pairing.code)
                while (true) {
                    val left =
                        ((pairing.expiresAtMillis - System.currentTimeMillis()) / 1_000).toInt()
                    if (left <= 0) {
                        watcher?.cancel()
                        _uiState.value = ConnectFamilyUiState.Expired
                        return@launch
                    }
                    _uiState.value = ConnectFamilyUiState.ShowingCode(formatted, left)
                    delay(1_000)
                }
            }
        }
    }

    /**
     * The caregiver count that existed before this code was shown, so re-pairing
     * a second family member doesn't instantly flash "Connected" for the first.
     */
    private suspend fun countBefore(linkId: String): Int =
        try {
            repository.caregiverCount(linkId).first()
        } catch (e: Exception) {
            0
        }
}
