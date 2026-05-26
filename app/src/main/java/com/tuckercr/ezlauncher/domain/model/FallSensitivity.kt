package com.tuckercr.ezlauncher.domain.model

/**
 * How aggressively the fall-detection algorithm fires.
 *
 * Higher sensitivity → catches smaller falls, but more false positives.
 * Lower sensitivity → requires a harder impact, fewer false positives.
 */
enum class FallSensitivity(
    val label: String,
    /** Accelerometer magnitude (m/s²) below which free-fall is declared. */
    val freeFallThreshold: Float,
    /** Minimum free-fall duration (ms) before watching for impact. */
    val minFreeFallMs: Long,
    /** Impact magnitude (m/s²) that confirms a fall. */
    val impactThreshold: Float,
) {
    LOW(
        label             = "Low (fewer alerts)",
        freeFallThreshold = 2.0f,
        minFreeFallMs     = 100L,
        impactThreshold   = 28.0f,
    ),
    MEDIUM(
        label             = "Medium (recommended)",
        freeFallThreshold = 3.0f,
        minFreeFallMs     = 80L,
        impactThreshold   = 22.0f,
    ),
    HIGH(
        label             = "High (most sensitive)",
        freeFallThreshold = 4.0f,
        minFreeFallMs     = 60L,
        impactThreshold   = 18.0f,
    ),
}
