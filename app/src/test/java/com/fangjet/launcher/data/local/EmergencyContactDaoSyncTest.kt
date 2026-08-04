package com.fangjet.launcher.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [EmergencyContactDao.syncFromRemote] — the Kotlin default method that
 * merges the caregiver's authoritative list into the local table while keeping
 * row ids stable. The abstract Room operations are replaced by an in-memory
 * fake, so the merge logic runs on the JVM without a database.
 */
class EmergencyContactDaoSyncTest {

    /** In-memory stand-in for the Room-generated DAO. */
    private class FakeDao : EmergencyContactDao {
        val rows = mutableListOf<EmergencyContactEntity>()
        private var nextId = 1L

        override fun observeAll(): Flow<List<EmergencyContactEntity>> = MutableStateFlow(rows.toList())

        override suspend fun getAll(): List<EmergencyContactEntity> = rows.toList()

        override suspend fun upsert(entity: EmergencyContactEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            rows.removeAll { it.id == id }
            rows += entity.copy(id = id)
            return id
        }

        override suspend fun delete(entity: EmergencyContactEntity) {
            rows.removeAll { it.id == entity.id }
        }

        override suspend fun deleteAll() = rows.clear()

        override suspend fun insertAll(entities: List<EmergencyContactEntity>): List<Long> = entities.map { upsert(it) }

        override suspend fun updateAll(entities: List<EmergencyContactEntity>) {
            entities.forEach { updated ->
                val index = rows.indexOfFirst { it.id == updated.id }
                if (index >= 0) rows[index] = updated
            }
        }

        override suspend fun deleteAllExcept(keepIds: List<Long>) {
            rows.removeAll { it.id !in keepIds }
        }
    }

    private fun remote(
        remoteId: String?,
        name: String,
        primary: Boolean = false,
    ) = EmergencyContactEntity(
        id = 0,
        name = name,
        phoneNumber = "555",
        isPrimary = primary,
        remoteId = remoteId,
    )

    @Test
    fun `re-syncing the same contacts keeps local row ids stable`() =
        runTest {
            val dao = FakeDao()
            dao.syncFromRemote(listOf(remote("a", "Sarah", primary = true), remote("b", "Tom")))
            val idsBefore = dao.rows.associate { it.remoteId to it.id }

            // Caregiver renames Sarah and saves — same remote ids come back.
            dao.syncFromRemote(listOf(remote("a", "Sarah Chen", primary = true), remote("b", "Tom")))

            val byRemote = dao.rows.associateBy { it.remoteId }
            assertEquals(idsBefore["a"], byRemote["a"]?.id)
            assertEquals(idsBefore["b"], byRemote["b"]?.id)
            assertEquals("Sarah Chen", byRemote["a"]?.name)
        }

    @Test
    fun `new remote contacts are inserted and removed ones deleted`() =
        runTest {
            val dao = FakeDao()
            dao.syncFromRemote(listOf(remote("a", "Sarah"), remote("b", "Tom")))
            val sarahId = dao.rows.first { it.remoteId == "a" }.id

            dao.syncFromRemote(listOf(remote("a", "Sarah"), remote("c", "Nina")))

            assertEquals(setOf("a", "c"), dao.rows.map { it.remoteId }.toSet())
            assertEquals(sarahId, dao.rows.first { it.remoteId == "a" }.id)
        }

    @Test
    fun `local-only rows are replaced by the authoritative caregiver list`() =
        runTest {
            val dao = FakeDao()
            // Added on the phone itself — no remote identity.
            dao.upsert(remote(null, "Local Larry"))

            dao.syncFromRemote(listOf(remote("a", "Sarah")))

            assertEquals(listOf("Sarah"), dao.rows.map { it.name })
        }

    @Test
    fun `an empty caregiver list clears the table`() =
        runTest {
            val dao = FakeDao()
            dao.syncFromRemote(listOf(remote("a", "Sarah")))
            dao.syncFromRemote(emptyList())
            assertTrue(dao.rows.isEmpty())
        }
}
