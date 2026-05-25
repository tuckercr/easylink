package com.tuckercr.ezlauncher.presentation.speeddial.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.model.DeviceContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import com.tuckercr.ezlauncher.domain.usecase.AddSpeedDialContactUseCase
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
    private val addContact: AddSpeedDialContactUseCase,
    private val repository: SpeedDialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSpeedDialUiState())
    val uiState: StateFlow<AddSpeedDialUiState> = _uiState.asStateFlow()

    init { observeSearchQuery() }

    // ── Mode toggle ───────────────────────────────────────────────────────

    fun onToggleManualEntry() =
        _uiState.update { it.copy(isManualEntry = !it.isManualEntry) }

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

    fun onSearchQueryChanged(query: String) =
        _uiState.update { it.copy(searchQuery = query) }

    fun onContactSelected(contact: DeviceContact) {
        viewModelScope.launch {
            val result = when (addContact(contact)) {
                AddSpeedDialContactUseCase.Result.Success ->
                    AddSpeedDialUiState.SaveResult.Success
                AddSpeedDialContactUseCase.Result.AlreadyAdded ->
                    AddSpeedDialUiState.SaveResult.AlreadyAdded
                AddSpeedDialContactUseCase.Result.InvalidContact ->
                    AddSpeedDialUiState.SaveResult.InvalidContact
            }
            _uiState.update { it.copy(saveResult = result) }
        }
    }

    // ── Manual entry mode ─────────────────────────────────────────────────

    fun onManualNameChanged(value: String) =
        _uiState.update { it.copy(manualName = value, manualNameError = null) }

    fun onManualPhoneChanged(value: String) =
        _uiState.update { it.copy(manualPhone = value, manualPhoneError = null) }

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

        viewModelScope.launch {
            // contactId = -1 signals a manually entered contact;
            // AddSpeedDialContactUseCase dedupes by phone number for these.
            val contact = DeviceContact(
                contactId   = -1L,
                name        = state.manualName.trim(),
                phoneNumber = state.manualPhone.trim(),
                photoUri    = null,
            )
            val result = when (addContact(contact)) {
                AddSpeedDialContactUseCase.Result.Success ->
                    AddSpeedDialUiState.SaveResult.Success
                AddSpeedDialContactUseCase.Result.AlreadyAdded ->
                    AddSpeedDialUiState.SaveResult.AlreadyAdded
                AddSpeedDialContactUseCase.Result.InvalidContact ->
                    AddSpeedDialUiState.SaveResult.InvalidContact
            }
            _uiState.update { it.copy(saveResult = result) }
        }
    }

    fun onSaveResultConsumed() =
        _uiState.update { it.copy(saveResult = null) }
}
