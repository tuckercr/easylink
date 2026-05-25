package com.tuckercr.ezlauncher.domain.model

/**
 * Domain model for a single emergency contact.
 *
 * Pure Kotlin — no Room annotations, no Android imports.
 * The data layer maps between this and [EmergencyContactEntity].
 *
 * @param id          Stable identifier (Room primary key, 0 = unsaved)
 * @param name        Display name shown on the settings screen
 * @param phoneNumber E.164 or local format — SmsManager accepts both
 * @param isPrimary   The primary contact is called first; others receive SMS only
 */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false,
) {
    /** True if the number has enough digits to be a real phone number. */
    val isValid: Boolean
        get() = phoneNumber.filter { it.isDigit() }.length >= 7
}
