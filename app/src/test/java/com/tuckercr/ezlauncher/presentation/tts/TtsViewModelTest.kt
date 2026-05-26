package com.tuckercr.ezlauncher.presentation.tts

import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.model.TtsState
import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import com.tuckercr.ezlauncher.domain.usecase.SpeakLabelUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TtsRepository
    private lateinit var speakLabel: SpeakLabelUseCase
    private lateinit var viewModel: TtsViewModel

    private val engineStateFlow = MutableStateFlow<TtsState>(TtsState.Initializing)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        speakLabel = mockk(relaxed = true)
        every { repository.engineState } returns engineStateFlow
        every { repository.getPreferences() } returns flowOf(TtsPreferences.Default)
        viewModel = TtsViewModel(repository, speakLabel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `canSpeak is false while engine is initializing`() =
        runTest {
            backgroundScope.launch { viewModel.canSpeak.collect() }
            engineStateFlow.value = TtsState.Initializing
            advanceUntilIdle()
            assertFalse(viewModel.canSpeak.value)
        }

    @Test
    fun `canSpeak is true when engine ready and TTS enabled`() =
        runTest {
            every { repository.getPreferences() } returns flowOf(TtsPreferences(isEnabled = true))
            val vm = TtsViewModel(repository, speakLabel)
            backgroundScope.launch { vm.canSpeak.collect() }
            engineStateFlow.value = TtsState.Ready()
            advanceUntilIdle()
            assertTrue(vm.canSpeak.value)
        }

    @Test
    fun `canSpeak is false when engine ready but TTS disabled`() =
        runTest {
            every { repository.getPreferences() } returns flowOf(TtsPreferences(isEnabled = false))
            val vm = TtsViewModel(repository, speakLabel)
            backgroundScope.launch { vm.canSpeak.collect() }
            engineStateFlow.value = TtsState.Ready()
            advanceUntilIdle()
            assertFalse(vm.canSpeak.value)
        }

    @Test
    fun `canSpeak is false when engine is unavailable`() =
        runTest {
            backgroundScope.launch { viewModel.canSpeak.collect() }
            engineStateFlow.value = TtsState.Unavailable("no engine")
            advanceUntilIdle()
            assertFalse(viewModel.canSpeak.value)
        }

    @Test
    fun `speak delegates to SpeakLabelUseCase`() =
        runTest {
            viewModel.speak("Camera")
            verify { speakLabel("Camera") }
        }

    @Test
    fun `stop delegates to repository`() =
        runTest {
            viewModel.stop()
            verify { repository.stop() }
        }

    @Test
    fun `isSpeaking reflects engine state`() =
        runTest {
            backgroundScope.launch { viewModel.isSpeaking.collect() }
            engineStateFlow.value = TtsState.Ready(isSpeaking = true)
            advanceUntilIdle()
            assertTrue(viewModel.isSpeaking.value)

            engineStateFlow.value = TtsState.Ready(isSpeaking = false)
            advanceUntilIdle()
            assertFalse(viewModel.isSpeaking.value)
        }
}
