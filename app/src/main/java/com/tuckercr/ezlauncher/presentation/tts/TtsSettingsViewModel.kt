package com.tuckercr.ezlauncher.presentation.tts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.usecase.GetTtsPreferencesUseCase
import com.tuckercr.ezlauncher.domain.usecase.SpeakLabelUseCase
import com.tuckercr.ezlauncher.domain.usecase.UpdateTtsPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the TTS Settings screen.
 *
 * Exposes live [TtsPreferences] as a [StateFlow] so sliders and toggles
 * stay in sync with DataStore. Updates are written back immediately on
 * each slider change — no "Save" button required.
 *
 * Demonstrates an important UX pattern: settings that apply in real time
 * give elderly users instant feedback. They can adjust the speech rate
 * slider and hear the demo phrase update live, rather than guessing
 * whether their change had any effect.
 */
@HiltViewModel
class TtsSettingsViewModel @Inject constructor(
    getPreferences: GetTtsPreferencesUseCase,
    private val updatePreferences: UpdateTtsPreferencesUseCase,
    private val speakLabel: SpeakLabelUseCase,
) : ViewModel() {

    val preferences: StateFlow<TtsPreferences> = getPreferences()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = TtsPreferences.Default,
        )

    // ── User intents ──────────────────────────────────────────────────────────

    fun onEnabledToggled(enabled: Boolean) {
        save { it.copy(isEnabled = enabled) }
    }

    /** Called on every slider move — rate changes apply to the live engine immediately. */
    fun onSpeechRateChanged(rate: Float) {
        save { it.copy(speechRate = rate) }
    }

    fun onPitchChanged(pitch: Float) {
        save { it.copy(pitch = pitch) }
    }

    /**
     * Speak the demo phrase at the current rate and pitch.
     * Called when the user taps "Test voice" — lets them hear their changes
     * without navigating away from the settings screen.
     */
    fun onTestVoiceTapped() {
        speakLabel("Hello! Your phone is ready to help you.")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun save(transform: (TtsPreferences) -> TtsPreferences) {
        viewModelScope.launch {
            updatePreferences(transform(preferences.value))
        }
    }
}
