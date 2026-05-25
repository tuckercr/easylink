package com.tuckercr.ezlauncher.domain.model

import android.net.Uri

/**
 * A pinned favourite contact shown as a large photo tile on the Speed Dial screen.
 *
 * @param id             Room primary key (0 for unsaved contacts)
 * @param contactId      Android Contacts content-provider ID — used to open the
 *                       contact detail and to reload the photo if it changes
 * @param name           Display name shown beneath the photo tile
 * @param phoneNumber    The number dialled when the tile is tapped
 * @param photoUri       Content URI for the contact's thumbnail photo, or null if
 *                       the contact has no photo (UI shows initials placeholder)
 * @param displayOrder   0-based position in the grid; lower = earlier
 */
data class SpeedDialContact(
    val id: Long,
    val contactId: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: Uri?,
    val displayOrder: Int,
) {
    /** Initials to show in the placeholder when [photoUri] is null (max 2 chars). */
    val initials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    val isValid: Boolean
        get() = name.isNotBlank() && phoneNumber.isNotBlank()
}
