package com.fangjet.launcher.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for emergency contact CRUD operations.
 *
 * All queries use suspend functions or Flow — no blocking calls
 * on the main thread. Room handles the dispatcher automatically.
 */
@Dao
interface EmergencyContactDao {

    /**
     * Emits the full contact list ordered so the primary contact appears first.
     * Re-emits automatically whenever the table changes (Room + Flow = reactive DB).
     */
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    /** Returns all contacts as a one-shot list — used inside triggerSos(). */
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC")
    suspend fun getAll(): List<EmergencyContactEntity>

    /**
     * Insert or replace. REPLACE strategy handles updates — if [entity.id] > 0,
     * Room replaces the existing row; if id = 0, it inserts a new one.
     * Returns the new row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmergencyContactEntity): Long

    @Delete
    suspend fun delete(entity: EmergencyContactEntity)
}
