package com.tuckercr.ezlauncher.presentation.home

import com.tuckercr.ezlauncher.domain.model.BatteryState

/**
 * Immutable snapshot of everything the Home screen needs to render.
 *
 * Using a sealed class hierarchy (Loading / Success / Error) is the
 * modern recommended pattern — the Fragment switches on the type and
 * never needs null checks or separate boolean flags.
 */
sealed class HomeUiState {

    /** Shown while the first battery broadcast hasn't arrived yet. */
    data object Loading : HomeUiState()

    /**
     * The home screen is ready to display.
     *
     * @param batteryState    Current battery level and charging status.
     * @param isFlashlightOn  Whether the torch is currently active.
     * @param currentTime     Pre-formatted time string for the clock widget.
     */
    data class Success(
        val batteryState: BatteryState,
        val isFlashlightOn: Boolean = false,
        val currentTime: String = "",
    ) : HomeUiState()

    /** Shown if an unrecoverable error occurred (e.g. camera permission denied). */
    data class Error(val message: String) : HomeUiState()
}
