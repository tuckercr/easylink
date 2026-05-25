package com.tuckercr.ezlauncher.data.repository

import com.tuckercr.ezlauncher.data.contacts.ContactsHelper
import com.tuckercr.ezlauncher.data.local.SpeedDialDao
import com.tuckercr.ezlauncher.data.local.SpeedDialEntity
import com.tuckercr.ezlauncher.domain.model.DeviceContact
import com.tuckercr.ezlauncher.domain.model.SpeedDialContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [SpeedDialRepository].
 *
 * ## Data sources
 *  - [SpeedDialDao]     — Room table for pinned contacts (persisted)
 *  - [ContactsHelper]   — Android Contacts provider (device contacts)
 *
 * ## Ordering strategy
 * Display order is stored as a simple 0-based integer. On add we use
 * `count()` as the next index — O(1). On delete the remaining rows keep
 * their relative order (gaps are fine; the DAO sorts by displayOrder ASC).
 * On explicit reorder we write the full list in a loop — acceptable since
 * Speed Dial lists are small (typically < 12 contacts).
 */
@Singleton
class SpeedDialRepositoryImpl @Inject constructor(
    private val dao: SpeedDialDao,
    private val contactsHelper: ContactsHelper,
) : SpeedDialRepository {

    override fun getSpeedDialContacts(): Flow<List<SpeedDialContact>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addContact(contact: DeviceContact) {
        val nextOrder = dao.count()
        val entity = SpeedDialEntity(
            contactId      = contact.contactId,
            name           = contact.name,
            phoneNumber    = contact.phoneNumber,
            photoUriString = contact.photoUri?.toString(),
            displayOrder   = nextOrder,
        )
        dao.insert(entity)
    }

    override suspend fun removeContact(contact: SpeedDialContact) {
        dao.delete(SpeedDialEntity.fromDomain(contact))
    }

    /**
     * Write new displayOrder values for all pinned contacts.
     *
     * @param orderedIds [SpeedDialContact.id] values in the desired order.
     */
    override suspend fun reorderContacts(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateOrder(id = id, order = index)
        }
    }

    override suspend fun searchDeviceContacts(query: String, limit: Int): List<DeviceContact> =
        contactsHelper.searchContacts(query, limit)
}
