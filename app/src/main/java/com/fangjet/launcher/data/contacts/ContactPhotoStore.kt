package com.fangjet.launcher.data.contacts

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ContactPhotoStore"

/**
 * Copies a user-picked profile photo into app-private storage.
 *
 * The photo picker only grants temporary read access to the source URI, so
 * storing that URI in Room would break after the process dies. Importing the
 * bytes into [Context.getFilesDir] gives us a stable file URI we own forever.
 */
@Singleton
class ContactPhotoStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Returns a permanent file URI for [source], or null if the copy failed. */
    suspend fun import(source: Uri): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "contact_photos").apply { mkdirs() }
                val dest = File(dir, "photo_${System.currentTimeMillis()}.img")
                context.contentResolver.openInputStream(source)!!.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.toUri()
            }.onFailure { Log.e(TAG, "Failed to import contact photo", it) }
                .getOrNull()
        }
}
