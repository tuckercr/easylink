package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.ReminderAction
import com.fangjet.launcher.domain.repository.MedicationRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use Case: record that the user took, snoozed, or missed a reminder.
 *
 * Called from two places:
 *  1. [ReminderActionReceiver] — when the user taps "TAKEN" or "SNOOZE"
 *     in the notification (background context)
 *  2. [MedicationsViewModel] — when the user marks a dose taken from
 *     within the app's schedule view
 *
 * Keeping this in a use case means both callers share the same
 * validation and logging logic.
 */
class LogReminderActionUseCase @Inject constructor(
    private val repository: MedicationRepository,
) {
    suspend operator fun invoke(
        medicationId: Long,
        scheduledTime: LocalDateTime,
        action: ReminderAction,
    ) {
        repository.logReminderAction(
            medicationId = medicationId,
            scheduledTime = scheduledTime,
            action = action,
        )
    }
}
