package com.tuckercr.ezlauncher.presentation.fall

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.data.fall.FallDetectionNotificationHelper
import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.usecase.GetEmergencyContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class FallAlertUiState(
    val secondsRemaining: Int = COUNTDOWN_SECONDS,
    val primaryContact: EmergencyContact? = null,
    val callingNow: Boolean = false,
    val dismissed: Boolean = false,
)

private const val COUNTDOWN_SECONDS = 30

@HiltViewModel
class FallAlertViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getEmergencyContacts: GetEmergencyContactsUseCase,
    private val notifHelper: FallDetectionNotificationHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(FallAlertUiState())
    val state: StateFlow<FallAlertUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadPrimaryContact()
        startCountdown()
    }

    private fun loadPrimaryContact() {
        viewModelScope.launch {
            val contacts = getEmergencyContacts().first()
            val primary = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
            _state.value = _state.value.copy(primaryContact = primary)
        }
    }

    private fun startCountdown() {
        countdownJob = viewModelScope.launch {
            var secs = COUNTDOWN_SECONDS
            while (secs > 0) {
                delay(1_000.milliseconds)
                secs--
                _state.value = _state.value.copy(secondsRemaining = secs)
            }
            // Countdown reached zero → auto-call
            callNow()
        }
    }

    /** User tapped "I'm OK" — cancel the countdown and dismiss. */
    fun dismiss() {
        countdownJob?.cancel()
        notifHelper.dismissFallAlert()
        _state.value = _state.value.copy(dismissed = true)
    }

    /** User tapped "Call Now" or countdown reached zero. */
    fun callNow() {
        countdownJob?.cancel()
        val contact = _state.value.primaryContact ?: run {
            dismiss()
            return
        }
        notifHelper.dismissFallAlert()
        _state.value = _state.value.copy(callingNow = true, dismissed = true)

        val callIntent = Intent(Intent.ACTION_CALL, "tel:${contact.phoneNumber}".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
    }
}
