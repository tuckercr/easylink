package com.tuckercr.ezlauncher.data.inbox

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import com.tuckercr.ezlauncher.domain.model.CallType
import com.tuckercr.ezlauncher.domain.model.InboxItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads recent entries from [CallLog.Calls].
 *
 * ## Permission
 * Requires [android.Manifest.permission.READ_CALL_LOG].
 * On Android 9+ (API 28+) this is separate from READ_CONTACTS and must
 * be declared and requested independently.
 *
 * ## Contact name resolution
 * [CallLog.Calls.CACHED_NAME] contains the display name cached by the
 * system dialler at the time of the call. We prefer this over a live
 * Contacts lookup to avoid an extra query per row. If the cache is empty
 * (unknown number) we fall back to the raw number string.
 *
 * ## Photo URI
 * [CallLog.Calls.CACHED_PHOTO_URI] is available on API 21+.
 */
@Singleton
class CallLogReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun readRecentCalls(
        contentResolver: ContentResolver,
        limit: Int,
    ): List<InboxItem.Call> = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext emptyList()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
        )

        val uri = CallLog.Calls.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", limit.toString())
            .build()

        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        ) ?: return@withContext emptyList()

        val results = mutableListOf<InboxItem.Call>()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val nameCol = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val numberCol = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val photoCol = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
            val dateCol = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeCol = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val durationCol = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val rawNumber = it.getString(numberCol) ?: continue
                val cachedName = it.getString(nameCol)
                val photoUriStr = it.getString(photoCol)

                results += InboxItem.Call(
                    id = it.getLong(idCol),
                    displayName = cachedName?.takeIf { n -> n.isNotBlank() } ?: rawNumber,
                    phoneNumber = rawNumber,
                    photoUri = photoUriStr?.let { s -> Uri.parse(s) },
                    timestamp = it.getLong(dateCol),
                    type = CallType.fromInt(it.getInt(typeCol)),
                    durationSeconds = it.getLong(durationCol),
                )
            }
        }

        results
    }
}
