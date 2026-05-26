package com.tuckercr.ezlauncher.presentation.inbox

import com.tuckercr.ezlauncher.domain.model.InboxItem

/**
 * UI state for the Inbox screen.
 *
 *  - [Loading]  — initial query not yet complete; show skeleton/spinner
 *  - [Empty]    — no calls or messages found (permissions denied or no history)
 *  - [Success]  — combined sorted list of calls and message threads
 *  - [Error]    — unexpected read failure
 *
 * [Success.missedCallCount] drives the badge on the Inbox navigation button
 * so the user can see at a glance how many missed calls await them.
 */
sealed interface InboxUiState {

    data object Loading : InboxUiState

    data object Empty : InboxUiState

    data class Success(
        val items: List<InboxItem>,
        val missedCallCount: Int = items.count {
            it is InboxItem.Call && it.isMissed
        },
        val unreadMessageCount: Int = items.count {
            it is InboxItem.Message && !it.isRead
        },
    ) : InboxUiState

    data class Error(
        val message: String,
    ) : InboxUiState
}
