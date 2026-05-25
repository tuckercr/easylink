package com.tuckercr.ezlauncher.domain.repository

import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.model.SosResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all SOS-related data and actions.
 *
 * The domain layer defines WHAT is needed; the data layer defines HOW.
 * Nothing in this interface imports Room, SmsManager, or LocationClient —
 * those are implementation details the ViewModel must never see.
 */
interface SosRepository {

    /** Live stream of all configured emergency contacts, ordered by isPrimary desc. */
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>

    /** Persist a new or updated contact. Returns the saved contact with its ID. */
    suspend fun saveEmergencyContact(contact: EmergencyContact): EmergencyContact

    /** Remove a contact permanently. */
    suspend fun deleteEmergencyContact(contact: EmergencyContact)

    /**
     * Fire the SOS: get GPS coordinates, call the primary contact,
     * and send location SMS to all contacts. Returns a [SosResult]
     * describing exactly what succeeded and what didn't.
     */
    suspend fun triggerSos(): SosResult
}
