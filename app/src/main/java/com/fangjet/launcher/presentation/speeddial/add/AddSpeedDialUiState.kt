package com.fangjet.launcher.presentation.speeddial.add

import android.net.Uri
import com.fangjet.launcher.domain.model.DeviceContact

/**
 * UI state for the Add / Edit Person screen.
 *
 * Add mode supports two entry paths:
 *  - Contact search ([isManualEntry] = false): searches device contacts
 *  - Manual entry  ([isManualEntry] = true):  user types name + phone directly
 *
 * Edit mode ([isEditMode] = true) reuses the manual entry form, prefilled with
 * the person being edited; the mode tabs are hidden and a Delete button appears.
 */
data class AddSpeedDialUiState(
    // ── Mode ──────────────────────────────────────────────────────────────
    val isManualEntry: Boolean = false,
    val isEditMode: Boolean = false,
    // ── Search mode ───────────────────────────────────────────────────────
    val searchQuery: String = "",
    val contacts: List<DeviceContact> = emptyList(),
    val isLoading: Boolean = false,
    // ── Manual entry / edit form ──────────────────────────────────────────
    val manualName: String = "",
    val manualPhone: String = "",
    val manualPhotoUri: Uri? = null,
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
