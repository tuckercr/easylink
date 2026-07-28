package com.fangjet.launcher.data.pairing

import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fangjet.shared.FirestorePaths
import com.fangjet.shared.PairingCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** A pairing code currently being displayed, with its expiry for the countdown UI. */
data class ActivePairing(
    val linkId: String,
    val code: String,
    val expiresAtMillis: Long,
)

/**
 * Elder-side pairing: mints the 6-digit code a caregiver types into EasyLink Care.
 *
 * ## How pairing works
 *
 * 1. The launcher signs in anonymously — that uid *is* the elder's identity.
 * 2. It writes `links/{linkId}` (created once, [linkId] persisted locally) with a
 *    fresh [PairingCode] and expiry, plus a `pairingCodes/{code} → linkId` lookup
 *    doc so Care can find the link from just the code.
 * 3. Care redeems the code by adding its own uid to `caregiverUids` — an update
 *    the security rules only allow while a live, unexpired code is set.
 * 4. This screen watches `caregiverUids` grow to show "Connected!".
 *
 * Redemption is currently enforced by security rules alone; the production plan
 * is a `redeemPairing` Cloud Function for rate limiting (see PairingCode docs).
 */
@Singleton
class PairingRepository @Inject constructor(
    @param:Named("pairingPrefs") private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private const val TAG = "PairingRepository"
        private val KEY_LINK_ID = stringPreferencesKey("link_id")
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /**
     * Generates and publishes a fresh pairing code, creating the link document on
     * first use. Safe to call repeatedly — each call replaces the previous code.
     */
    suspend fun beginPairing(): ActivePairing {
        val uid = ensureSignedIn()
        val linkId = ensureLinkId()
        val code = PairingCode.generate()
        val expiresAt = System.currentTimeMillis() + PairingCode.VALIDITY_MS

        // Merge, never overwrite: a re-pair must not clobber caregiverUids.
        db
            .document(FirestorePaths.link(linkId))
            .set(
                mapOf(
                    "linkId" to linkId,
                    "elderUid" to uid,
                    "elderDisplayName" to (Build.MODEL ?: "EasyLink phone"),
                    "pairingCode" to code,
                    "pairingCodeExpiresAt" to expiresAt,
                    "createdAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()

        db
            .document(FirestorePaths.pairingCode(code))
            .set(mapOf("linkId" to linkId, "expiresAt" to expiresAt))
            .await()

        Log.d(TAG, "Pairing code published for link $linkId")
        return ActivePairing(linkId, code, expiresAt)
    }

    /**
     * Emits the caregiver count for [linkId] whenever it changes, so the UI can
     * flip from "waiting" to "connected" the moment Care redeems the code.
     */
    fun caregiverCount(linkId: String): Flow<Int> =
        callbackFlow {
            val registration = db
                .document(FirestorePaths.link(linkId))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "link listener error", error)
                        return@addSnapshotListener
                    }
                    @Suppress("UNCHECKED_CAST")
                    val uids = snapshot?.get("caregiverUids") as? List<String> ?: emptyList()
                    trySend(uids.size)
                }
            awaitClose { registration.remove() }
        }

    private suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        return auth
            .signInAnonymously()
            .await()
            .user
            ?.uid
            ?: error("Anonymous sign-in returned no user")
    }

    /** The launcher's stable link id — created once, reused for every re-pair. */
    private suspend fun ensureLinkId(): String {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        prefs[KEY_LINK_ID]?.let { return it }
        val fresh = UUID.randomUUID().toString()
        dataStore.edit { it[KEY_LINK_ID] = fresh }
        return fresh
    }
}
