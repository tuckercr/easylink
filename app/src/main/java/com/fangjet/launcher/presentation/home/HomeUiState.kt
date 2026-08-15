package com.fangjet.launcher.presentation.home

import com.fangjet.launcher.domain.model.HomeButton
import com.fangjet.shared.config.SettingsDefaults
import com.fangjet.weather.model.WeatherInfo

/**
 * Immutable snapshot of everything the Home screen needs to render.
 */
sealed class HomeUiState {

    data object Loading : HomeUiState()

    data class Success(
        val isFlashlightOn: Boolean = false,
        /** Ordered list of buttons the user has enabled (display order preserved). */
        val enabledButtons: List<HomeButton> = HomeButton.entries.filter { it.defaultEnabled },
        val weather: WeatherInfo = WeatherInfo.Loading,
        /** Whether the "Say a Command" grid tile is shown. Default OFF. */
        val voiceButtonEnabled: Boolean = false,
        /** Whether the full-width SOS button is shown. Default ON. */
        val sosButtonEnabled: Boolean = true,
        /** How long the SOS button must be held to fire, in ms (Remote Config tunable). */
        val sosHoldDurationMs: Long = SettingsDefaults.HARDCODED.sosHoldDurationMs,
    ) : HomeUiState()

    data class Error(
        val message: String,
    ) : HomeUiState()
}
