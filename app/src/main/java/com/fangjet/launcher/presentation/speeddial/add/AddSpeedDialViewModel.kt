package com.fangjet.launcher.presentation.speeddial.add

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.data.contacts.ContactPhotoStore
import com.fangjet.launcher.domain.model.DeviceContact
import com.fangjet.launcher.domain.model.SpeedDialContact
import com.fangjet.launcher.domain.repository.SpeedDialRepository
import com.fangjet.launcher.domain.usecase.AddSpeedDialContactUseCase
import com.fangjet.launcher.domain.usecase.RemoveSpeedDialContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AddSpeedDialViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addContact: AddSpeedDialContactUseCase,
    private val removeContact: RemoveSpeedDialContactUseCase,
    private val repository: SpeedDialRepository,
    private val photoStore: ContactPhotoStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSpeedDialUiState())
    val uiState: StateFlow<AddSpeedDialUiState> = _uiState.asStateFlow()

    /** The person being edited, or null in add mode. */
    private var editing: SpeedDialContact? = null

    init {
        val editId = savedStateHandle.get<Long>("speedDialId")?.takeIf { it > 0 }
        if (editId != null) {
            loadForEdit(editId)
        } else {
            observeSearchQuery()
        }
    }

    // ── Edit mode ─────────────────────────────────────────────────────────

    private fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val contact = repository.getContact(id) ?: return@launch
            editing = contact
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    isManualEntry = true,
                    manualName = contact.name,
                    manualPhone = contact.phoneNumber,
                    manualPhotoUri = contact.photoUri,
                )
            }
        }
    }

    /** Delete the person being edited (edit mode only). */
    fun onDelete() {
        val contact = editing ?: return
        viewModelScope.launch {
            removeContact(contact)
            _uiState.update { it.copy(saveResult = AddSpeedDialUiState.SaveResult.Success) }
        }
    }

    // ── Mode toggle ───────────────────────────────────────────────────────

    fun onToggleManualEntry() = _uiState.update { it.copy(isManualEntry = !it.isManualEntry) }

    // ── Search mode ───────────────────────────────────────────────────────

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _uiState
                .map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(300)
                .collectLatest { query ->
                    _uiState.update { it.copy(isLoading = true) }
                    val results = runCatching {
                        repository.searchDeviceContacts(query)
                    }.getOrElse { emptyList() }
                    _uiState.update { it.copy(contacts = results, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onContactSelected(contact: DeviceContact) {
        viewModelScope.launch {
            _uiState.update { it.copy(saveResult = addContact(contact).toSaveResult()) }
        }
    }

    // ── Manual entry / edit form ──────────────────────────────────────────

    fun onManualNameChanged(value: String) = _uiState.update { it.copy(manualName = value, manualNameError = null) }

    fun onManualPhoneChanged(value: String) = _uiState.update { it.copy(manualPhone = value, manualPhoneError = null) }

    /**
     * Called with the URI returned by the system photo picker (null = cancelled).
     * The image is copied into app storage so it outlives the picker's grant.
     */
    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val stored = photoStore.import(uri) ?: return@launch
            _uiState.update { it.copy(manualPhotoUri = stored) }
        }
    }

    fun onManualSave() {
        val state = _uiState.value
        var valid = true

        if (state.manualName.isBlank()) {
            _uiState.update { it.copy(manualNameError = "Name is required") }
            valid = false
        }
        val digits = state.manualPhone.filter { it.isDigit() }
        if (digits.length < 7) {
            _uiState.update { it.copy(manualPhoneError = "Enter a valid phone number") }
            valid = false
        }
        if (!valid) return

        val edited = editing
        if (edited != null) {
            viewModelScope.launch {
                repository.updateContact(
                    edited.copy(
                        name = state.manualName.trim(),
                        phoneNumber = state.manualPhone.trim(),
                        photoUri = state.manualPhotoUri,
                    ),
                )
                _uiState.update { it.copy(saveResult = AddSpeedDialUiState.SaveResult.Success) }
            }
            return
        }

        viewModelScope.launch {
            // contactId = -1 signals a manually entered contact;
            // AddSpeedDialContactUseCase dedupes by phone number for these.
            val contact = DeviceContact(
                contactId = -1L,
                name = state.manualName.trim(),
                phoneNumber = state.manualPhone.trim(),
                photoUri = state.manualPhotoUri,
            )
            _uiState.update { it.copy(saveResult = addContact(contact).toSaveResult()) }
        }
    }

    fun onSaveResultConsumed() = _uiState.update { it.copy(saveResult = null) }

    private fun AddSpeedDialContactUseCase.Result.toSaveResult(): AddSpeedDialUiState.SaveResult =
        when (this) {
            AddSpeedDialContactUseCase.Result.Success -> AddSpeedDialUiState.SaveResult.Success
            AddSpeedDialContactUseCase.Result.AlreadyAdded -> AddSpeedDialUiState.SaveResult.AlreadyAdded
            AddSpeedDialContactUseCase.Result.InvalidContact -> AddSpeedDialUiState.SaveResult.InvalidContact
        }
}
