package com.tuckercr.ezlauncher.presentation.home

import com.tuckercr.ezlauncher.domain.model.HomeButton
import com.tuckercr.ezlauncher.domain.model.WeatherInfo

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
        /** Whether the full-width "Say a Command" bar is shown. Default OFF. */
        val voiceButtonEnabled: Boolean = false,
    ) : HomeUiState()

    data class Error(
        val message: String,
    ) : HomeUiState()
}
