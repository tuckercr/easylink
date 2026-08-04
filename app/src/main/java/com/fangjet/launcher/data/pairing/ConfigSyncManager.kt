package com.fangjet.launcher.data.pairing

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fangjet.launcher.data.local.EmergencyContactDao
import com.fangjet.shared.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Applies caregiver-made configuration to the elder's phone.
 *
 * Listens to `links/{linkId}/config/current` (written by EasyLink Care) and
 * applies each new revision locally. Currently syncs **emergency contacts**;
 * medications and home-button config will ride the same pipe.
 *
 * ## Semantics
 * - The caregiver's list is authoritative: applying wholesale-replaces the local
 *   table. A local edit made after the last caregiver save survives only until
 *   the next caregiver save (documented one-way sync for the MVP).
 * - Each config revision (`updatedAt`) is applied exactly once, tracked in
 *   DataStore — restarting the app does not re-clobber local edits with an old
 *   revision.
 * - Does nothing until the phone has been paired (linkId exists) and the
 *   anonymous Firebase session is present.
 */
@Singleton
class ConfigSyncManager @Inject constructor(
    @param:Named("pairingPrefs") private val dataStore: DataStore<Preferences>,
    private val emergencyContactDao: EmergencyContactDao,
) {
    companion object {
        private const val TAG = "ConfigSyncManager"
        private val KEY_LINK_ID = stringPreferencesKey("link_id")
        private val KEY_APPLIED_AT = longPreferencesKey("config_applied_at")
    }

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Call once from Application.onCreate with an application-scoped scope. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[KEY_LINK_ID] }
                .distinctUntilChanged()
                .collectLatest { linkId ->
                    if (linkId == null) return@collectLatest
                    // Wait rather than bail: on a cold start Firebase may not have
                    // restored the persisted anonymous session yet, and since the
                    // linkId flow never re-emits, an early return here would leave
                    // sync dead for the entire process lifetime.
                    awaitSignedIn()
                    Log.d(TAG, "Watching config for link $linkId")
                    watchConfig(linkId)
                }
        }
    }

    /** Suspends until Firebase has a signed-in user (returns at once if it already does). */
    private suspend fun awaitSignedIn() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) return
        Log.d(TAG, "Paired but session not restored yet — waiting for sign-in")
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }.first { it }
    }

    private suspend fun watchConfig(linkId: String) {
        configSnapshots(linkId).collect { snapshot ->
            try {
                applyIfNew(snapshot)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply remote config", e)
            }
        }
    }

    private fun configSnapshots(linkId: String) =
        callbackFlow {
            val reg = db
                .document(FirestorePaths.config(linkId))
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w(TAG, "config listener error", error)
                        return@addSnapshotListener
                    }
                    if (snap != null && snap.exists()) trySend(snap)
                }
            awaitClose { reg.remove() }
        }

    private suspend fun applyIfNew(snapshot: DocumentSnapshot) {
        val updatedAt = snapshot.getLong("updatedAt") ?: 0L
        val appliedAt = dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[KEY_APPLIED_AT] ?: -1L
        if (updatedAt <= appliedAt) return

        val contacts = ContactConfigMapper.parse(snapshot.get("emergencyContacts"))
        emergencyContactDao.syncFromRemote(ContactConfigMapper.toEntities(contacts))
        dataStore.edit { it[KEY_APPLIED_AT] = updatedAt }
        Log.i(TAG, "Applied caregiver config rev $updatedAt: ${contacts.size} emergency contacts")
    }
}
