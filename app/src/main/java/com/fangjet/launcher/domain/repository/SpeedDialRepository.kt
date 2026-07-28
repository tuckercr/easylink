package com.fangjet.launcher.domain.repository

import com.fangjet.launcher.domain.model.DeviceContact
import com.fangjet.launcher.domain.model.SpeedDialContact
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the Speed Dial feature.
 *
 * All reads are [Flow]-based so the UI reacts to changes immediately
 * (e.g. when the user reorders tiles or adds/removes a contact).
 */
interface SpeedDialRepository {

    /** Ordered list of pinned contacts — emits on every insert, delete, or reorder. */
    fun getSpeedDialContacts(): Flow<List<SpeedDialContact>>

    /**
     * Add a device contact to the Speed Dial screen.
     * The new contact is appended at the end of the current list.
     */
    suspend fun addContact(contact: DeviceContact)

    /** One-shot lookup of a pinned contact by id — used by the Edit Person screen. */
    suspend fun getContact(id: Long): SpeedDialContact?

    /** Persist edits (name, number, photo) to an existing pinned contact. */
    suspend fun updateContact(contact: SpeedDialContact)

    /** Remove a pinned contact. */
    suspend fun removeContact(contact: SpeedDialContact)

    /**
     * Persist a new display order for all pinned contacts.
     * Called after the user drags to reorder tiles.
     *
     * @param orderedIds Contact IDs in the desired display order.
     */
    suspend fun reorderContacts(orderedIds: List<Long>)

    /**
     * Search the device Contacts database for contacts matching [query].
     * Returns all matching results sorted by name.
     * Requires [android.Manifest.permission.READ_CONTACTS].
     */
    suspend fun searchDeviceContacts(query: String): List<DeviceContact>
}
