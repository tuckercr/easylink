package com.fangjet.launcher.presentation.speeddial.add

import com.fangjet.launcher.domain.model.DeviceContact

/**
 * UI state for the Add Speed Dial Contact picker.
 *
 * Supports two modes:
 *  - Contact search ([isManualEntry] = false): searches device contacts
 *  - Manual entry  ([isManualEntry] = true):  user types name + phone directly
 */
data class AddSpeedDialUiState(
    // ── Mode toggle ───────────────────────────────────────────────────────
    val isManualEntry: Boolean = false,
    // ── Search mode ───────────────────────────────────────────────────────
    val searchQuery: String = "",
    val contacts: List<DeviceContact> = emptyList(),
    val isLoading: Boolean = false,
    // ── Manual entry mode ─────────────────────────────────────────────────
    val manualName: String = "",
    val manualPhone: String = "",
    val manualNameError: String? = null,
    val manualPhoneError: String? = null,
    // ── Save result (both modes) ──────────────────────────────────────────
    val saveResult: SaveResult? = null,
) {
    sealed interface SaveResult {
        data object Success : SaveResult

        data object AlreadyAdded : SaveResult

        data object InvalidContact : SaveResult

        data class Error(
            val message: String,
        ) : SaveResult
    }
}
