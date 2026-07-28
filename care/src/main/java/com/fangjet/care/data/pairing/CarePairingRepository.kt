package com.fangjet.care.data.pairing

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fangjet.shared.FirestorePaths
import com.fangjet.shared.PairingCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pairingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "care_pairing_prefs")

sealed class RedeemResult {
    /** Paired. [elderDisplayName] is best-effort (device model until profiles exist). */
    data class Success(
        val linkId: String,
        val elderDisplayName: String,
    ) : RedeemResult()

    data object InvalidCode : RedeemResult()

    data object Expired : RedeemResult()

    data object NetworkError : RedeemResult()
}

/**
 * Caregiver-side pairing: turns a typed 6-digit code into membership of a link.
 *
 * Flow: sign in (anonymously for now — real accounts come with the full Care
 * app), `get()` the `pairingCodes/{code}` lookup doc, then add our uid to the
 * link's `caregiverUids` and burn the code. The security rules only allow that
 * update while the link holds a live, unexpired code, so a stale or already-used
 * code fails server-side no matter what this client does.
 */
@Singleton
class CarePairingRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        companion object {
            private const val TAG = "CarePairingRepository"
            private val KEY_LINK_ID = stringPreferencesKey("link_id")
        }

        private val dataStore = context.pairingDataStore
        private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
        private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

        /** The redeemed linkId, or null before pairing. Drives the app's root routing. */
        val linkId: Flow<String?> = dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[KEY_LINK_ID] }

        suspend fun redeem(code: String): RedeemResult {
            if (!PairingCode.isValidFormat(code)) return RedeemResult.InvalidCode

            return try {
                val uid = ensureSignedIn()

                val claim = db.document(FirestorePaths.pairingCode(code)).get().await()
                if (!claim.exists()) return RedeemResult.InvalidCode
                val linkId = claim.getString("linkId") ?: return RedeemResult.InvalidCode
                val expiresAt = claim.getLong("expiresAt") ?: 0L
                if (PairingCode.isExpired(expiresAt, System.currentTimeMillis())) {
                    return RedeemResult.Expired
                }

                // Join the link and burn the code in one write; rules verify the
                // live code server-side. A second write cleans up the lookup doc.
                val linkRef = db.document(FirestorePaths.link(linkId))
                linkRef
                    .update(
                        mapOf(
                            "caregiverUids" to FieldValue.arrayUnion(uid),
                            "pairingCode" to null,
                        ),
                    ).await()
                runCatching { db.document(FirestorePaths.pairingCode(code)).delete().await() }

                dataStore.edit { it[KEY_LINK_ID] = linkId }

                // Now that we're a caregiver we may read the link document.
                val elderName = runCatching {
                    linkRef.get().await().getString("elderDisplayName")
                }.getOrNull() ?: "their phone"

                Log.d(TAG, "Paired to link $linkId")
                RedeemResult.Success(linkId, elderName)
            } catch (e: FirebaseFirestoreException) {
                // PERMISSION_DENIED = rules rejected the join: code already used,
                // cleared, or expired server-side.
                Log.w(TAG, "redeem failed: ${e.code}", e)
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    RedeemResult.Expired
                } else {
                    RedeemResult.NetworkError
                }
            } catch (e: Exception) {
                Log.w(TAG, "redeem failed", e)
                RedeemResult.NetworkError
            }
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
    }
