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

private val Context.pairingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "pairing_prefs")

@Module
@InstallIn(SingletonComponent::class)
object PairingModule {

    @Provides
    @Singleton
    @Named("pairingPrefs")
    fun providePairingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.pairingDataStore
}
