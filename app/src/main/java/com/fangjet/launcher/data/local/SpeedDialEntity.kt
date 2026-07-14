package com.fangjet.launcher.data.local

import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fangjet.launcher.domain.model.SpeedDialContact

/**
 * Room entity for a pinned Speed Dial contact.
 *
 * [contactId] carries a UNIQUE index so the same device contact can only
 * appear once in the grid, even if [addContact] is called concurrently.
 */
@Entity(
    tableName = "speed_dial_contacts",
    indices = [Index(value = ["contactId"], unique = true)],
)
data class SpeedDialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Android Contacts content-provider ID. */
    val contactId: Long,
    val name: String,
    val phoneNumber: String,
    /** Stored as a string; null when the contact has no photo. */
    val photoUriString: String?,
    /** 0-based sort order in the grid. */
    val displayOrder: Int,
) {
    fun toDomain() =
        SpeedDialContact(
            id = id,
            contactId = contactId,
            name = name,
            phoneNumber = phoneNumber,
            photoUri = photoUriString?.toUri(),
            displayOrder = displayOrder,
        )

    companion object {
        fun fromDomain(contact: SpeedDialContact) =
            SpeedDialEntity(
                id = contact.id,
                contactId = contact.contactId,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                photoUriString = contact.photoUri?.toString(),
                displayOrder = contact.displayOrder,
            )
    }
}
