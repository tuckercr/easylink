package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import javax.inject.Inject

/**
 * Use Case: persist updated TTS preferences.
 *
 * Clamps rate and pitch to safe ranges before saving — the UI sliders
 * enforce this too, but the use case is the authoritative guard.
 */
class UpdateTtsPreferencesUseCase @Inject constructor(
    private val repository: TtsRepository,
) {
    suspend operator fun invoke(preferences: TtsPreferences) {
        val safe = preferences.copy(
            speechRate = preferences.speechRate.coerceIn(TtsPreferences.MIN_RATE, TtsPreferences.MAX_RATE),
            pitch      = preferences.pitch.coerceIn(TtsPreferences.MIN_PITCH, TtsPreferences.MAX_PITCH),
        )
        repository.updatePreferences(safe)
    }
}
