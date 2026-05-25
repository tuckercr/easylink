package com.tuckercr.ezlauncher.di

import com.tuckercr.ezlauncher.data.repository.InboxRepositoryImpl
import com.tuckercr.ezlauncher.domain.repository.InboxRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module wiring [InboxRepository] → [InboxRepositoryImpl].
 *
 * [CallLogReader] and [SmsReader] are `@Singleton` with `@Inject` constructors
 * so Hilt provides them automatically — no explicit `@Provides` needed here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InboxModule {

    @Binds
    @Singleton
    abstract fun bindInboxRepository(impl: InboxRepositoryImpl): InboxRepository
}
