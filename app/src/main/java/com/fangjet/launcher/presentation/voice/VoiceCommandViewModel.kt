package com.fangjet.launcher.presentation.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.R
import com.fangjet.launcher.data.contacts.ContactsHelper
import com.fangjet.launcher.data.permissions.PermissionChecker
import com.fangjet.launcher.data.permissions.placePhoneCall
import com.fangjet.launcher.data.voice.VoiceCommandParser
import com.fangjet.launcher.domain.model.VoiceCommand
import com.fangjet.launcher.domain.repository.AppRepository
import com.fangjet.launcher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "VoiceCommandVM"
private const val AUTO_DISMISS_MS = 1_500L

/**
 * Manages the full voice-command lifecycle:
 *  1. Creates/holds a [SpeechRecognizer] (must live on the main thread).
 *  2. Drives [VoiceUiState] through the Idle → Listening → Heard → Success
 *     (or NotUnderstood / Error) state machine.
 *  3. Executes recognised commands (contacts lookup, app launch, intents).
 *  4. Emits [VoiceEffect] for in-app side-effects the UI handles (navigation,
 *     flashlight toggle) — these can't be handled here because they require a
 *     NavController or a ViewModel reference the UI already owns.
 *
 * Threading: [SpeechRecognizer] must be created and all methods called on the
 * main thread. Since [hiltViewModel] is always called from the Compose main
 * thread, [startListening] / [dismiss] are inherently on the right thread.
 */
@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: VoiceCommandParser,
    private val contactsHelper: ContactsHelper,
    private val appRepository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val permissions: PermissionChecker,
) : ViewModel() {

    // ── State & effects ───────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<VoiceEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<VoiceEffect> = _effects.asSharedFlow()

    // ── SpeechRecognizer (main-thread only) ───────────────────────────────────

    private var recognizer: SpeechRecognizer? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Start listening. Call only from main thread (Composable callback). */
    fun startListening() {
        if (_uiState.value is VoiceUiState.Listening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_not_available))
            return
        }

        ensureRecognizer()
        _uiState.value = VoiceUiState.Listening

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Shorter silence timeouts for snappier UX
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1_000L,
            )
        }
        recognizer?.startListening(intent)
        Log.d(TAG, "startListening()")
    }

    /** Cancel active recognition and return to Idle immediately. */
    fun dismiss() {
        recognizer?.cancel()
        _uiState.value = VoiceUiState.Idle
        Log.d(TAG, "dismiss()")
    }

    /** Retry — cancel any pending state and start fresh. */
    fun retry() {
        recognizer?.cancel()
        startListening()
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
        super.onCleared()
        Log.d(TAG, "onCleared()")
    }

    // ── SpeechRecognizer setup ────────────────────────────────────────────────

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(recognitionListener)
            Log.d(TAG, "SpeechRecognizer created")
        }
    }

    private val recognitionListener = object : RecognitionListener {

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()

            Log.d(TAG, "onResults: \"$text\"")

            if (text.isBlank()) {
                _uiState.value =
                    VoiceUiState.Error(context.getString(R.string.voice_error_no_match))
            } else {
                handleText(text)
            }
        }

        override fun onError(error: Int) {
            Log.w(TAG, "onError: $error")

            // Recognizer is unusable after BUSY — recreate on next call
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                recognizer?.destroy()
                recognizer = null
            }

            _uiState.value = VoiceUiState.Error(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> context.getString(R.string.voice_error_no_match)
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    -> context.getString(R.string.voice_error_network)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.voice_error_busy)
                    SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.voice_error_audio)
                    SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.voice_error_client)
                    else -> context.getString(R.string.voice_error_generic, error)
                },
            )
        }

        // ── Required stubs ─────────────────────────────────────────────────────
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partial: Bundle?) = Unit

        override fun onEvent(
            eventType: Int,
            params: Bundle?,
        ) = Unit
    }

    // ── Command handling ──────────────────────────────────────────────────────

    private fun handleText(raw: String) {
        _uiState.value = VoiceUiState.Heard(raw)

        viewModelScope.launch {
            val command = parser.parse(raw)
            Log.d(TAG, "Parsed: $command")

            when (command) {
                is VoiceCommand.Call -> executeCall(raw, command.name)
                is VoiceCommand.Text -> executeText(raw, command.name)
                is VoiceCommand.OpenApp -> executeOpenApp(raw, command.appName)
                is VoiceCommand.OpenCamera -> {
                    success(raw, context.getString(R.string.voice_command_camera))
                    context.startActivity(
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    autoDismiss()
                }

                is VoiceCommand.OpenAllApps -> {
                    success(raw, context.getString(R.string.voice_command_apps))
                    _effects.emit(VoiceEffect.NavigateToApps)
                    autoDismiss()
                }

                is VoiceCommand.FlashlightOn -> {
                    success(raw, context.getString(R.string.flashlight_on))
                    _effects.emit(VoiceEffect.FlashlightOn)
                    autoDismiss()
                }

                is VoiceCommand.FlashlightOff -> {
                    success(raw, context.getString(R.string.flashlight_off))
                    _effects.emit(VoiceEffect.FlashlightOff)
                    autoDismiss()
                }

                is VoiceCommand.ToggleFlashlight -> {
                    success(raw, context.getString(R.string.voice_command_flashlight_toggle))
                    _effects.emit(VoiceEffect.ToggleFlashlight)
                    autoDismiss()
                }

                is VoiceCommand.GoHome -> {
                    success(raw, context.getString(R.string.voice_command_home))
                    _effects.emit(VoiceEffect.NavigateHome)
                    autoDismiss()
                }

                is VoiceCommand.Unrecognized ->
                    _uiState.value = VoiceUiState.NotUnderstood(raw)
            }
        }
    }

    private suspend fun executeCall(
        raw: String,
        name: String,
    ) {
        if (!hasContactsPermission()) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_contacts_permission))
            return
        }
        val contact = contactsHelper.searchContacts(name).firstOrNull()
        if (contact == null) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_contact_not_found, name))
            return
        }
        success(raw, context.getString(R.string.voice_command_calling, contact.name))
        placePhoneCall(context, contact.phoneNumber, permissions)
        autoDismiss()
    }

    private suspend fun executeText(
        raw: String,
        name: String,
    ) {
        if (!hasContactsPermission()) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_contacts_permission))
            return
        }
        val contact = contactsHelper.searchContacts(name).firstOrNull()
        if (contact == null) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_contact_not_found, name))
            return
        }
        success(raw, context.getString(R.string.voice_command_texting, contact.name))
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "sms:${contact.phoneNumber}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        autoDismiss()
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    private suspend fun executeOpenApp(
        raw: String,
        appName: String,
    ) {
        val apps = appRepository.getInstalledApps().first()
        val app = apps.firstOrNull { it.label.lowercase().contains(appName.lowercase()) }

        if (app == null) {
            _uiState.value =
                VoiceUiState.Error(context.getString(R.string.voice_error_app_not_found, appName))
            return
        }
        success(raw, context.getString(R.string.voice_command_opening, app.label))
        launchAppUseCase(app.packageName)
        autoDismiss()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun success(
        commandText: String,
        label: String,
    ) {
        _uiState.value = VoiceUiState.Success(commandText, label)
    }

    private suspend fun autoDismiss() {
        delay(AUTO_DISMISS_MS.milliseconds)
        _uiState.value = VoiceUiState.Idle
    }
}
