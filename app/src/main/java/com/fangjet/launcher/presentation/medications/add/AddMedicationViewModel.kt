package com.fangjet.launcher.presentation.medications.add

import android.Manifest
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.R
import com.fangjet.launcher.data.preferences.PermissionAskPreferences
import com.fangjet.launcher.domain.model.Medication
import com.fangjet.launcher.domain.model.MedicationColor
import com.fangjet.launcher.domain.repository.MedicationRepository
import com.fangjet.launcher.domain.usecase.AddMedicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for the Add/Edit Medication screen.
 *
 * When [SavedStateHandle] contains a non-zero "medicationId" arg the screen
 * is in edit mode: the existing medication is fetched and pre-populated into
 * the form. Saving calls the same [AddMedicationUseCase] (which calls upsert),
 * but with the original id so the existing row is overwritten.
 */
@HiltViewModel
class AddMedicationViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val addMedication: AddMedicationUseCase,
    private val repository: MedicationRepository,
    private val permissionAsks: PermissionAskPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** See [PermissionAskPreferences]: tells "never asked" from "permanently denied". */
    val notifPermissionRequested: StateFlow<Boolean> =
        permissionAsks
            .asked(Manifest.permission.POST_NOTIFICATIONS)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun markNotifPermissionRequested() {
        viewModelScope.launch { permissionAsks.markAsked(Manifest.permission.POST_NOTIFICATIONS) }
    }

    private val medicationId: Long =
        savedStateHandle.get<Long>("medicationId") ?: 0L

    private val _uiState = MutableStateFlow(AddMedicationUiState(medicationId = medicationId))
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

    init {
        if (medicationId != 0L) loadExistingMedication()
    }

    // ── Load for edit ─────────────────────────────────────────────────────

    private fun loadExistingMedication() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExisting = true) }
            val medication = repository.getMedicationById(medicationId)
            if (medication != null) {
                _uiState.update {
                    it.copy(
                        isLoadingExisting = false,
                        name = medication.name,
                        dosage = medication.dosage,
                        notes = medication.notes,
                        reminderTimes = medication.reminderTimes,
                        activeDays = medication.activeDays,
                        isActive = medication.isActive,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingExisting = false) }
            }
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value, nameError = null) }

    fun onDosageChanged(value: String) = _uiState.update { it.copy(dosage = value) }

    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }

    fun onActiveToggled(isActive: Boolean) = _uiState.update { it.copy(isActive = isActive) }

    fun onReminderTimeAdded(time: LocalTime) =
        _uiState.update { state ->
            if (time in state.reminderTimes) return@update state
            state.copy(
                reminderTimes = (state.reminderTimes + time).sortedBy { it.toSecondOfDay() },
                reminderTimesError = null,
            )
        }

    fun onReminderTimeRemoved(time: LocalTime) =
        _uiState.update { state ->
            state.copy(reminderTimes = state.reminderTimes - time)
        }

    fun onDayToggled(day: DayOfWeek) =
        _uiState.update { state ->
            val current = state.activeDays
            val updated = if (day in current) current - day else current + day
            state.copy(activeDays = updated, activeDaysError = null)
        }

    // ── Save (add or update) ──────────────────────────────────────────────

    /**
     * Runs the same checks AddMedicationUseCase enforces and surfaces the same
     * error messages, without saving. The screen calls this BEFORE asking for
     * the notification permission — otherwise an invalid form triggered the
     * system prompt (and the resume-chain behind it) and the user never saw
     * why nothing was saved.
     */
    fun validateForSave(): Boolean {
        val state = _uiState.value
        var valid = true
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = context.getString(R.string.error_name_required)) }
            valid = false
        }
        if (state.reminderTimes.isEmpty()) {
            _uiState.update {
                it.copy(reminderTimesError = context.getString(R.string.error_reminder_time_required))
            }
            valid = false
        }
        if (state.activeDays.isEmpty()) {
            _uiState.update {
                it.copy(activeDaysError = context.getString(R.string.error_active_day_required))
            }
            valid = false
        }
        return valid
    }

    fun onSaveTapped() {
        val state = _uiState.value
        if (state.isSaving || state.isLoadingExisting) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val medication = Medication(
                id = medicationId, // 0L → insert, non-zero → update (upsert)
                name = state.name.trim(),
                dosage = state.dosage.trim(),
                notes = state.notes.trim(),
                reminderTimes = state.reminderTimes,
                activeDays = state.activeDays,
                isActive = state.isActive,
                color = MedicationColor.entries.random(),
            )

            when (addMedication(medication)) {
                is AddMedicationUseCase.Result.Success ->
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }

                is AddMedicationUseCase.Result.InvalidName ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            nameError = context.getString(R.string.error_name_required),
                        )
                    }

                is AddMedicationUseCase.Result.NoReminderTimes ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            reminderTimesError = context.getString(R.string.error_reminder_time_required),
                        )
                    }

                is AddMedicationUseCase.Result.NoActiveDays ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            activeDaysError = context.getString(R.string.error_active_day_required),
                        )
                    }
            }
        }
    }
}
