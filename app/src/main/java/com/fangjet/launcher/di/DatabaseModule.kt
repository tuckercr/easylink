package com.fangjet.launcher.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fangjet.launcher.data.local.EasyLinkDatabase
import com.fangjet.launcher.data.local.EmergencyContactDao
import com.fangjet.launcher.data.local.MIGRATION_1_2
import com.fangjet.launcher.data.local.MIGRATION_2_3
import com.fangjet.launcher.data.local.MIGRATION_3_4
import com.fangjet.launcher.data.local.MedicationDao
import com.fangjet.launcher.data.local.ReminderLogDao
import com.fangjet.launcher.data.local.SpeedDialDao
import com.fangjet.launcher.data.repository.SosRepositoryImpl
import com.fangjet.launcher.domain.repository.SosRepository
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): EasyLinkDatabase =
        Room
            .databaseBuilder(
                context,
                EasyLinkDatabase::class.java,
                "easylink.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            // TRUNCATE journal mode writes directly to the .db file instead of
            // WAL side-car files (.db-wal / .db-shm).  This keeps the database
            // as a single consistent file, which Android Auto Backup can capture
            // reliably without risk of restoring an inconsistent WAL state.
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()

    @Provides
    @Singleton
    fun provideEmergencyContactDao(db: EasyLinkDatabase): EmergencyContactDao = db.emergencyContactDao()

    @Provides
    @Singleton
    fun provideMedicationDao(db: EasyLinkDatabase): MedicationDao = db.medicationDao()

    @Provides
    @Singleton
    fun provideReminderLogDao(db: EasyLinkDatabase): ReminderLogDao = db.reminderLogDao()

    @Provides
    @Singleton
    fun provideSpeedDialDao(db: EasyLinkDatabase): SpeedDialDao = db.speedDialDao()
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
