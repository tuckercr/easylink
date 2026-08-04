package com.fangjet.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fangjet.launcher.domain.model.EmergencyContact

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
    /**
     * Firestore document id for contacts synced from EasyLink Care; null for
     * contacts added locally. Sync matches on this so a caregiver edit updates
     * the existing row in place instead of churning primary keys (which would
     * reset LazyColumn item identity on every save).
     */
    val remoteId: String? = null,
) {
    fun toDomain() =
        EmergencyContact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            isPrimary = isPrimary,
        )

    companion object {
        fun fromDomain(contact: EmergencyContact) =
            EmergencyContactEntity(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                isPrimary = contact.isPrimary,
            )
    }
}
