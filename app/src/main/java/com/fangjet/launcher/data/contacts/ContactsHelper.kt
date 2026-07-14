package com.fangjet.launcher.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.fangjet.launcher.domain.model.DeviceContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads contacts from the Android [ContactsContract] content provider.
 *
 * All queries run on [Dispatchers.IO]. Requires
 * [android.Manifest.permission.READ_CONTACTS] — callers must have already
 * obtained this permission before invoking any method here.
 *
 * ## Why not a repository?
 * The Android Contacts provider is not a database we own — it's a system
 * service. [ContactsHelper] is a focused helper that shields the rest of the
 * data layer from ContentResolver cursor boilerplate, following the single-
 * responsibility principle. [SpeedDialRepositoryImpl] delegates to it.
 */
@Singleton
class ContactsHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Returns contacts whose display name contains [query] (case-insensitive).
     * An empty query returns all contacts with at least one phone number.
     *
     * Note: the query targets [ContactsContract.CommonDataKinds.Phone.CONTENT_URI]
     * which has one row per phone number, not per contact. Deduplication by
     * contactId is applied so each person appears only once, using whichever
     * phone number row the provider returns first for them.
     */
    suspend fun searchContacts(query: String): List<DeviceContact> =
        withContext(Dispatchers.IO) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) return@withContext emptyList()

            val results = mutableListOf<DeviceContact>()

            val selection = if (query.isBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} IS NOT NULL"
            } else {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = if (query.isBlank()) null else arrayOf("%$query%")

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ),
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            ) ?: return@withContext emptyList()

            cursor.use {
                val idCol =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                // Deduplicate by contactId — a contact can have multiple phone rows
                val seen = mutableSetOf<Long>()

                while (it.moveToNext()) {
                    val contactId = it.getLong(idCol)
                    if (!seen.add(contactId)) continue

                    val name = it.getString(nameCol) ?: continue
                    val number = it.getString(numCol) ?: continue
                    val photoUriStr = it.getString(photoCol)

                    results += DeviceContact(
                        contactId = contactId,
                        name = name,
                        phoneNumber = number,
                        photoUri = photoUriStr?.toUri(),
                    )
                }
            }

            results
        }
}
