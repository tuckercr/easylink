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
        val enabledButtons: List<HomeButton> = HomeButton.entries.toList(),
        val weather: WeatherInfo = WeatherInfo.Loading,
    ) : HomeUiState()

    data class Error(
        val message: String,
    ) : HomeUiState()
}
