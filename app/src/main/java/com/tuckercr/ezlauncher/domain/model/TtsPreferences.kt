package com.tuckercr.ezlauncher.domain.model

/**
 * User-configurable TTS preferences, persisted across app restarts via DataStore.
 *
 * Kept as a simple value class so it can be compared with == and used
 * directly in [StateFlow] without custom equals/hashCode.
 *
 * Speech rate and pitch match Android [TextToSpeech] API ranges:
 *   - rate:  0.1 (very slow) → 4.0 (very fast), normal = 1.0
 *   - pitch: 0.1 (very low)  → 2.0 (very high),  normal = 1.0
 *
 * For this app's elderly/vision-impaired audience, the meaningful range
 * is 0.5–1.5 for both — extremes are hard to understand.
 */
data class TtsPreferences(
    val isEnabled: Boolean = true,
    val speechRate: Float = 0.85f, // slightly slower than default — easier to follow
    val pitch: Float = 1.0f,
) {
    companion object {
        val Default = TtsPreferences()

        // Clamped ranges exposed to the settings UI
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 1.5f
        const val MIN_PITCH = 0.7f
        const val MAX_PITCH = 1.3f
    }
}
