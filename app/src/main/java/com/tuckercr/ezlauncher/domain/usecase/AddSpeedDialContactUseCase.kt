package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.DeviceContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Pins a device contact to the Speed Dial screen.
 *
 * ## Validation
 *  - [Result.InvalidContact]  — name or phone number is blank
 *  - [Result.AlreadyAdded]    — this contactId is already pinned
 *  - [Result.Success]         — contact saved; the grid updates via the reactive Flow
 *
 * Duplicate detection uses a one-shot [first] snapshot of the current pinned
 * list so the caller doesn't have to manage a subscription.
 */
class AddSpeedDialContactUseCase @Inject constructor(
    private val repository: SpeedDialRepository,
) {
    sealed interface Result {
        data object Success : Result

        data object InvalidContact : Result

        data object AlreadyAdded : Result
    }

    suspend operator fun invoke(contact: DeviceContact): Result {
        if (contact.name.isBlank() || contact.phoneNumber.isBlank()) {
            return Result.InvalidContact
        }

        val existing = repository.getSpeedDialContacts().first()
        val isDuplicate = if (contact.contactId > 0) {
            // Device contact: dedup by contactId
            existing.any { it.contactId == contact.contactId }
        } else {
            // Manually entered (contactId = -1): dedup by normalised digits
            val digits = contact.phoneNumber.filter { it.isDigit() }
            existing.any { it.phoneNumber.filter { c -> c.isDigit() } == digits }
        }
        if (isDuplicate) return Result.AlreadyAdded

        repository.addContact(contact)
        return Result.Success
    }
}
