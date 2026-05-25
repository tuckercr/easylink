package com.tuckercr.ezlauncher.presentation.medications

import com.tuckercr.ezlauncher.domain.model.TodayReminder

/**
 * UI state for the Medications (today's schedule) screen.
 *
 * The screen shows one of three states:
 *  - [Loading] — initial load, show a progress indicator
 *  - [Success] — we have data; list may be empty (no meds scheduled today)
 *  - [Error]   — something went wrong reading from Room
 */
sealed interface MedicationsUiState {

    data object Loading : MedicationsUiState

    data class Success(
        val reminders: List<TodayReminder>,
        /** True when every dose for today is TAKEN or MISSED — show a congratulations banner. */
        val allDone: Boolean = reminders.isNotEmpty() &&
            reminders.all { it.status in TodayReminder.TERMINAL_STATUSES },
    ) : MedicationsUiState

    data class Error(val message: String) : MedicationsUiState
}
