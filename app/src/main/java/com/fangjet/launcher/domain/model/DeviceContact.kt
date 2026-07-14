package com.fangjet.launcher.domain.model

import android.net.Uri

/**
 * A contact read from the Android Contacts content-provider.
 *
 * Used only in the "add favorite" picker — distinct from [SpeedDialContact]
 * which represents a contact already pinned to the Speed Dial screen.
 */
data class DeviceContact(
    val contactId: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: Uri?,
)
