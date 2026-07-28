package com.fangjet.launcher.di

import com.fangjet.launcher.BuildConfig
import com.fangjet.launcher.data.config.FeatureFlags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureFlagsModule {

    @Provides
    @Singleton
    fun provideFeatureFlags(): FeatureFlags = FeatureFlags(safetyFeatures = BuildConfig.SAFETY_FEATURES)
}
