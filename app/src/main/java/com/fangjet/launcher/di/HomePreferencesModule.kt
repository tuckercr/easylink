package com.fangjet.launcher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

// Top-level delegate — only one instance created per process, stored against Context.
private val Context.homeDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "home_prefs")

@Module
@InstallIn(SingletonComponent::class)
object HomePreferencesModule {

    /**
     * Provides the DataStore used by [HomePreferencesDataSource].
     * Uses @Named to distinguish from the TTS DataStore (same type, different file).
     */
    @Provides
    @Singleton
    @Named("homePrefs")
    fun provideHomeDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.homeDataStore
}
