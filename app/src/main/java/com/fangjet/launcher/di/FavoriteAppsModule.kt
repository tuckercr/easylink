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

private val Context.appUsageDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "app_usage")

private val Context.favoriteAppsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "favorite_apps")

@Module
@InstallIn(SingletonComponent::class)
object FavoriteAppsModule {

    @Provides
    @Singleton
    @Named("appUsagePrefs")
    fun provideAppUsageDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.appUsageDataStore

    @Provides
    @Singleton
    @Named("favoriteAppsPrefs")
    fun provideFavoriteAppsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.favoriteAppsDataStore
}
