package com.tuckercr.ezlauncher.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tuckercr.ezlauncher.data.local.EmergencyContactDao
import com.tuckercr.ezlauncher.data.local.EmergencyContactEntity
import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.model.SosResult
import com.tuckercr.ezlauncher.domain.repository.SosRepository
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
    @ApplicationContext private val context: Context,
    private val dao: EmergencyContactDao,
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

        // 2. Build the SMS message
        val message = buildSmsMessage(locationShared = locationLink != null, locationLink)

        // 3. Send SMS to ALL contacts
        val smsCount = sendSmsToAll(contacts, message)

        // 4. Call the primary contact (or fall back to the first one)
        val primaryContact = contacts.firstOrNull { it.isPrimary } ?: contacts.first()
        val callPlaced = placeCall(primaryContact.phoneNumber)

        if (!callPlaced) {
            Log.e(TAG, "Failed to place emergency call to ${primaryContact.name}")
        }

        return SosResult.Success(
            calledContact = if (callPlaced) primaryContact else null,
            smsRecipients = smsCount,
            locationShared = locationLink != null,
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
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
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
     */
    @Suppress("DEPRECATION") // SmsManager.getDefault() fine for API 26+
    private fun sendSmsToAll(contacts: List<EmergencyContact>, message: String): Int {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) {
            Log.e(TAG, "SEND_SMS permission not granted")
            return 0
        }

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
     * Initiates a phone call to [phoneNumber].
     * Uses ACTION_CALL (requires CALL_PHONE permission) so the call
     * starts immediately without the dialer confirmation screen.
     */
    private fun placeCall(phoneNumber: String): Boolean {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            Log.e(TAG, "CALL_PHONE permission not granted")
            return false
        }

        return runCatching {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrElse { e ->
            Log.e(TAG, "Failed to place call", e)
            false
        }
    }
}
