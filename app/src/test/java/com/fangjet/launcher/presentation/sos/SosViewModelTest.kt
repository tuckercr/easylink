package com.fangjet.launcher.presentation.sos

import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.model.SosResult
import com.fangjet.launcher.domain.usecase.TriggerSosUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SosViewModel].
 *
 * Uses [StandardTestDispatcher] with [advanceTimeBy] to control coroutine
 * time — the countdown uses [delay], which the test scheduler controls
 * without real wall-clock waiting. This makes the tests instant despite
 * testing a 5-second countdown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var triggerSos: TriggerSosUseCase
    private lateinit var viewModel: SosViewModel

    private val fakeContact =
        EmergencyContact(id = 1, name = "Alice", phoneNumber = "5551234", isPrimary = true)
    private val successResult =
        SosResult.Success(calledContact = fakeContact, smsRecipients = 1, locationShared = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        triggerSos = mockk()
        coEvery { triggerSos() } coAnswers {
            delay(10) // Ensure suspension so UI state transitions aren't conflated
            successResult
        }
        viewModel = SosViewModel(triggerSos)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Counting at COUNTDOWN_SECONDS`() =
        runTest {
            val state = viewModel.uiState.value
            assertTrue(state is SosUiState.Counting)
            assertEquals(SosUiState.COUNTDOWN_SECONDS, (state as SosUiState.Counting).secondsRemaining)
        }

    @Test
    fun `countdown decrements each second`() =
        runTest {
            val states = mutableListOf<SosUiState>()
            val job = launch { viewModel.uiState.toList(states) }

            advanceTimeBy(3_100) // advance 3 seconds
            job.cancel()

            val countingStates = states.filterIsInstance<SosUiState.Counting>()
            assertTrue(countingStates.any { it.secondsRemaining == 3 })
            assertTrue(countingStates.any { it.secondsRemaining == 2 })
        }

    @Test
    fun `after full countdown transitions to Dispatching then Done`() =
        runTest {
            val states = mutableListOf<SosUiState>()
            val job = launch { viewModel.uiState.toList(states) }

            advanceTimeBy((SosUiState.COUNTDOWN_SECONDS + 1) * 1_000L)

            job.cancel()

            assertTrue("Expected Dispatching state", states.any { it is SosUiState.Dispatching })
            val done = states.filterIsInstance<SosUiState.Done>()
            assertTrue("Expected Done state", done.isNotEmpty())
            assertEquals(successResult, done.last().result)
        }

    @Test
    fun `cancel during countdown emits Cancelled and does not call triggerSos`() =
        runTest {
            advanceTimeBy(2_000)
            viewModel.cancel()
            advanceTimeBy(5_000) // let any pending work complete

            assertEquals(SosUiState.Cancelled, viewModel.uiState.value)
            coVerify(exactly = 0) { triggerSos() }
        }

    @Test
    fun `NoContactsConfigured result propagated to Done state`() =
        runTest {
            coEvery { triggerSos() } returns SosResult.NoContactsConfigured
            val vm = SosViewModel(triggerSos)

            val states = mutableListOf<SosUiState>()
            val job = launch { vm.uiState.toList(states) }
            advanceTimeBy((SosUiState.COUNTDOWN_SECONDS + 1) * 1_000L)
            job.cancel()

            val done = states.filterIsInstance<SosUiState.Done>().lastOrNull()
            assertTrue(done?.result is SosResult.NoContactsConfigured)
        }

    @Test
    fun `triggerSos is called exactly once after countdown`() =
        runTest {
            advanceTimeBy((SosUiState.COUNTDOWN_SECONDS + 1) * 1_000L)
            coVerify(exactly = 1) { triggerSos() }
        }
}
