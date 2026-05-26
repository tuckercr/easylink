package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.DeviceContact
import com.tuckercr.ezlauncher.domain.model.SpeedDialContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddSpeedDialContactUseCaseTest {

    private lateinit var repository: SpeedDialRepository
    private lateinit var useCase: AddSpeedDialContactUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = AddSpeedDialContactUseCase(repository)
    }

    private fun deviceContact(
        contactId: Long = 1L,
        name: String = "Alice Smith",
        phoneNumber: String = "555-0100",
    ) = DeviceContact(
        contactId = contactId,
        name = name,
        phoneNumber = phoneNumber,
        photoUri = null,
    )

    private fun pinnedContact(contactId: Long = 99L) =
        SpeedDialContact(
            id = 1L,
            contactId = contactId,
            name = "Bob Jones",
            phoneNumber = "555-0200",
            photoUri = null,
            displayOrder = 0,
        )

    @Test
    fun `returns Success and saves when contact is valid and not already pinned`() =
        runTest {
            every { repository.getSpeedDialContacts() } returns flowOf(emptyList())
            val contact = deviceContact()

            val result = useCase(contact)

            assertEquals(AddSpeedDialContactUseCase.Result.Success, result)
            coVerify(exactly = 1) { repository.addContact(contact) }
        }

    @Test
    fun `returns InvalidContact when name is blank`() =
        runTest {
            every { repository.getSpeedDialContacts() } returns flowOf(emptyList())
            val result = useCase(deviceContact(name = "   "))

            assertEquals(AddSpeedDialContactUseCase.Result.InvalidContact, result)
            coVerify(exactly = 0) { repository.addContact(any()) }
        }

    @Test
    fun `returns InvalidContact when phone number is blank`() =
        runTest {
            every { repository.getSpeedDialContacts() } returns flowOf(emptyList())
            val result = useCase(deviceContact(phoneNumber = ""))

            assertEquals(AddSpeedDialContactUseCase.Result.InvalidContact, result)
            coVerify(exactly = 0) { repository.addContact(any()) }
        }

    @Test
    fun `returns AlreadyAdded when contactId is already pinned`() =
        runTest {
            val existing = pinnedContact(contactId = 1L)
            every { repository.getSpeedDialContacts() } returns flowOf(listOf(existing))

            val result = useCase(deviceContact(contactId = 1L))

            assertEquals(AddSpeedDialContactUseCase.Result.AlreadyAdded, result)
            coVerify(exactly = 0) { repository.addContact(any()) }
        }

    @Test
    fun `allows adding a different contact even when others are pinned`() =
        runTest {
            val existing = pinnedContact(contactId = 99L)
            every { repository.getSpeedDialContacts() } returns flowOf(listOf(existing))
            val newContact = deviceContact(contactId = 42L)

            val result = useCase(newContact)

            assertEquals(AddSpeedDialContactUseCase.Result.Success, result)
            coVerify(exactly = 1) { repository.addContact(newContact) }
        }

    @Test
    fun `validation runs before duplicate check and InvalidContact wins`() =
        runTest {
            // Name is blank AND contactId already pinned — InvalidContact should be returned
            val existing = pinnedContact(contactId = 1L)
            every { repository.getSpeedDialContacts() } returns flowOf(listOf(existing))

            val result = useCase(deviceContact(contactId = 1L, name = ""))

            assertEquals(AddSpeedDialContactUseCase.Result.InvalidContact, result)
        }
}
