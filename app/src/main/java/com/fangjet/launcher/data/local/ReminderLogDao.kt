package com.fangjet.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ReminderLogDao {

    /** Last 30 days of history, newest first. */
    @Query(
        """
        SELECT * FROM reminder_logs
        WHERE scheduledTime >= :since
        ORDER BY scheduledTime DESC
    """,
    )
    fun observeHistory(since: LocalDateTime): Flow<List<ReminderLogEntity>>

    /**
     * Today's logs as a reactive Flow — used to determine reminder status for
     * the schedule view. Emits a new list whenever any log is inserted/updated,
     * so the UI refreshes automatically when the user taps TAKEN on a notification.
     */
    @Query(
        """
        SELECT * FROM reminder_logs
        WHERE scheduledTime >= :dayStart AND scheduledTime < :dayEnd
    """,
    )
    fun getLogsForDay(
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
    ): Flow<List<ReminderLogEntity>>

    /** Look up a specific reminder slot (medication + scheduled time). */
    @Query(
        """
        SELECT * FROM reminder_logs
        WHERE medicationId = :medicationId AND scheduledTime = :scheduledTime
        LIMIT 1
    """,
    )
    suspend fun getLog(
        medicationId: Long,
        scheduledTime: LocalDateTime,
    ): ReminderLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ReminderLogEntity)
}
