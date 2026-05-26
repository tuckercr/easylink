package com.tuckercr.ezlauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tuckercr.ezlauncher.data.fall.FallDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.fallDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "fall_prefs")

@Module
@InstallIn(SingletonComponent::class)
object FallDetectionModule {

    @Provides
    @Singleton
    @Named("fallPrefs")
    fun provideFallDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.fallDataStore

    /**
     * Provides a single shared [FallDetector] instance.
     * The service injects it and mutates [FallDetector.sensitivity] whenever
     * the DataStore preference changes.
     */
    @Provides
    @Singleton
    fun provideFallDetector(): FallDetector = FallDetector()
}
