package com.fangjet.launcher.di

import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import com.fangjet.launcher.data.weather.CrashlyticsWeatherFetchReporter
import com.fangjet.weather.WeatherApiConfig
import com.fangjet.weather.WeatherApiConfigProvider
import com.fangjet.weather.WeatherFetchReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the Firebase-free `:weather` module to this app's infrastructure:
 * Remote Config supplies the endpoint/key, Crashlytics receives failure
 * telemetry. See [WeatherApiConfig] for why the endpoint is server-tunable.
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherIntegrationModule {

    @Provides
    fun provideWeatherApiConfig(defaults: SettingsDefaultsProvider): WeatherApiConfigProvider =
        WeatherApiConfigProvider {
            val current = defaults.current()
            WeatherApiConfig(
                baseUrl = current.weatherApiBaseUrl,
                apiKey = current.weatherApiKey,
            )
        }

    @Provides
    @Singleton
    fun provideWeatherFetchReporter(impl: CrashlyticsWeatherFetchReporter): WeatherFetchReporter = impl
}
