package com.fangjet.shared

/**
 * Every Firestore path used by both apps, in one place.
 *
 * Paths are the one thing a typo can break silently — a misspelled collection
 * name creates a new empty collection instead of failing — so neither app should
 * ever build a path from a string literal.
 *
 * ## Document layout
 *
 * ```
 * links/{linkId}
 *   ├─ (fields)          pairingCode, elderUid, caregiverUids, …
 *   ├─ config/current    written by Care,     read by Launcher
 *   ├─ status/current    written by Launcher, read by Care
 *   └─ events/{eventId}  written by Launcher, read+acked by Care
 * ```
 *
 * The write direction of each subcollection is enforced by security rules; see
 * `firestore.rules`. Keeping config and status in separate documents means the
 * elder's phone can never clobber its own configuration, and a caregiver edit
 * can never race a status heartbeat.
 */
object FirestorePaths {
    const val LINKS = "links"

    /**
     * Short-lived code → linkId lookup used during pairing.
     *
     * The document ID *is* the 6-digit code, so Care can `get()` it directly
     * without a query (rules forbid listing this collection — the code itself
     * is the secret). Docs hold `{ linkId, expiresAt }` and are deleted on
     * redemption.
     */
    const val PAIRING_CODES = "pairingCodes"

    const val CONFIG = "config"
    const val STATUS = "status"
    const val EVENTS = "events"

    /** Both config and status are single documents; this is their fixed id. */
    const val CURRENT = "current"

    fun link(linkId: String) = "$LINKS/$linkId"

    fun config(linkId: String) = "${link(linkId)}/$CONFIG/$CURRENT"

    fun status(linkId: String) = "${link(linkId)}/$STATUS/$CURRENT"

    fun events(linkId: String) = "${link(linkId)}/$EVENTS"

    fun event(
        linkId: String,
        eventId: String,
    ) = "${events(linkId)}/$eventId"

    fun pairingCode(code: String) = "$PAIRING_CODES/$code"
}
