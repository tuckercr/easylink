package com.tuckercr.ezlauncher.domain.model

/**
 * Lifecycle state of the Text-to-Speech engine.
 *
 * Android's [TextToSpeech] initialises asynchronously — there's a window
 * between construction and the [onInit] callback where the engine isn't
 * ready. Modelling this as a sealed class (rather than a Boolean flag)
 * means the ViewModel and UI handle every state explicitly with no
 * null-checks or race conditions.
 */
sealed class TtsState {

    /** Engine is still loading. UI should disable long-press affordances. */
    data object Initializing : TtsState()

    /**
     * Engine is ready. Long-press on any labelled button will trigger speech.
     *
     * @param isSpeaking  True while an utterance is actively playing.
     */
    data class Ready(
        val isSpeaking: Boolean = false,
    ) : TtsState()

    /**
     * Engine could not be initialised — TTS unavailable on this device.
     * UI should hide TTS affordances gracefully rather than crashing.
     *
     * @param reason  Human-readable description for logging/debug overlay.
     */
    data class Unavailable(
        val reason: String,
    ) : TtsState()
}
