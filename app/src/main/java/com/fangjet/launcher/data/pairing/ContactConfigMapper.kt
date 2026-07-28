package com.fangjet.launcher.data.pairing

import com.fangjet.launcher.data.local.EmergencyContactEntity
import com.fangjet.shared.model.ContactConfig

/**
 * Maps the Firestore wire format ([ContactConfig]) onto the launcher's Room
 * rows. Pure functions, unit-tested — this is the seam where a schema mismatch
 * between the two apps would surface.
 */
object ContactConfigMapper {

    /**
     * Parses the raw `emergencyContacts` array from a config document.
     * Tolerant of junk: entries missing a name are dropped rather than crashing
     * the sync, because a bad write from an old Care build must never break SOS.
     */
    fun parse(raw: Any?): List<ContactConfig> {
        val list = raw as? List<*> ?: return emptyList()
        return list
            .mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                ContactConfig(
                    id = m["id"] as? String ?: "",
                    name = (m["name"] as? String)?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null,
                    phoneNumber = m["phoneNumber"] as? String ?: "",
                    relationship = m["relationship"] as? String ?: "",
                    position = (m["position"] as? Number)?.toInt() ?: 0,
                )
            }.sortedBy { it.position }
    }

    /**
     * Converts to Room entities. Position 0 is the primary contact (SOS calls
     * them; everyone else gets SMS only). Ids are fresh (0 = autogenerate) since
     * the caregiver list wholesale-replaces the local table.
     */
    fun toEntities(contacts: List<ContactConfig>): List<EmergencyContactEntity> =
        contacts
            .sortedBy { it.position }
            .mapIndexed { index, c ->
                EmergencyContactEntity(
                    id = 0,
                    name = c.name,
                    phoneNumber = c.phoneNumber,
                    isPrimary = index == 0,
                )
            }
}
