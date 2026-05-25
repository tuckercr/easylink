package com.tuckercr.ezlauncher.data.inbox

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Converts a ContentProvider URI into a reactive [Flow<Unit>] that emits
 * whenever the underlying data changes.
 *
 * ## How it works
 * Android's [ContentObserver] notifies subscribers when a ContentProvider
 * signals a change (insert, update, delete). We bridge this callback into a
 * coroutine Flow using [callbackFlow]:
 *
 *  1. Register a [ContentObserver] on [uri] with `notifyForDescendants = true`
 *     so changes to child URIs (e.g. `content://sms/inbox/42`) also trigger.
 *  2. Immediately `trySend(Unit)` so the first collector gets an initial emission
 *     and can load the current snapshot right away.
 *  3. [awaitClose] unregisters the observer when the Flow is cancelled —
 *     this prevents leaks when the Fragment navigates away.
 *
 * ## Usage
 * ```kotlin
 * observeUri(contentResolver, CallLog.Calls.CONTENT_URI)
 *     .onEach { /* re-query call log */ }
 *     .launchIn(scope)
 * ```
 *
 * @param contentResolver The app's [ContentResolver].
 * @param uri             The content URI to watch.
 */
fun observeUri(contentResolver: ContentResolver, uri: Uri): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) { trySend(Unit) }
        override fun onChange(selfChange: Boolean, changeUri: Uri?) { trySend(Unit) }
    }
    contentResolver.registerContentObserver(uri, /* notifyForDescendants= */ true, observer)
    trySend(Unit)  // emit once immediately so collectors get data on subscription
    awaitClose { contentResolver.unregisterContentObserver(observer) }
}
