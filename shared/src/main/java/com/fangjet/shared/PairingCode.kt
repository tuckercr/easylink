package com.fangjet.shared

import kotlin.random.Random

/**
 * The 6-digit code that links a launcher install to a caregiver account.
 *
 * Codes are generated on the elder's phone, read aloud or over the shoulder, and
 * typed into Care once. They are short-lived and single-use: redemption happens in
 * a Cloud Function that clears the code, so a code that leaks is useless after the
 * first redemption or [VALIDITY_MS], whichever comes first.
 *
 * Six digits is only ~1M possibilities, which is not much on its own — the
 * server-side rate limit on `redeemPairing` is what makes that safe, not the
 * length. Do not lengthen the code instead of rate limiting; a longer code is
 * harder for the people who need this product and no real defence.
 */
object PairingCode {
    const val LENGTH = 6

    /** Codes expire 15 minutes after generation. */
    const val VALIDITY_MS = 15 * 60 * 1_000L

    private val DIGITS = '0'..'9'

    /**
     * Generates a random code. Leading zeros are allowed and meaningful, so codes
     * are always [LENGTH] characters and must never be handled as an Int.
     */
    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) { append(DIGITS.random(random)) }
        }

    fun isValidFormat(code: String): Boolean = code.length == LENGTH && code.all { it.isDigit() }

    fun isExpired(
        expiresAt: Long,
        nowMillis: Long,
    ): Boolean = nowMillis >= expiresAt

    /** Formats for display as "123 456" — easier to read aloud than a solid run of digits. */
    fun formatForDisplay(code: String): String = if (code.length == LENGTH) "${code.take(3)} ${code.drop(3)}" else code
}
