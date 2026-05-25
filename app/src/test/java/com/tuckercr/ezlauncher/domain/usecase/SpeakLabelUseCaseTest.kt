package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SpeakLabelUseCase].
 *
 * This is a pure domain test — no Android classes, no coroutines, no mocking
 * of complex systems. We only verify what [SpeakLabelUseCase] guarantees:
 * sanitised text is forwarded to [TtsRepository.speak], and unspeakable
 * input never reaches the engine.
 *
 * [TtsRepository] is mocked with MockK so we can verify exactly what
 * text was passed to [speak] after sanitisation.
 */
class SpeakLabelUseCaseTest {

    private lateinit var repository: TtsRepository
    private lateinit var useCase: SpeakLabelUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)  // relaxed = no-op all unverified calls
        useCase = SpeakLabelUseCase(repository)
    }

    @Test
    fun `plain label is forwarded as-is`() {
        useCase("Phone")
        verify { repository.speak("Phone") }
    }

    @Test
    fun `newlines in label are collapsed to spaces`() {
        useCase("All\nApps")
        verify { repository.speak("All Apps") }
    }

    @Test
    fun `tabs are collapsed to spaces`() {
        useCase("Messages\t(3 unread)")
        verify { repository.speak("Messages (3 unread)") }
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        useCase("  Camera  ")
        verify { repository.speak("Camera") }
    }

    @Test
    fun `multiple spaces collapsed to single space`() {
        useCase("Flash   light")
        verify { repository.speak("Flash light") }
    }

    @Test
    fun `blank string does not call speak`() {
        useCase("   ")
        verify(exactly = 0) { repository.speak(any()) }
    }

    @Test
    fun `empty string does not call speak`() {
        useCase("")
        verify(exactly = 0) { repository.speak(any()) }
    }

    @Test
    fun `string that becomes blank after sanitisation does not call speak`() {
        useCase("\n\t\r")
        verify(exactly = 0) { repository.speak(any()) }
    }

    @Test
    fun `long label with mixed whitespace is fully cleaned`() {
        useCase("  All\n\nApps\t(installed) ")
        verify { repository.speak("All Apps (installed)") }
    }
}
