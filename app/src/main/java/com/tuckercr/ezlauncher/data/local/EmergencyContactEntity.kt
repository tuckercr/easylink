package com.tuckercr.ezlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tuckercr.ezlauncher.domain.model.EmergencyContact

/**
 * Room database entity for persisting emergency contacts.
 *
 * Lives in the data layer only. The domain layer never sees this class —
 * [toDomain] / [fromDomain] are the only crossing points.
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isPrimary: Boolean,
) {
    fun toDomain() = EmergencyContact(
        id          = id,
        name        = name,
        phoneNumber = phoneNumber,
        isPrimary   = isPrimary,
    )

    companion object {
        fun fromDomain(contact: EmergencyContact) = EmergencyContactEntity(
            id          = contact.id,
            name        = contact.name,
            phoneNumber = contact.phoneNumber,
            isPrimary   = contact.isPrimary,
        )
    }
}
