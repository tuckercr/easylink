package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SaveEmergencyContactUseCase].
 *
 * Demonstrates testing validation logic that lives in the use case,
 * not the repository. The mock only needs to verify the happy path
 * is forwarded; the error paths never reach the repository at all.
 */
class SaveEmergencyContactUseCaseTest {

    private lateinit var repository: SosRepository
    private lateinit var useCase: SaveEmergencyContactUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SaveEmergencyContactUseCase(repository)
    }

    @Test
    fun `valid contact is saved and Success returned`() = runTest {
        val contact = EmergencyContact(name = "Alice", phoneNumber = "5551234567")
        coEvery { repository.saveEmergencyContact(contact) } returns contact.copy(id = 1)

        val result = useCase(contact)

        assertTrue(result is SaveEmergencyContactUseCase.Result.Success)
        coVerify(exactly = 1) { repository.saveEmergencyContact(contact) }
    }

    @Test
    fun `blank name returns InvalidName without calling repository`() = runTest {
        val contact = EmergencyContact(name = "   ", phoneNumber = "5551234567")

        val result = useCase(contact)

        assertTrue(result is SaveEmergencyContactUseCase.Result.InvalidName)
        coVerify(exactly = 0) { repository.saveEmergencyContact(any()) }
    }

    @Test
    fun `short phone number returns InvalidPhone without calling repository`() = runTest {
        val contact = EmergencyContact(name = "Bob", phoneNumber = "123")

        val result = useCase(contact)

        assertTrue(result is SaveEmergencyContactUseCase.Result.InvalidPhone)
        coVerify(exactly = 0) { repository.saveEmergencyContact(any()) }
    }

    @Test
    fun `phone with formatting characters is valid if enough digits`() = runTest {
        val contact = EmergencyContact(name = "Carol", phoneNumber = "(555) 123-4567")
        coEvery { repository.saveEmergencyContact(contact) } returns contact.copy(id = 2)

        val result = useCase(contact)

        assertTrue(result is SaveEmergencyContactUseCase.Result.Success)
    }

    @Test
    fun `name with only whitespace is trimmed and rejected`() = runTest {
        val contact = EmergencyContact(name = "\t\n", phoneNumber = "5551234567")
        val result = useCase(contact)
        assertTrue(result is SaveEmergencyContactUseCase.Result.InvalidName)
    }

    @Test
    fun `error message in InvalidPhone is non-empty`() = runTest {
        val contact = EmergencyContact(name = "Dave", phoneNumber = "12")
        val result = useCase(contact) as SaveEmergencyContactUseCase.Result.InvalidPhone
        assertTrue(result.message.isNotBlank())
    }
}
