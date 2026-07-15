package com.fangjet.launcher.presentation.tts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.TtsPreferences
import com.fangjet.launcher.domain.usecase.GetTtsPreferencesUseCase
import com.fangjet.launcher.domain.usecase.SpeakLabelUseCase
import com.fangjet.launcher.domain.usecase.UpdateTtsPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

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
    @param:ApplicationContext private val context: Context,
    getPreferences: GetTtsPreferencesUseCase,
    private val updatePreferences: UpdateTtsPreferencesUseCase,
    private val speakLabel: SpeakLabelUseCase,
) : ViewModel() {

    val preferences: StateFlow<TtsPreferences> = getPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
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
        speakLabel(context.getString(R.string.tts_test_message))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun save(transform: (TtsPreferences) -> TtsPreferences) {
        viewModelScope.launch {
            updatePreferences(transform(preferences.value))
        }
    }
}
