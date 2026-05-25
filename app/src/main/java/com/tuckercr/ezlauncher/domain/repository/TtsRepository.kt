package com.tuckercr.ezlauncher.domain.repository

import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.model.TtsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for Text-to-Speech capability.
 *
 * The key design decisions:
 *
 * 1. [engineState] is a [StateFlow] (not a one-shot) because TTS
 *    initialisation is async. Any screen can observe this and react
 *    when the engine becomes ready or unavailable.
 *
 * 2. [speak] is a plain function (not suspend). TTS speech is fire-and-forget
 *    — the engine queues it and plays asynchronously. Making it suspend would
 *    imply we block until speaking is done, which we never want.
 *
 * 3. Preferences are separate from engine state — they can be updated
 *    independently and don't require engine restart (setSpeechRate / setPitch
 *    can be called on a live engine).
 */
interface TtsRepository {

    /** Current state of the TTS engine — Initializing, Ready, or Unavailable. */
    val engineState: StateFlow<TtsState>

    /** Live stream of user-configured TTS preferences from DataStore. */
    fun getPreferences(): Flow<TtsPreferences>

    /**
     * Speak [text] immediately, interrupting any current utterance.
     * No-ops silently if the engine is not [TtsState.Ready] or TTS is disabled.
     */
    fun speak(text: String)

    /** Stop any current speech immediately. */
    fun stop()

    /** Persist updated [preferences] to DataStore. */
    suspend fun updatePreferences(preferences: TtsPreferences)
}
