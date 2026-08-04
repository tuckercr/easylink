package com.fangjet.launcher.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entities: List<EmergencyContactEntity>): List<Long>

    @Update
    suspend fun updateAll(entities: List<EmergencyContactEntity>)

    @Query("DELETE FROM emergency_contacts WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<Long>)

    /**
     * Applies the caregiver's authoritative list while keeping local row ids
     * stable: incoming entities are matched to existing rows by [EmergencyContactEntity.remoteId]
     * and updated in place; unmatched ones are inserted; everything else —
     * including local-only rows, per the documented one-way sync — is deleted.
     * Stable ids mean the contacts list keeps item identity (scroll position,
     * animations) across caregiver saves instead of re-keying every row.
     */
    @Transaction
    suspend fun syncFromRemote(entities: List<EmergencyContactEntity>) {
        val existingByRemoteId = getAll()
            .filter { it.remoteId != null }
            .associateBy { it.remoteId }

        val toUpdate = mutableListOf<EmergencyContactEntity>()
        val toInsert = mutableListOf<EmergencyContactEntity>()
        val keepIds = mutableListOf<Long>()

        entities.forEach { incoming ->
            val match = incoming.remoteId?.let { existingByRemoteId[it] }
            if (match != null) {
                toUpdate += incoming.copy(id = match.id)
                keepIds += match.id
            } else {
                toInsert += incoming
            }
        }

        updateAll(toUpdate)
        keepIds += insertAll(toInsert)
        deleteAllExcept(keepIds)
    }
}
