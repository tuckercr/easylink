package com.fangjet.launcher.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {

    /** Reactive ordered list — emits on every change. */
    @Query("SELECT * FROM speed_dial_contacts ORDER BY displayOrder ASC")
    fun observeAll(): Flow<List<SpeedDialEntity>>

    /** One-shot current count — used when appending to determine the next displayOrder. */
    @Query("SELECT COUNT(*) FROM speed_dial_contacts")
    suspend fun count(): Int

    /** Insert or silently replace (unique constraint on contactId handles duplicates). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SpeedDialEntity): Long

    @Delete
    suspend fun delete(entity: SpeedDialEntity)

    /** Update the displayOrder for a single row during a reorder operation. */
    @Query("UPDATE speed_dial_contacts SET displayOrder = :order WHERE id = :id")
    suspend fun updateOrder(
        id: Long,
        order: Int,
    )

    @Update
    suspend fun update(entity: SpeedDialEntity)
}
