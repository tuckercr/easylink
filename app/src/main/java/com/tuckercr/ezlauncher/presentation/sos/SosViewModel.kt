package com.tuckercr.ezlauncher.presentation.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.usecase.TriggerSosUseCase
import com.tuckercr.ezlauncher.presentation.sos.SosUiState.Companion.COUNTDOWN_SECONDS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the SOS countdown screen.
 *
 * This ViewModel survives configuration changes (screen rotation), which
 * is critical here — if the user rotates while the countdown is ticking,
 * the countdown must NOT restart. Keeping the countdown in the ViewModel
 * (not the Fragment) is what guarantees that.
 *
 * The countdown is a simple coroutine loop. Because it runs in
 * [viewModelScope], it is automatically cancelled if the user navigates
 * away without pressing the SOS button — a safety net against phantom calls.
 */
@HiltViewModel
class SosViewModel @Inject constructor(
    private val triggerSos: TriggerSosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosUiState>(SosUiState.Counting(COUNTDOWN_SECONDS))
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private fun startCountdown() {
        countdownJob = viewModelScope.launch {
            for (secondsLeft in COUNTDOWN_SECONDS downTo 1) {
                _uiState.value = SosUiState.Counting(secondsLeft)
                delay(1_000)
            }
            // Countdown finished — fire the SOS
            dispatchSos()
        }
    }

    // ── User intents ──────────────────────────────────────────────────────────

    /** Called when the user taps the Cancel button during the countdown. */
    fun cancel() {
        countdownJob?.cancel()
        _uiState.value = SosUiState.Cancelled
    }

    // ── SOS dispatch ──────────────────────────────────────────────────────────

    private fun dispatchSos() {
        viewModelScope.launch {
            _uiState.value = SosUiState.Dispatching
            val result = triggerSos()
            _uiState.value = SosUiState.Done(result)
        }
    }
}
