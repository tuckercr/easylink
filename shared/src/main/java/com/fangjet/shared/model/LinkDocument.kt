package com.fangjet.shared.model

/**
 * The root document tying one elder's phone to one or more caregivers.
 *
 * Every field has a default so Firestore's POJO mapper can instantiate it
 * reflectively — do not remove the defaults, and do not add non-nullable fields
 * without one.
 *
 * @property elderUid Anonymous-auth uid of the launcher install. The elder never
 *   signs in with credentials; the anonymous account *is* their identity.
 * @property caregiverUids Every caregiver with access. An array (not a single
 *   uid) so siblings share one view without a second pairing.
 * @property pairingCode The active 6-digit code, or null once redeemed. Cleared
 *   by the `redeemPairing` Cloud Function so a code can never be reused.
 * @property pairingCodeExpiresAt Epoch millis. A code past this instant is
 *   rejected server-side even if it was never redeemed.
 */
data class LinkDocument(
    val linkId: String = "",
    val elderUid: String = "",
    val elderDisplayName: String = "",
    val caregiverUids: List<String> = emptyList(),
    val pairingCode: String? = null,
    val pairingCodeExpiresAt: Long = 0L,
    val createdAt: Long = 0L,
)
