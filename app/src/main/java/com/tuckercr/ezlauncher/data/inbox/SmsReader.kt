package com.tuckercr.ezlauncher.data.inbox

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.tuckercr.ezlauncher.domain.model.InboxItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads recent SMS messages from [Telephony.Sms].
 *
 * ## Permission
 * Requires [android.Manifest.permission.READ_SMS].
 *
 * ## One row per conversation
 * We query all SMS ordered by date DESC, then deduplicate by thread_id in
 * code — keeping only the first (most-recent) message per thread. This gives
 * us one Inbox row per conversation, matching the pattern users expect from
 * messaging apps.
 *
 * The alternative ([Telephony.Sms.Conversations]) is less reliable across
 * OEMs and omits fields like `address` and `read`, so we avoid it.
 *
 * ## Contact name resolution
 * We look up the display name and photo for each unique address via
 * [ContactsContract.PhoneLookup] after deduplication, so we only make
 * one query per distinct conversation rather than one per SMS row.
 */
@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun readRecentThreads(
        contentResolver: ContentResolver,
        limit: Int,
        maxThreads: Int,
    ): List<InboxItem.Message> = withContext(Dispatchers.IO) {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) return@withContext emptyList()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,        // 1 = inbox (incoming), 2 = sent (outgoing)
        )

        val uri = Telephony.Sms.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .build()

        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return@withContext emptyList()

        // Deduplicate: one entry per thread_id (first row wins = most recent)
        val seen    = mutableSetOf<Long>()
        val threads = mutableListOf<RawThread>()

        cursor.use {
            val idCol       = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadCol   = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressCol  = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol     = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol     = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readCol     = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val typeCol     = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext() && threads.size < maxThreads) {
                val threadId = it.getLong(threadCol)
                if (!seen.add(threadId)) continue

                threads += RawThread(
                    id         = it.getLong(idCol),
                    threadId   = threadId,
                    address    = it.getString(addressCol) ?: continue,
                    snippet    = it.getString(bodyCol)?.take(MAX_SNIPPET_LENGTH) ?: "",
                    timestamp  = it.getLong(dateCol),
                    isRead     = it.getInt(readCol) != 0,
                    isIncoming = it.getInt(typeCol) == Telephony.Sms.MESSAGE_TYPE_INBOX,
                )
            }
        }

        // Resolve contact names and photos for each unique address
        threads.map { raw ->
            val (name, photoUri) = resolveContact(contentResolver, raw.address)
            InboxItem.Message(
                id          = raw.id,
                displayName = name ?: raw.address,
                phoneNumber = raw.address,
                photoUri    = photoUri,
                timestamp   = raw.timestamp,
                snippet     = raw.snippet,
                isRead      = raw.isRead,
                threadId    = raw.threadId,
                isIncoming  = raw.isIncoming,
            )
        }
    }

    /**
     * Looks up [address] in the device contacts and returns (displayName, photoUri).
     * Returns (null, null) if the address is not found in contacts or permission is missing.
     */
    private fun resolveContact(
        contentResolver: ContentResolver,
        address: String,
    ): Pair<String?, Uri?> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return Pair(null, null)

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address),
        )
        val cursor = contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
            ),
            null, null, null,
        ) ?: return Pair(null, null)

        return cursor.use {
            if (it.moveToFirst()) {
                val name  = it.getString(0)
                val photo = it.getString(1)?.let { s -> Uri.parse(s) }
                Pair(name, photo)
            } else {
                Pair(null, null)
            }
        }
    }

    private data class RawThread(
        val id: Long,
        val threadId: Long,
        val address: String,
        val snippet: String,
        val timestamp: Long,
        val isRead: Boolean,
        val isIncoming: Boolean,
    )

    companion object {
        /** Truncate message snippets so the row doesn't overflow. */
        private const val MAX_SNIPPET_LENGTH = 80

        /** Raw SMS rows to scan before deduplication (generous so threads don't disappear). */
        const val RAW_SCAN_LIMIT = 200
    }
}
