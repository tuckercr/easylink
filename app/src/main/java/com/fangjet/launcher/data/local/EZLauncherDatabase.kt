package com.fangjet.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The single Room database for EZ Launcher.
 *
 * ## Version history
 *  - v1: emergency_contacts
 *  - v2: + medications, reminder_logs
 *  - v3: + speed_dial_contacts
 *
 * ## Migration policy
 * Always provide an explicit [Migration] rather than using
 * `fallbackToDestructiveMigration`. Deleting a user's emergency contacts,
 * medication schedule, or speed-dial favorites on upgrade is a serious bug,
 * not a convenience.
 */
@Database(
    entities = [
        EmergencyContactEntity::class,
        MedicationEntity::class,
        ReminderLogEntity::class,
        SpeedDialEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(MedicationConverters::class)
abstract class EZLauncherDatabase : RoomDatabase() {
    abstract fun emergencyContactDao(): EmergencyContactDao

    abstract fun medicationDao(): MedicationDao

    abstract fun reminderLogDao(): ReminderLogDao

    abstract fun speedDialDao(): SpeedDialDao
}

/**
 * Migration from v1 → v2: adds the medications and reminder_logs tables.
 *
 * Raw SQL mirrors exactly what Room would auto-generate, verified against
 * the exported schema JSON. Using explicit migrations means:
 *  1. Existing users keep their emergency contacts.
 *  2. CI can run `./gradlew verifyRoomSchemas` to catch schema drift.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medications` (
                `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name`          TEXT NOT NULL,
                `dosage`        TEXT NOT NULL,
                `notes`         TEXT NOT NULL,
                `reminderTimes` TEXT NOT NULL,
                `activeDays`    TEXT NOT NULL,
                `isActive`      INTEGER NOT NULL,
                `color`         TEXT NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_logs` (
                `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `medicationId`   INTEGER,
                `medicationName` TEXT NOT NULL,
                `dosage`         TEXT NOT NULL,
                `scheduledTime`  TEXT NOT NULL,
                `actionTime`     TEXT,
                `action`         TEXT NOT NULL,
                FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_logs_medicationId` ON `reminder_logs` (`medicationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_logs_scheduledTime` ON `reminder_logs` (`scheduledTime`)")
    }
}

/**
 * Migration from v2 → v3: adds the speed_dial_contacts table.
 *
 * The UNIQUE index on [contactId] is enforced at the DB level so duplicate
 * inserts are silently ignored even if the app logic misses the check.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `speed_dial_contacts` (
                `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contactId`      INTEGER NOT NULL,
                `name`           TEXT NOT NULL,
                `phoneNumber`    TEXT NOT NULL,
                `photoUriString` TEXT,
                `displayOrder`   INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_speed_dial_contacts_contactId` " +
                "ON `speed_dial_contacts` (`contactId`)",
        )
    }
}
