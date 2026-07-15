package com.fangjet.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.fangjet.launcher.data.local.EmergencyContactDao
import com.fangjet.launcher.data.local.EmergencyContactEntity
import com.fangjet.launcher.data.permissions.PermissionChecker
import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.model.SosResult
import com.fangjet.launcher.domain.repository.SosRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "SosRepository"
private const val LOCATION_TIMEOUT_MS = 8_000L // don't delay SOS more than 8 seconds for GPS

/** How the emergency call was (or wasn't) started. */
private enum class CallOutcome {
    /** Placed directly via ACTION_CALL (CALL_PHONE granted). */
    DIRECT,

    /** CALL_PHONE was missing — opened the dialer pre-filled so the user can tap call. */
    DIALER,

    /** Couldn't start any call activity. */
    FAILED,
}

/**
 * Concrete implementation of [SosRepository].
 *
 * This class is the only place in the app that touches:
 *  - [SmsManager]            — sending location SMS to all contacts
 *  - [FusedLocationClient]   — getting current GPS coordinates
 *  - [Intent.ACTION_CALL]    — initiating the emergency phone call
 *  - [EmergencyContactDao]   — reading/writing contacts from Room
 *
 * Everything above this class is pure Kotlin. That's the point.
 */
@Singleton
class SosRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: EmergencyContactDao,
    private val permissions: PermissionChecker,
) : SosRepository {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // ── Contacts CRUD ─────────────────────────────────────────────────────────

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveEmergencyContact(contact: EmergencyContact): EmergencyContact {
        val entity = EmergencyContactEntity.fromDomain(contact)
        val newId = dao.upsert(entity)
        return contact.copy(id = newId)
    }

    override suspend fun deleteEmergencyContact(contact: EmergencyContact) {
        dao.delete(EmergencyContactEntity.fromDomain(contact))
    }

    // ── SOS trigger ───────────────────────────────────────────────────────────

    override suspend fun triggerSos(): SosResult {
        val contacts = dao.getAll().map { it.toDomain() }

        if (contacts.isEmpty()) {
            return SosResult.NoContactsConfigured
        }

        // 1. Try to get GPS location — but don't block the SOS on it
        val locationLink = fetchLocationLink()

        // 2. Text all contacts (only if we're allowed to)
        val smsPermitted = permissions.hasSendSms()
        val smsCount = if (smsPermitted) {
            val message = buildSmsMessage(locationShared = locationLink != null, locationLink)
            sendSmsToAll(contacts, message)
        } else {
            Log.e(TAG, "SEND_SMS permission not granted — SOS cannot text contacts")
            0
        }

        // 3. Call the primary contact (or fall back to the first one)
        val primaryContact = contacts.firstOrNull { it.isPrimary } ?: contacts.first()
        val callOutcome = placeCall(primaryContact.phoneNumber)

        return resolveSosResult(
            smsPermitted = smsPermitted,
            smsSent = smsCount,
            callPlaced = callOutcome != CallOutcome.FAILED,
            locationShared = locationLink != null,
            calledContact = primaryContact,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Requests a fresh high-accuracy location with a hard timeout.
     * Returns a Google Maps link string on success, null if unavailable.
     *
     * Uses [suspendCancellableCoroutine] to bridge the GMS Task API
     * into a coroutine — a common and important Android pattern.
     */
    private suspend fun fetchLocationLink(): String? {
        if (!permissions.hasFineLocation()) {
            Log.w(TAG, "Location permission not granted — sending SOS without GPS")
            return null
        }

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }

                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        val link = location?.let {
                            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                        }
                        continuation.resume(link)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Location fetch failed", e)
                        continuation.resume(null)
                    }
            }
        }
    }

    private fun buildSmsMessage(
        locationShared: Boolean,
        locationLink: String?,
    ): String {
        val base = "EMERGENCY ALERT: This person needs immediate help!"
        return if (locationShared && locationLink != null) {
            "$base\n\nCurrent location:\n$locationLink"
        } else {
            "$base\n\n(Location unavailable — please call them directly)"
        }
    }

    /**
     * Sends the SOS SMS to every configured contact.
     * Uses multipart send for messages longer than 160 characters.
     * Returns the number of contacts messaged successfully.
     *
     * Caller must confirm SEND_SMS is granted before calling.
     */
    @Suppress("DEPRECATION") // SmsManager.getDefault() fine for API 26+
    private fun sendSmsToAll(contacts: List<EmergencyContact>, message: String): Int {
        val smsManager = context.getSystemService(SmsManager::class.java)
        var successCount = 0

        contacts.forEach { contact ->
            runCatching {
                val parts = smsManager.divideMessage(message)
                if (parts.size == 1) {
                    smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                } else {
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber,
                        null,
                        parts,
                        null,
                        null,
                    )
                }
                successCount++
                Log.d(TAG, "SOS SMS sent to ${contact.name}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to send SMS to ${contact.name}", e)
            }
        }

        return successCount
    }

    /**
     * Starts the emergency call to [phoneNumber].
     *
     * With CALL_PHONE granted, uses ACTION_CALL to dial immediately. Without it,
     * falls back to ACTION_DIAL — the dialer opens pre-filled so the user (or a
     * bystander) only has to tap the call button, rather than the call silently
     * failing.
     */
    private fun placeCall(phoneNumber: String): CallOutcome {
        val direct = permissions.hasCallPhone()
        val action = if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL
        if (!direct) {
            Log.w(TAG, "CALL_PHONE not granted — opening the dialer instead of calling directly")
        }
        return runCatching {
            val intent = Intent(action, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (direct) CallOutcome.DIRECT else CallOutcome.DIALER
        }.getOrElse { e ->
            Log.e(TAG, "Failed to start emergency call", e)
            CallOutcome.FAILED
        }
    }
}

/**
 * Pure decision: given what actually happened during an SOS dispatch, what
 * result should the user see? Extracted so it can be unit-tested without any
 * Android framework dependencies.
 */
internal fun resolveSosResult(
    smsPermitted: Boolean,
    smsSent: Int,
    callPlaced: Boolean,
    locationShared: Boolean,
    calledContact: EmergencyContact?,
): SosResult {
    // Nothing reached the contacts at all.
    if (smsSent == 0 && !callPlaced) {
        val reason = if (!smsPermitted) {
            "Turn on text and phone permissions so SOS can reach your contacts."
        } else {
            "SOS couldn't send a text or start a call. Please call for help directly."
        }
        return SosResult.Failure(reason)
    }

    // The call went out, but texts were blocked by a missing SMS permission.
    if (!smsPermitted) {
        return SosResult.PartialSuccess(
            smsRecipients = 0,
            reason = "Couldn't text your contacts (SMS permission is off). A call was started.",
        )
    }

    return SosResult.Success(
        calledContact = if (callPlaced) calledContact else null,
        smsRecipients = smsSent,
        locationShared = locationShared,
    )
}
