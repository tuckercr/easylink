package com.fangjet.care.data.config

import android.util.Log
import com.fangjet.shared.FirestorePaths
import com.fangjet.shared.model.ContactConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Care's window onto the elder's remotely-managed configuration.
 *
 * Writes go to `links/{linkId}/config/current` — the caregiver-owned document
 * the launcher applies on its side (see the launcher's ConfigSyncManager). Every
 * write stamps `updatedAt`/`updatedBy` so the launcher can apply last-write-wins
 * and the audit trail shows who changed what.
 */
@Singleton
class CareConfigRepository
    @Inject
    constructor() {
        companion object {
            private const val TAG = "CareConfigRepository"
        }

        private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
        private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

        /** The elder's display name from the link document ("their phone" until profiles exist). */
        fun observeElderName(linkId: String): Flow<String> =
            callbackFlow {
                val reg = db
                    .document(FirestorePaths.link(linkId))
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            Log.w(TAG, "link listener error", error)
                            return@addSnapshotListener
                        }
                        trySend(snap?.getString("elderDisplayName") ?: "their phone")
                    }
                awaitClose { reg.remove() }
            }

        /** Live emergency-contact list from config/current (empty until first save). */
        fun observeEmergencyContacts(linkId: String): Flow<List<ContactConfig>> =
            callbackFlow {
                val reg = db
                    .document(FirestorePaths.config(linkId))
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            Log.w(TAG, "config listener error", error)
                            return@addSnapshotListener
                        }
                        trySend(parseContacts(snap?.get("emergencyContacts")))
                    }
                awaitClose { reg.remove() }
            }

        /** Replaces the emergency-contact list. The whole list is the unit of edit. */
        suspend fun saveEmergencyContacts(
            linkId: String,
            contacts: List<ContactConfig>,
        ) {
            db
                .document(FirestorePaths.config(linkId))
                .set(
                    mapOf(
                        "emergencyContacts" to contacts.map { c ->
                            mapOf(
                                "id" to c.id,
                                "name" to c.name,
                                "phoneNumber" to c.phoneNumber,
                                "relationship" to c.relationship,
                                "position" to c.position,
                            )
                        },
                        "updatedAt" to System.currentTimeMillis(),
                        "updatedBy" to (auth.currentUser?.uid ?: ""),
                    ),
                    SetOptions.merge(),
                ).await()
            Log.d(TAG, "Saved ${contacts.size} emergency contacts")
        }

        private fun parseContacts(raw: Any?): List<ContactConfig> {
            val list = raw as? List<*> ?: return emptyList()
            return list
                .mapNotNull { item ->
                    val m = item as? Map<*, *> ?: return@mapNotNull null
                    ContactConfig(
                        id = m["id"] as? String ?: "",
                        name = m["name"] as? String ?: return@mapNotNull null,
                        phoneNumber = m["phoneNumber"] as? String ?: "",
                        relationship = m["relationship"] as? String ?: "",
                        position = (m["position"] as? Number)?.toInt() ?: 0,
                    )
                }.sortedBy { it.position }
        }
    }
