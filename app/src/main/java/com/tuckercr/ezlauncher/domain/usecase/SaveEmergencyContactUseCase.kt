package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import javax.inject.Inject

/**
 * Use Case: validate and save an emergency contact.
 *
 * Validation lives here — not in the Fragment, not in the ViewModel,
 * not in the repository. The use case is the single place that defines
 * what a valid emergency contact looks like.
 */
class SaveEmergencyContactUseCase @Inject constructor(
    private val repository: SosRepository,
) {
    sealed class Result {
        data class Success(val contact: EmergencyContact) : Result()
        data class InvalidPhone(val message: String) : Result()
        data class InvalidName(val message: String) : Result()
    }

    suspend operator fun invoke(contact: EmergencyContact): Result {
        if (contact.name.isBlank()) {
            return Result.InvalidName("Name cannot be empty")
        }
        if (!contact.isValid) {
            return Result.InvalidPhone("Enter a valid phone number (at least 7 digits)")
        }
        val saved = repository.saveEmergencyContact(contact)
        return Result.Success(saved)
    }
}
