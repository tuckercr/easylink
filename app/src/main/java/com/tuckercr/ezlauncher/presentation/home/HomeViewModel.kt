package com.tuckercr.ezlauncher.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.repository.AppRepository
import com.tuckercr.ezlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

/**
 * ViewModel for the Home (launcher) screen.
 *
 * Key design decisions:
 *  - All state is exposed as [StateFlow] (not LiveData) — works better
 *    with Kotlin coroutines and is lifecycle-aware via [repeatOnLifecycle].
 *  - The ViewModel never references the View or Fragment. It only produces
 *    [HomeUiState] and receives user intent via plain function calls.
 *  - Hilt injects dependencies — no manual factory boilerplate needed.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val _isFlashlightOn = MutableStateFlow(false)
    private val _currentTime = MutableStateFlow(formatTime())

    // ── Public UI state (read-only) ───────────────────────────────────────────

    /**
     * Single source of truth for the Home screen.
     * The Fragment observes this; all rendering decisions are driven by it.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeBatteryState(),
        _isFlashlightOn,
        _currentTime,
    ) { batteryState, flashlight, time ->
        HomeUiState.Success(
            batteryState = batteryState,
            isFlashlightOn = flashlight,
            currentTime = time,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    // ── Clock ─────────────────────────────────────────────────────────────────

    private val clockTimer = Timer().also { timer ->
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _currentTime.value = formatTime()
            }
        }, 0L, 60_000L) // update every minute
    }

    private fun formatTime(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    // ── User intents ──────────────────────────────────────────────────────────

    /** Called when the user taps the flashlight toggle button. */
    fun toggleFlashlight() {
        _isFlashlightOn.update { !it }
        // Actual camera torch control happens in the Fragment via CameraX
        // because it needs a lifecycle owner. The ViewModel only tracks state.
    }

    /** Called when the user taps a quick-launch app tile. */
    fun onAppTapped(packageName: String) {
        viewModelScope.launch {
            launchAppUseCase(packageName)
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        clockTimer.cancel()
    }
}
