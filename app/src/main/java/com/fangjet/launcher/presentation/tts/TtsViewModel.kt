package com.fangjet.launcher.presentation.tts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.domain.model.TtsState
import com.fangjet.launcher.domain.repository.TtsRepository
import com.fangjet.launcher.domain.usecase.SpeakLabelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Shared ViewModel for TTS functionality.
 *
 * ## Why a shared ViewModel instead of injecting the use case directly?
 *
 * Each Fragment could inject [SpeakLabelUseCase] directly. But sharing
 * one ViewModel across all screens via `activityViewModels()` gives us:
 *
 *  1. One place to observe [TtsState] — if the engine is unavailable,
 *     every screen knows at once without each polling independently.
 *
 *  2. The [canSpeak] StateFlow is computed once and cached — no duplicate
 *     DataStore + engineState combination logic in every screen.
 *
 *  3. Stopping speech on navigation is trivial: the Fragment's
 *     `onDestroyView` calls `ttsViewModel.stop()` once.
 *
 * ## Scope
 * This ViewModel is scoped to the Activity (obtained via `activityViewModels()`),
 * so it outlives individual Fragment transactions. It is NOT a singleton —
 * it is recreated with the Activity on process restart, which is correct
 * because the TTS engine itself is re-initialised then too.
 */
@HiltViewModel
class TtsViewModel @Inject constructor(
    private val repository: TtsRepository,
    private val speakLabel: SpeakLabelUseCase,
) : ViewModel() {

    /**
     * True when TTS is ready AND the user has it enabled.
     * Fragments use this to decide whether to show long-press hints.
     */
    val canSpeak: StateFlow<Boolean> = combine(
        repository.engineState,
        repository.getPreferences(),
    ) { state, prefs ->
        state is TtsState.Ready && prefs.isEnabled
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    /**
     * True while an utterance is actively playing.
     * Use this to pulse or animate a speaker icon if desired.
     */
    val isSpeaking: StateFlow<Boolean> = repository.engineState
        .map { state ->
            state is TtsState.Ready && state.isSpeaking
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    // ── User intents ──────────────────────────────────────────────────────────

    /**
     * Speak [label] aloud. Call this from any Fragment's long-press listener.
     *
     * The label is sanitised by [SpeakLabelUseCase] before being handed
     * to the engine — no caller needs to worry about stripping emoji.
     */
    fun speak(label: String) = speakLabel(label)

    /** Stop current speech. Call from Fragment.onDestroyView(). */
    fun stop() = repository.stop()
}
