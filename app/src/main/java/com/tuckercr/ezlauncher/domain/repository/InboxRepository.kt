package com.tuckercr.ezlauncher.domain.repository

import com.tuckercr.ezlauncher.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the unified Inbox (recent calls + SMS conversations).
 *
 * ## Reactivity
 * [getRecentItems] returns a [Flow] backed by [android.database.ContentObserver]
 * registrations on both the call log and SMS URIs. The Flow emits:
 *  1. Immediately on collection with the current snapshot.
 *  2. Whenever the call log or SMS database changes (new call, new SMS, etc.).
 *
 * ## Permissions required (requested at runtime before observing)
 *  - [android.Manifest.permission.READ_CALL_LOG]
 *  - [android.Manifest.permission.READ_SMS]
 *
 * ## Data contract
 * The list is:
 *  - Limited to [MAX_ITEMS] entries per source before merge.
 *  - Sorted by [InboxItem.timestamp] descending (newest first).
 *  - Deduplicated: one SMS row per thread (most recent message shown).
 */
interface InboxRepository {

    /** Live stream of the most recent calls and message threads, newest first. */
    fun getRecentItems(): Flow<List<InboxItem>>

    companion object {
        /** Maximum items fetched per source (calls / messages) before merge. */
        const val MAX_ITEMS_PER_SOURCE = 30
    }
}
