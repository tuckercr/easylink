package com.fangjet.launcher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.permissionAskDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "permission_prefs")

/**
 * Tracks which runtime permissions this install has actually requested.
 *
 * Needed to tell "never asked" from "permanently denied": both report
 * `shouldShowRequestPermissionRationale() == false`, and a permanently denied
 * request is silently swallowed by the system — on current Android without
 * even invoking the result callback — so tap handlers must recognise the dead
 * end themselves, before launching.
 *
 * This store is EXCLUDED from Auto Backup (see backup_descriptor.xml /
 * data_extraction_rules.xml): permissions reset on reinstall, so restored
 * asked-before flags would claim a fresh install had already asked.
 */
@Singleton
class PermissionAskPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.permissionAskDataStore

    /** True once [markAsked] has been called for [permission] on this install. */
    fun asked(permission: String): Flow<Boolean> = dataStore.data.map { prefs -> prefs[keyFor(permission)] ?: false }

    suspend fun markAsked(permission: String) {
        dataStore.edit { prefs -> prefs[keyFor(permission)] = true }
    }

    private fun keyFor(permission: String) = booleanPreferencesKey("asked_$permission")
}
