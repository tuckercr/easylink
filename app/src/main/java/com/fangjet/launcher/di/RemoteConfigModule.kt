package com.fangjet.launcher.di

import com.fangjet.launcher.data.config.RemoteConfigSettingsDefaultsProvider
import com.fangjet.launcher.data.config.SettingsDefaultsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    @Singleton
    abstract fun bindSettingsDefaultsProvider(impl: RemoteConfigSettingsDefaultsProvider): SettingsDefaultsProvider
}
