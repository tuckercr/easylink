package com.fangjet.launcher.presentation.settings

import com.fangjet.launcher.domain.model.EmergencyContact

sealed class EmergencySettingsUiState {
    data object Loading : EmergencySettingsUiState()

    data class Ready(
        val contacts: List<EmergencyContact>,
        /** Non-null while the add/edit dialog should be shown. */
        val editingContact: EmergencyContact? = null,
        /**
         * Incremented each time the dialog is opened. Used as a Compose `remember`
         * key so dialog field state always resets to the contact's current values,
         * even when re-opening "Add Contact" where the contact id is always 0.
         */
        val dialogKey: Int = 0,
        /** Validation error message for the current edit, if any. */
        val validationError: String? = null,
        /** One-shot event: show a snackbar with this message. */
        val snackbarMessage: String? = null,
    ) : EmergencySettingsUiState()
}
