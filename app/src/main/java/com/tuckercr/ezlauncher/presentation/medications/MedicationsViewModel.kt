package com.tuckercr.ezlauncher.presentation.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.ReminderAction
import com.tuckercr.ezlauncher.domain.model.TodayReminder
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Medications screen (today's schedule).
 *
 * The UI state is a simple transformation of [MedicationRepository.getTodayReminders],
 * which is itself a combined Flow of active medications and today's logs. Any
 * change to either — e.g. the user tapping TAKEN on a notification — will push
 * a new [MedicationsUiState.Success] automatically.
 */
@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val repository: MedicationRepository,
) : ViewModel() {

    val uiState: StateFlow<MedicationsUiState> = repository
        .getTodayReminders()
        .map<_, MedicationsUiState> { reminders ->
            MedicationsUiState.Success(reminders)
        }
        .catch { e ->
            emit(MedicationsUiState.Error(e.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MedicationsUiState.Loading,
        )

    fun markTaken(reminder: TodayReminder) {
        viewModelScope.launch {
            repository.logReminderAction(
                medicationId  = reminder.medication.id,
                scheduledTime = reminder.scheduledAt,
                action        = ReminderAction.TAKEN,
            )
        }
    }

    fun snooze(reminder: TodayReminder) {
        viewModelScope.launch {
            repository.logReminderAction(
                medicationId  = reminder.medication.id,
                scheduledTime = reminder.scheduledAt,
                action        = ReminderAction.SNOOZED,
            )
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch { repository.deleteMedication(medication) }
    }
}
