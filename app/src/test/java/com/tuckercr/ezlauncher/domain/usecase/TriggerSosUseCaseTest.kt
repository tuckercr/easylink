package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.model.SosResult
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            coEvery { repository.triggerSos() } returns SosResult.NoContactsConfigured
            assertTrue(useCase() is SosResult.NoContactsConfigured)
        }

    @Test
    fun `returns PartialSuccess when repository returns that`() =
        runTest {
            val expected = SosResult.PartialSuccess(smsRecipients = 1, reason = "No location")
            coEvery { repository.triggerSos() } returns expected
            assertEquals(expected, useCase())
        }

    @Test
    fun `returns Failure containing exception message when repository throws`() =
        runTest {
            coEvery { repository.triggerSos() } throws RuntimeException("Network down")
            val result = useCase()
            assertTrue(result is SosResult.Failure)
            assertEquals("Network down", (result as SosResult.Failure).reason)
        }

    @Test
    fun `returns Failure with fallback message when exception has no message`() =
        runTest {
            coEvery { repository.triggerSos() } throws RuntimeException()
            val result = useCase() as SosResult.Failure
            assertEquals("Unexpected SOS error", result.reason)
        }
}
