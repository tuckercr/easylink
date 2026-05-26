package com.tuckercr.ezlauncher.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import com.tuckercr.ezlauncher.data.weather.WeatherService
import com.tuckercr.ezlauncher.domain.model.HomeButton
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
import com.tuckercr.ezlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val launchAppUseCase: LaunchAppUseCase,
    private val homePrefs: HomePreferencesDataSource,
    private val weatherService: WeatherService,
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val isFlashlightOn = MutableStateFlow(false)
    private val weather = MutableStateFlow<WeatherInfo>(WeatherInfo.Loading)

    // ── Public UI state (read-only) ───────────────────────────────────────────

    val uiState: StateFlow<HomeUiState> = combine(
        isFlashlightOn,
        homePrefs.enabledButtons,
        weather,
    ) { flashlight, buttons, weather ->
        HomeUiState.Success(
            isFlashlightOn = flashlight,
            // Preserve the canonical enum order so the grid is stable
            enabledButtons = HomeButton.entries.filter { it in buttons },
            weather = weather,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    // ── Weather ───────────────────────────────────────────────────────────────

    init {
        fetchWeather()
    }

    /** Re-fetch weather (called on init and when the user grants location). */
    fun refreshWeather() {
        fetchWeather()
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            weather.value = WeatherInfo.Loading
            weather.value = weatherService.fetch()
        }
    }

    // ── User intents ──────────────────────────────────────────────────────────

    fun toggleFlashlight() {
        isFlashlightOn.update { !it }
    }

    /** Set the flashlight to a specific on/off state (used by voice commands). */
    fun setFlashlightEnabled(enabled: Boolean) {
        isFlashlightOn.value = enabled
    }

    fun onAppTapped(packageName: String) {
        viewModelScope.launch { launchAppUseCase(packageName) }
    }

    /** Toggle a home button on/off; persisted to DataStore. */
    fun setButtonEnabled(
        button: HomeButton,
        enabled: Boolean,
    ) {
        viewModelScope.launch { homePrefs.setButtonEnabled(button, enabled) }
    }
}
