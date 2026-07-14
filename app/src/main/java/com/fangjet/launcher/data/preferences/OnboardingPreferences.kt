package com.fangjet.launcher.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    @param:Named("onboardingPrefs") private val dataStore: DataStore<Preferences>,
) {
    private val keyComplete = booleanPreferencesKey("onboarding_complete")

    /** Emits true once the user has completed (or skipped through) onboarding. */
    val isComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[keyComplete] ?: false
    }

    suspend fun markComplete() {
        dataStore.edit { prefs -> prefs[keyComplete] = true }
    }
}
