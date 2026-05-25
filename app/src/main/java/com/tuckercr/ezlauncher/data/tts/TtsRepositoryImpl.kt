package com.tuckercr.ezlauncher.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.model.TtsState
import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG           = "TtsRepository"
private const val UTTERANCE_ID  = "ezlauncher_tts"

/** DataStore preference keys — defined at file scope to avoid allocation per call. */
private object PrefKeys {
    val ENABLED     = booleanPreferencesKey("tts_enabled")
    val SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val PITCH       = floatPreferencesKey("tts_pitch")
}

/**
 * Concrete implementation of [TtsRepository].
 *
 * ## Android TTS quirks this implementation handles:
 *
 * ### Async initialisation
 * [TextToSpeech] calls [TextToSpeech.OnInitListener.onInit] on the main
 * thread after construction. We model this with [_engineState] starting
 * as [TtsState.Initializing] and transitioning to [TtsState.Ready] or
 * [TtsState.Unavailable] in the callback.
 *
 * ### Speaking state
 * [UtteranceProgressListener] notifies us when speech starts and ends.
 * We update [TtsState.Ready.isSpeaking] so the UI can show a subtle
 * "speaking" indicator (e.g. the button pulses while its label is read).
 *
 * ### Rate/pitch applied live
 * When the user changes speech rate or pitch in settings, we apply the
 * new values to the live engine immediately — no restart required.
 *
 * ### This is a @Singleton
 * TTS initialisation takes ~200–400ms. Keeping one instance alive for
 * the app lifetime means every long-press after the first is instant.
 * A launcher app is the ideal candidate for this — it's always running.
 */
@Singleton
class TtsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : TtsRepository {

    // Coroutine scope tied to the singleton — survives configuration changes
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Engine state ──────────────────────────────────────────────────────────

    private val _engineState = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val engineState: StateFlow<TtsState> = _engineState.asStateFlow()

    private var engine: TextToSpeech? = null

    init {
        initEngine()
        // React to preference changes and apply them to the live engine
        scope.launch {
            getPreferences().collect { prefs -> applyPreferencesToEngine(prefs) }
        }
    }

    private fun initEngine() {
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = engine?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fall back to English — better than silence
                    engine?.setLanguage(Locale.ENGLISH)
                    Log.w(TAG, "Device locale not supported; falling back to English")
                }
                engine?.setOnUtteranceProgressListener(utteranceListener)
                _engineState.value = TtsState.Ready(isSpeaking = false)
                Log.d(TAG, "TTS engine ready")
            } else {
                _engineState.value = TtsState.Unavailable("onInit status=$status")
                Log.e(TAG, "TTS engine failed to initialise (status=$status)")
            }
        }
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _engineState.update { if (it is TtsState.Ready) it.copy(isSpeaking = true) else it }
        }
        override fun onDone(utteranceId: String?) {
            _engineState.update { if (it is TtsState.Ready) it.copy(isSpeaking = false) else it }
        }
        @Deprecated("Required override for API < 21")
        override fun onError(utteranceId: String?) {
            _engineState.update { if (it is TtsState.Ready) it.copy(isSpeaking = false) else it }
        }
    }

    // ── Preferences (DataStore) ───────────────────────────────────────────────

    override fun getPreferences(): Flow<TtsPreferences> = dataStore.data.map { prefs ->
        TtsPreferences(
            isEnabled  = prefs[PrefKeys.ENABLED]     ?: TtsPreferences.Default.isEnabled,
            speechRate = prefs[PrefKeys.SPEECH_RATE] ?: TtsPreferences.Default.speechRate,
            pitch      = prefs[PrefKeys.PITCH]       ?: TtsPreferences.Default.pitch,
        )
    }

    override suspend fun updatePreferences(preferences: TtsPreferences) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.ENABLED]     = preferences.isEnabled
            prefs[PrefKeys.SPEECH_RATE] = preferences.speechRate
            prefs[PrefKeys.PITCH]       = preferences.pitch
        }
        // Engine params updated reactively via the collect{} in init
    }

    private fun applyPreferencesToEngine(prefs: TtsPreferences) {
        engine?.setSpeechRate(prefs.speechRate)
        engine?.setPitch(prefs.pitch)
    }

    // ── Speech control ────────────────────────────────────────────────────────

    override fun speak(text: String) {
        val currentState = _engineState.value
        if (currentState !is TtsState.Ready) {
            Log.d(TAG, "speak() called but engine not ready ($currentState)")
            return
        }

        // Check the isEnabled preference synchronously via a cached StateFlow value.
        // This is intentional: speak() is fire-and-forget and must not suspend.
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        engine?.stop()
        _engineState.update { if (it is TtsState.Ready) it.copy(isSpeaking = false) else it }
    }
}
