package com.fangjet.launcher.presentation.speeddial

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.launcher.domain.model.SpeedDialContact
import com.fangjet.launcher.domain.repository.SpeedDialRepository
import com.fangjet.launcher.domain.usecase.GetSpeedDialContactsUseCase
import com.fangjet.launcher.domain.usecase.RemoveSpeedDialContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the Speed Dial screen.
 *
 * ## Call flow
 * Tapping a tile calls [onContactTapped], which:
 *  1. Sets [callInProgress] = true (disables tiles briefly to prevent double-dial)
 *  2. Fires `ACTION_CALL` intent — the system phone app takes over
 *  3. Resets [callInProgress] after 2 s (in case the user cancels immediately)
 *
 * ## Drag-to-reorder
 * The Fragment uses [ItemTouchHelper] to handle drag gestures. When a drag
 * completes, it calls [onContactsReordered] with the new ordered list.
 */
@HiltViewModel
class SpeedDialViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getContacts: GetSpeedDialContactsUseCase,
    private val removeContact: RemoveSpeedDialContactUseCase,
    private val repository: SpeedDialRepository,
) : ViewModel() {

    private val callInProgress = MutableStateFlow(false)

    val uiState: StateFlow<SpeedDialUiState> = combine(
        getContacts().catch { emit(emptyList()) },
        callInProgress,
    ) { contacts, inProgress ->
        when {
            contacts.isEmpty() -> SpeedDialUiState.Empty
            else -> SpeedDialUiState.Success(contacts, inProgress)
        }
    }.catch<SpeedDialUiState> { e ->
        emit(SpeedDialUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SpeedDialUiState.Loading,
    )

    // ── Actions ───────────────────────────────────────────────────────────

    fun onContactTapped(contact: SpeedDialContact) {
        if (callInProgress.value) return
        viewModelScope.launch {
            callInProgress.value = true
            try {
                val dialIntent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:${contact.phoneNumber}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            } finally {
                delay(2_000.milliseconds)
                callInProgress.value = false
            }
        }
    }

    fun onRemoveContact(contact: SpeedDialContact) {
        viewModelScope.launch {
            removeContact(contact)
        }
    }

    /**
     * Persist the new order after the user finishes a drag-reorder gesture.
     *
     * @param reordered The full contact list in the order now shown on screen.
     */
    fun onContactsReordered(reordered: List<SpeedDialContact>) {
        viewModelScope.launch {
            repository.reorderContacts(reordered.map { it.id })
        }
    }
}
