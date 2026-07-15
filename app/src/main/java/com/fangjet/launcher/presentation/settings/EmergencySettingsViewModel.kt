package com.fangjet.launcher.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.repository.SosRepository
import com.fangjet.launcher.domain.usecase.GetEmergencyContactsUseCase
import com.fangjet.launcher.domain.usecase.SaveEmergencyContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class EmergencySettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    getContacts: GetEmergencyContactsUseCase,
    private val saveContact: SaveEmergencyContactUseCase,
    private val repository: SosRepository,
) : ViewModel() {

    // Internal state that isn't driven by the DB
    private data class LocalState(
        val editingContact: EmergencyContact? = null,
        /**
         * Monotonically-increasing key, incremented every time the dialog is opened.
         * Used as a Compose `remember` key so the dialog fields always reset to the
         * contact's current values — even when opening "Add Contact" twice in a row
         * (where contact.id would be 0 both times and wouldn't act as a changed key).
         */
        val dialogKey: Int = 0,
        val validationError: String? = null,
        val snackbarMessage: String? = null,
    )

    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<EmergencySettingsUiState> = combine(
        getContacts(),
        localState,
    ) { contacts, local ->
        EmergencySettingsUiState.Ready(
            contacts = contacts,
            editingContact = local.editingContact,
            dialogKey = local.dialogKey,
            validationError = local.validationError,
            snackbarMessage = local.snackbarMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmergencySettingsUiState.Loading,
    )

    // ── User intents ──────────────────────────────────────────────────────────

    /** Open the add dialog pre-populated for a new contact. */
    fun onAddContactClicked() {
        localState.update {
            it.copy(
                editingContact = EmergencyContact(name = "", phoneNumber = ""),
                dialogKey = it.dialogKey + 1,
                validationError = null,
            )
        }
    }

    /** Open the edit dialog pre-populated with an existing contact. */
    fun onEditContactClicked(contact: EmergencyContact) {
        localState.update {
            it.copy(
                editingContact = contact,
                dialogKey = it.dialogKey + 1,
                validationError = null,
            )
        }
    }

    /** Close the dialog without saving. */
    fun onDismissDialog() {
        localState.update { it.copy(editingContact = null, validationError = null) }
    }

    /** Validate and persist the contact from the dialog. */
    fun onSaveContact(
        name: String,
        phone: String,
        isPrimary: Boolean,
    ) {
        val current = localState.value.editingContact ?: return
        val updated =
            current.copy(name = name.trim(), phoneNumber = phone.trim(), isPrimary = isPrimary)

        viewModelScope.launch {
            when (val result = saveContact(updated)) {
                is SaveEmergencyContactUseCase.Result.Success -> {
                    localState.update {
                        it.copy(
                            editingContact = null,
                            validationError = null,
                            snackbarMessage = context.getString(R.string.contact_saved),
                        )
                    }
                }

                is SaveEmergencyContactUseCase.Result.InvalidName ->
                    localState.update { it.copy(validationError = result.message) }

                is SaveEmergencyContactUseCase.Result.InvalidPhone ->
                    localState.update { it.copy(validationError = result.message) }
            }
        }
    }

    fun onDeleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contact)
            localState.update { it.copy(snackbarMessage = context.getString(R.string.contact_removed, contact.name)) }
        }
    }

    /** Called after the Fragment shows the snackbar — prevents re-showing on recomposition. */
    fun onSnackbarShown() {
        localState.update { it.copy(snackbarMessage = null) }
    }
}
