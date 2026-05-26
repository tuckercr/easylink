package com.tuckercr.ezlauncher.di

import com.tuckercr.ezlauncher.data.repository.MedicationRepositoryImpl
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires [MedicationRepository] → [MedicationRepositoryImpl].
 *
 * ## What this module does NOT provide
 *  - [MedicationDao] / [ReminderLogDao] — provided by [DatabaseModule]
 *  - [AlarmScheduler] — `@Inject` constructor + `@Singleton`, auto-provided by Hilt
 *  - [ReminderNotificationHelper] — same; no explicit binding needed
 *
 * ## Why a separate module?
 * Keeping each feature's DI in its own module makes it easy to swap the
 * implementation (e.g. for integration tests) without touching the shared
 * [DatabaseModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MedicationModule {

    @Binds
    @Singleton
    abstract fun bindMedicationRepository(impl: MedicationRepositoryImpl): MedicationRepository
}
