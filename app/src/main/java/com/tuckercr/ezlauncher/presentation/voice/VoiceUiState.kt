package com.tuckercr.ezlauncher.presentation.voice

/**
 * All possible states of the voice-command overlay.
 *
 * State machine:
 *   Idle ──tap──► Listening ──results──► Heard ──parse──► Success | NotUnderstood
 *                          └──error──► Error
 *   Any non-Idle state ──dismiss──► Idle
 */
sealed class VoiceUiState {
    /** Overlay hidden; mic button visible on home screen. */
    data object Idle : VoiceUiState()

    /** SpeechRecognizer is active; animated mic shown. */
    data object Listening : VoiceUiState()

    /**
     * Results received, briefly shown before executing.
     * Shown for a single frame while the ViewModel processes the command.
     */
    data class Heard(val text: String) : VoiceUiState()

    /** Command recognised and execution started; auto-dismisses after 1.5 s. */
    data class Success(
        val commandText: String,
        val actionLabel: String,
    ) : VoiceUiState()

    /**
     * Speech was recognised but no pattern matched.
     * Shows examples and offers Retry / Cancel.
     */
    data class NotUnderstood(val text: String) : VoiceUiState()

    /** SpeechRecognizer reported an error. Shows message + Retry / Cancel. */
    data class Error(val message: String) : VoiceUiState()
}

/**
 * One-shot side effects emitted by [VoiceCommandViewModel] and consumed by
 * the UI layer (HomeScreen) to trigger in-app navigation or device actions.
 *
 * Using a SharedFlow rather than a StateFlow avoids re-delivering the same
 * effect on recomposition.
 */
sealed class VoiceEffect {
    data object NavigateToApps    : VoiceEffect()
    data object NavigateHome      : VoiceEffect()
    data object FlashlightOn      : VoiceEffect()
    data object FlashlightOff     : VoiceEffect()
    data object ToggleFlashlight  : VoiceEffect()
}
