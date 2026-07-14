package com.fangjet.launcher.presentation.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class TimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
) {
    val isSet: Boolean get() = totalSeconds > 0
    val progress: Float
        get() = if (totalSeconds == 0) {
            0f
        } else {
            remainingSeconds.toFloat() / totalSeconds.toFloat()
        }
    val displayTime: String
        get() {
            val h = remainingSeconds / 3600
            val m = (remainingSeconds % 3600) / 60
            val s = remainingSeconds % 60
            return if (h > 0) {
                "%d:%02d:%02d".format(h, m, s)
            } else {
                "%02d:%02d".format(m, s)
            }
        }
}

@HiltViewModel
class ClockViewModel @Inject constructor() : ViewModel() {

    private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a")

    private val _currentTime = MutableStateFlow(LocalTime.now().format(clockFormatter))
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var clockJob: Job? = null
    private var timerJob: Job? = null

    init {
        startClock()
    }

    // ── Live clock ────────────────────────────────────────────────────────

    private fun startClock() {
        clockJob = viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                _currentTime.value = now.format(clockFormatter)
                // Sleep until the top of the next minute so the display flips on time.
                val msUntilNextMinute = (60 - now.second) * 1_000L - now.nano / 1_000_000L
                delay(msUntilNextMinute.milliseconds)
            }
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    /** Set the timer duration. Resets any running timer. */
    fun setTimer(seconds: Int) {
        timerJob?.cancel()
        _timerState.value = TimerState(totalSeconds = seconds, remainingSeconds = seconds)
    }

    /** Toggle between running and paused. Starts from current remaining time. */
    fun startPause() {
        val state = _timerState.value
        if (!state.isSet || state.isFinished) return

        if (state.isRunning) {
            timerJob?.cancel()
            _timerState.update { it.copy(isRunning = false) }
        } else {
            _timerState.update { it.copy(isRunning = true, isFinished = false) }
            timerJob = viewModelScope.launch {
                while (_timerState.value.remainingSeconds > 0) {
                    delay(1_000.milliseconds)
                    _timerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                }
                _timerState.update { it.copy(isRunning = false, isFinished = true) }
            }
        }
    }

    /** Reset timer to its originally set duration. */
    fun resetTimer() {
        timerJob?.cancel()
        _timerState.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                isRunning = false,
                isFinished = false,
            )
        }
    }

    public override fun onCleared() {
        clockJob?.cancel()
        timerJob?.cancel()
    }
}
