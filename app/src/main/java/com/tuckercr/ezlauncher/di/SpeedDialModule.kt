package com.tuckercr.ezlauncher.di

import com.tuckercr.ezlauncher.data.repository.SpeedDialRepositoryImpl
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module wiring [SpeedDialRepository] → [SpeedDialRepositoryImpl].
 *
 * [SpeedDialDao] and [ContactsHelper] are provided by [DatabaseModule]
 * and auto-injected via [SpeedDialRepositoryImpl]'s `@Inject` constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SpeedDialModule {

    @Binds
    @Singleton
    abstract fun bindSpeedDialRepository(
        impl: SpeedDialRepositoryImpl,
    ): SpeedDialRepository
}
