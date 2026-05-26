package com.tuckercr.ezlauncher.presentation.inbox

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.domain.model.InboxItem
import com.tuckercr.ezlauncher.domain.usecase.GetRecentInboxItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Inbox screen.
 *
 * ## Data flow
 * [InboxRepository.getRecentItems] → [GetRecentInboxItemsUseCase] → [uiState]
 *
 * The Flow updates automatically on call-log and SMS changes — no manual
 * refresh button needed.
 *
 * ## Call-back action
 * [onCallTapped] fires `ACTION_CALL` directly (requires CALL_PHONE permission,
 * which the Fragment requests before this screen is shown). No countdown is
 * used here — this is a one-tap call-back, not a panic SOS.
 *
 * ## Open message action
 * [onMessageTapped] fires an intent to open the default SMS app on the
 * correct thread. We let the system handle it rather than building a full
 * in-app messaging view — simplicity wins for elderly users.
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getRecentItems: GetRecentInboxItemsUseCase,
) : ViewModel() {

    val uiState: StateFlow<InboxUiState> = getRecentItems()
        .map { items ->
            if (items.isEmpty()) {
                InboxUiState.Empty
            } else {
                InboxUiState.Success(items)
            }
        }.catch { e -> emit(InboxUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState.Loading,
        )

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Dial back the number from a call-log entry.
     * Requires [android.Manifest.permission.CALL_PHONE].
     */
    fun onCallTapped(call: InboxItem.Call) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:${call.phoneNumber}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open the SMS thread in the device's default messaging app.
     *
     * Uses the thread-specific URI so the app scrolls directly to the
     * conversation rather than showing the message list.
     */
    fun onMessageTapped(message: InboxItem.Message) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "smsto:${message.phoneNumber}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        // Prefer the default SMS app so threads open in the right conversation
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        if (defaultSmsPackage != null) {
            intent.`package` = defaultSmsPackage
        }
        context.startActivity(intent)
    }

    /** Dial a number shown in a message row (e.g. when user wants to call instead of text). */
    fun onCallFromMessage(message: InboxItem.Message) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:${message.phoneNumber}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
