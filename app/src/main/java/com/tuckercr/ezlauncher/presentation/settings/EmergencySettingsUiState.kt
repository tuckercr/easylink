package com.tuckercr.ezlauncher.presentation.settings

import com.tuckercr.ezlauncher.domain.model.EmergencyContact

sealed class EmergencySettingsUiState {
    data object Loading : EmergencySettingsUiState()

    data class Ready(
        val contacts: List<EmergencyContact>,
        /** Non-null while the add/edit dialog should be shown. */
        val editingContact: EmergencyContact? = null,
        /** Validation error message for the current edit, if any. */
        val validationError: String? = null,
        /** One-shot event: show a snackbar with this message. */
        val snackbarMessage: String? = null,
    ) : EmergencySettingsUiState()
}
