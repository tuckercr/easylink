package com.fangjet.launcher.domain.model

import androidx.annotation.StringRes
import com.fangjet.launcher.R

/**
 * How aggressively the fall-detection algorithm fires.
 *
 * Higher sensitivity → catches smaller falls, but more false positives.
 * Lower sensitivity → requires a harder impact, fewer false positives.
 */
enum class FallSensitivity(
    /** One-word label for the selector buttons. */
    @param:StringRes val shortLabelRes: Int,
    /** Longer description shown under the selected level. */
    @param:StringRes val labelRes: Int,
    /** Accelerometer magnitude (m/s²) below which free-fall is declared. */
    val freeFallThreshold: Float,
    /** Minimum free-fall duration (ms) before watching for impact. */
    val minFreeFallMs: Long,
    /** Impact magnitude (m/s²) that confirms a fall. */
    val impactThreshold: Float,
) {
    LOW(
        shortLabelRes = R.string.fall_sensitivity_low,
        labelRes = R.string.fall_sensitivity_low_desc,
        freeFallThreshold = 2.0f,
        minFreeFallMs = 100L,
        impactThreshold = 28.0f,
    ),
    MEDIUM(
        shortLabelRes = R.string.fall_sensitivity_medium,
        labelRes = R.string.fall_sensitivity_medium_desc,
        freeFallThreshold = 3.0f,
        minFreeFallMs = 80L,
        impactThreshold = 22.0f,
    ),
    HIGH(
        shortLabelRes = R.string.fall_sensitivity_high,
        labelRes = R.string.fall_sensitivity_high_desc,
        freeFallThreshold = 4.0f,
        minFreeFallMs = 60L,
        impactThreshold = 18.0f,
    ),
}
