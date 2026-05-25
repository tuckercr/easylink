package com.tuckercr.ezlauncher.data.repository

import android.content.Context
import android.provider.CallLog
import android.provider.Telephony
import com.tuckercr.ezlauncher.data.inbox.CallLogReader
import com.tuckercr.ezlauncher.data.inbox.SmsReader
import com.tuckercr.ezlauncher.data.inbox.observeUri
import com.tuckercr.ezlauncher.domain.model.InboxItem
import com.tuckercr.ezlauncher.domain.repository.InboxRepository
import com.tuckercr.ezlauncher.domain.repository.InboxRepository.Companion.MAX_ITEMS_PER_SOURCE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [InboxRepository].
 *
 * ## Reactivity design
 * Android's call log and SMS databases are ContentProviders — they expose
 * change notifications via [android.database.ContentObserver] but have no
 * native Flow support.
 *
 * We bridge this by:
 *  1. [observeUri] — converts a ContentObserver callback into a `callbackFlow<Unit>`
 *  2. On each emission (including the first) re-query the corresponding reader
 *  3. [combine] the two result flows so any change in either source emits a fresh list
 *
 * ## Thread safety
 * [CallLogReader] and [SmsReader] both run their cursor queries on [Dispatchers.IO]
 * internally, so [getRecentItems] is safe to collect on any dispatcher.
 *
 * ## Empty-permission behaviour
 * If a permission is missing, the ContentResolver query returns null and the
 * reader returns an empty list — the UI shows whatever it can (e.g. only
 * messages if CALL_LOG is denied). No crash.
 */
@Singleton
class InboxRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callLogReader: CallLogReader,
    private val smsReader: SmsReader,
) : InboxRepository {

    private val contentResolver = context.contentResolver

    override fun getRecentItems(): Flow<List<InboxItem>> {
        val callsFlow = observeUri(contentResolver, CallLog.Calls.CONTENT_URI)
            .map {
                callLogReader.readRecentCalls(contentResolver, MAX_ITEMS_PER_SOURCE)
            }

        val messagesFlow = observeUri(contentResolver, Telephony.Sms.CONTENT_URI)
            .map {
                smsReader.readRecentThreads(
                    contentResolver = contentResolver,
                    limit           = SmsReader.RAW_SCAN_LIMIT,
                    maxThreads      = MAX_ITEMS_PER_SOURCE,
                )
            }

        return combine(callsFlow, messagesFlow) { calls, messages ->
            (calls + messages)
                .sortedByDescending { it.timestamp }
        }
    }
}
