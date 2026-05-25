package com.tuckercr.ezlauncher.di

import com.tuckercr.ezlauncher.data.repository.AppRepositoryImpl
import com.tuckercr.ezlauncher.domain.repository.AppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires domain interfaces to their data-layer implementations.
 *
 * Using @Binds (rather than @Provides) is more efficient — Hilt generates
 * a direct delegate instead of a wrapper method, and the compiler catches
 * type mismatches at build time rather than runtime.
 *
 * Installing in [SingletonComponent] means one shared instance lives for
 * the lifetime of the application process — correct for a repository that
 * maintains a cache of installed apps.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * Tells Hilt: "whenever something asks for [AppRepository],
     * give it [AppRepositoryImpl]."
     *
     * [AppRepositoryImpl] is @Singleton because it holds BroadcastReceiver
     * state. All ViewModels share the same instance and the same data stream.
     */
    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository
}
