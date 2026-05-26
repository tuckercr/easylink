package com.tuckercr.ezlauncher.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import com.tuckercr.ezlauncher.data.weather.WeatherService
import com.tuckercr.ezlauncher.domain.model.HomeButton
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val homePrefs: HomePreferencesDataSource,
    private val weatherService: WeatherService,
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val _isFlashlightOn = MutableStateFlow(false)
    private val _currentTime    = MutableStateFlow(formatTime())
    private val _weather        = MutableStateFlow<WeatherInfo>(WeatherInfo.Loading)

    // ── Public UI state (read-only) ───────────────────────────────────────────

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeBatteryState(),
        _isFlashlightOn,
        _currentTime,
        homePrefs.enabledButtons,
        _weather,
    ) { battery, flashlight, time, buttons, weather ->
        HomeUiState.Success(
            batteryState   = battery,
            isFlashlightOn = flashlight,
            currentTime    = time,
            // Preserve the canonical enum order so the grid is stable
            enabledButtons = HomeButton.entries.filter { it in buttons },
            weather        = weather,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    // ── Clock ─────────────────────────────────────────────────────────────────

    private val clockTimer = Timer().also { timer ->
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { _currentTime.value = formatTime() }
        }, 0L, 60_000L)
    }

    private fun formatTime(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    // ── Weather ───────────────────────────────────────────────────────────────

    init { fetchWeather() }

    /** Re-fetch weather (called on init and when the user grants location). */
    fun refreshWeather() { fetchWeather() }

    private fun fetchWeather() {
        viewModelScope.launch {
            _weather.value = WeatherInfo.Loading
            _weather.value = weatherService.fetch()
        }
    }

    // ── User intents ──────────────────────────────────────────────────────────

    fun toggleFlashlight() {
        _isFlashlightOn.update { !it }
    }

    /** Set the flashlight to a specific on/off state (used by voice commands). */
    fun setFlashlightEnabled(enabled: Boolean) {
        _isFlashlightOn.value = enabled
    }

    fun onAppTapped(packageName: String) {
        viewModelScope.launch { launchAppUseCase(packageName) }
    }

    /** Toggle a home button on/off; persisted to DataStore. */
    fun setButtonEnabled(button: HomeButton, enabled: Boolean) {
        viewModelScope.launch { homePrefs.setButtonEnabled(button, enabled) }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        clockTimer.cancel()
    }
}
