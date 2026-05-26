package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import javax.inject.Inject

/**
 * Use Case: validate and save a medication, scheduling its alarms.
 *
 * Validation rules:
 *  - Name must not be blank
 *  - At least one reminder time must be set
 *  - At least one active day must be selected
 *
 * These rules live here — not in the DAO, not in the Fragment.
 */
class AddMedicationUseCase @Inject constructor(
    private val repository: MedicationRepository,
) {
    sealed class Result {
        data class Success(
            val medication: Medication,
        ) : Result()

        data class InvalidName(
            val message: String,
        ) : Result()

        data class NoReminderTimes(
            val message: String,
        ) : Result()

        data class NoActiveDays(
            val message: String,
        ) : Result()
    }

    suspend operator fun invoke(medication: Medication): Result {
        if (medication.name.isBlank()) {
            return Result.InvalidName("Medication name cannot be empty")
        }
        if (medication.reminderTimes.isEmpty()) {
            return Result.NoReminderTimes("Please add at least one reminder time")
        }
        if (medication.activeDays.isEmpty()) {
            return Result.NoActiveDays("Please select at least one day")
        }
        val saved = repository.saveMedication(medication)
        return Result.Success(saved)
    }
}
