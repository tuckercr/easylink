package com.tuckercr.ezlauncher.presentation.speeddial

import com.tuckercr.ezlauncher.domain.model.SpeedDialContact

/**
 * UI state for the Speed Dial screen.
 *
 *  - [Loading]  — initial Room query in flight
 *  - [Empty]    — no contacts pinned yet; show "Add favorite" prompt
 *  - [Success]  — grid of photo tiles
 *  - [Error]    — DB read failed
 *
 * [CallInProgress] is a transient overlay state shown while the phone app
 * launches (prevents double-tap races while the system responds).
 */
sealed interface SpeedDialUiState {

    data object Loading : SpeedDialUiState

    data object Empty : SpeedDialUiState

    data class Success(
        val contacts: List<SpeedDialContact>,
        val callInProgress: Boolean = false,
    ) : SpeedDialUiState

    data class Error(
        val message: String,
    ) : SpeedDialUiState
}
