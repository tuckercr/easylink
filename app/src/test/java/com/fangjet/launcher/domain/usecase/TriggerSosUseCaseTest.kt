package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.model.SosResult
import com.fangjet.launcher.domain.repository.SosRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerSosUseCaseTest {

    private val repository: SosRepository = mockk()
    private val useCase = TriggerSosUseCase(repository)

    private val fakeContact =
        EmergencyContact(id = 1, name = "Alice", phoneNumber = "5551234", isPrimary = true)

    @Test
    fun `returns Success when repository succeeds`() =
        runTest {
            val expected =
                SosResult.Success(fakeContact, smsRecipients = 2, locationShared = true)
            coEvery { repository.triggerSos() } returns expected
            assertEquals(expected, useCase())
        }

    @Test
    fun `returns NoContactsConfigured when repository returns that`() =
        runTest {
            val expected = SosResult.NoContactsConfigured
            coEvery { repository.triggerSos() } returns expected
            assertEquals(expected, useCase())
        }

    @Test
    fun `returns PartialSuccess when repository returns that`() =
        runTest {
            val expected = SosResult.PartialSuccess(smsRecipients = 1, reason = "No location")
            coEvery { repository.triggerSos() } returns expected
            assertEquals(expected, useCase())
        }

    @Test
    fun `returns Failure containing exception message when repository returns that`() =
        runTest {
            val expected = SosResult.Failure("Network down")
            coEvery { repository.triggerSos() } returns expected
            val result = useCase()
            assertEquals(expected, result)
        }
}
