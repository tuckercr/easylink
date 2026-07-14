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

private val Context.weatherDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "weather_prefs")

@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    @Named("weatherPrefs")
    fun provideWeatherDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.weatherDataStore
}
