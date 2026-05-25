package com.tuckercr.ezlauncher.di

import android.content.Context
import androidx.room.Room
import com.tuckercr.ezlauncher.data.local.EZLauncherDatabase
import com.tuckercr.ezlauncher.data.local.EmergencyContactDao
import com.tuckercr.ezlauncher.data.local.MedicationDao
import com.tuckercr.ezlauncher.data.local.MIGRATION_1_2
import com.tuckercr.ezlauncher.data.local.MIGRATION_2_3
import com.tuckercr.ezlauncher.data.local.ReminderLogDao
import com.tuckercr.ezlauncher.data.local.SpeedDialDao
import com.tuckercr.ezlauncher.data.repository.SosRepositoryImpl
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for all database-related bindings.
 *
 * Separated from [AppModule] because it uses both @Provides (for Room
 * and the DAO) and @Binds (for the SosRepository). Mixing @Provides
 * and @Binds in the same module requires the module to be abstract,
 * but @Provides methods must be on a non-abstract class — so we keep
 * them in separate modules. This is the standard Hilt pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the single Room database instance.
     *
     * In production, use explicit [Migration] objects when schema changes.
     * [fallbackToDestructiveMigration] is disabled here as a safeguard —
     * Room will crash on a missing migration rather than silently deleting
     * user's emergency contacts.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EZLauncherDatabase =
        Room.databaseBuilder(
            context,
            EZLauncherDatabase::class.java,
            "ezlauncher.db",
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    @Provides
    @Singleton
    fun provideEmergencyContactDao(db: EZLauncherDatabase): EmergencyContactDao =
        db.emergencyContactDao()

    @Provides
    @Singleton
    fun provideMedicationDao(db: EZLauncherDatabase): MedicationDao =
        db.medicationDao()

    @Provides
    @Singleton
    fun provideReminderLogDao(db: EZLauncherDatabase): ReminderLogDao =
        db.reminderLogDao()

    @Provides
    @Singleton
    fun provideSpeedDialDao(db: EZLauncherDatabase): SpeedDialDao =
        db.speedDialDao()
}

/**
 * Separate abstract module for @Binds declarations that depend on
 * the providers in [DatabaseModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SosModule {

    @Binds
    @Singleton
    abstract fun bindSosRepository(impl: SosRepositoryImpl): SosRepository
}
