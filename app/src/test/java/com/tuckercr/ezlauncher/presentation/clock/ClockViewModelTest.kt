package com.tuckercr.ezlauncher.presentation.clock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ClockViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ClockViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── TimerState computed properties ────────────────────────────────────────

    @Test
    fun `TimerState isSet is false when totalSeconds is zero`() {
        assertFalse(TimerState().isSet)
    }

    @Test
    fun `TimerState isSet is true when totalSeconds is positive`() {
        assertTrue(TimerState(totalSeconds = 60, remainingSeconds = 60).isSet)
    }

    @Test
    fun `TimerState progress is zero when totalSeconds is zero`() {
        assertEquals(0f, TimerState().progress, 0.001f)
    }

    @Test
    fun `TimerState progress is 1 when remaining equals total`() {
        assertEquals(1f, TimerState(totalSeconds = 60, remainingSeconds = 60).progress, 0.001f)
    }

    @Test
    fun `TimerState progress is 0_5 when half time remains`() {
        assertEquals(0.5f, TimerState(totalSeconds = 60, remainingSeconds = 30).progress, 0.001f)
    }

    @Test
    fun `TimerState progress is 0 when remaining is 0`() {
        assertEquals(0f, TimerState(totalSeconds = 60, remainingSeconds = 0).progress, 0.001f)
    }

    @Test
    fun `TimerState displayTime formats mm_ss for under one hour`() {
        assertEquals("01:30", TimerState(remainingSeconds = 90).displayTime)
    }

    @Test
    fun `TimerState displayTime formats 00_00 for zero seconds`() {
        assertEquals("00:00", TimerState(remainingSeconds = 0).displayTime)
    }

    @Test
    fun `TimerState displayTime formats h_mm_ss for one hour or more`() {
        assertEquals("1:01:01", TimerState(remainingSeconds = 3661).displayTime)
    }

    @Test
    fun `TimerState displayTime formats exactly one hour`() {
        assertEquals("1:00:00", TimerState(remainingSeconds = 3600).displayTime)
    }

    // ── setTimer ──────────────────────────────────────────────────────────────

    @Test
    fun `setTimer initialises total and remaining seconds`() {
        viewModel.setTimer(120)
        with(viewModel.timerState.value) {
            assertEquals(120, totalSeconds)
            assertEquals(120, remainingSeconds)
            assertFalse(isRunning)
            assertFalse(isFinished)
        }
    }

    @Test
    fun `setTimer resets a previously running timer`() =
        runTest {
            viewModel.setTimer(10)
            viewModel.startPause()    // start
            viewModel.setTimer(60)   // reset to a new duration
            with(viewModel.timerState.value) {
                assertEquals(60, totalSeconds)
                assertFalse(isRunning)
            }
        }

    // ── startPause ────────────────────────────────────────────────────────────

    @Test
    fun `startPause does nothing when timer is not set`() {
        viewModel.startPause()
        assertFalse(viewModel.timerState.value.isRunning)
    }

    @Test
    fun `startPause marks the timer as running`() =
        runTest {
            viewModel.setTimer(60)
            viewModel.startPause()
            assertTrue(viewModel.timerState.value.isRunning)
        }

    @Test
    fun `startPause pauses a running timer`() =
        runTest {
            viewModel.setTimer(60)
            viewModel.startPause() // start
            viewModel.startPause() // pause
            assertFalse(viewModel.timerState.value.isRunning)
        }

    @Test
    fun `startPause does nothing when timer is finished`() =
        runTest {
            viewModel.setTimer(1)
            viewModel.startPause()
            advanceTimeBy(2_000)
            assertTrue(viewModel.timerState.value.isFinished)
            // Trying to start again should be a no-op
            viewModel.startPause()
            assertFalse(viewModel.timerState.value.isRunning)
        }

    // ── resetTimer ────────────────────────────────────────────────────────────

    @Test
    fun `resetTimer restores remaining to total and stops the timer`() =
        runTest {
            viewModel.setTimer(60)
            viewModel.startPause()
            advanceTimeBy(3_000)
            viewModel.resetTimer()
            with(viewModel.timerState.value) {
                assertEquals(60, remainingSeconds)
                assertFalse(isRunning)
                assertFalse(isFinished)
            }
        }

    @Test
    fun `resetTimer clears isFinished`() =
        runTest {
            viewModel.setTimer(1)
            viewModel.startPause()
            advanceTimeBy(2_000)
            assertTrue(viewModel.timerState.value.isFinished)
            viewModel.resetTimer()
            assertFalse(viewModel.timerState.value.isFinished)
        }

    // ── Countdown ticks ───────────────────────────────────────────────────────

    @Test
    fun `timer decrements remaining each second`() =
        runTest {
            viewModel.setTimer(10)
            viewModel.startPause()
            advanceTimeBy(3_100)
            val remaining = viewModel.timerState.value.remainingSeconds
            assertTrue("Expected remaining ≤ 7, got $remaining", remaining <= 7)
        }

    @Test
    fun `timer sets isFinished and clears isRunning when it reaches zero`() =
        runTest {
            viewModel.setTimer(3)
            viewModel.startPause()
            advanceTimeBy(4_000)
            with(viewModel.timerState.value) {
                assertTrue(isFinished)
                assertFalse(isRunning)
                assertEquals(0, remainingSeconds)
            }
        }

    @Test
    fun `timer emits decreasing remainingSeconds while running`() =
        runTest {
            viewModel.setTimer(5)
            viewModel.startPause()

            val states = mutableListOf<TimerState>()
            val job = launch { viewModel.timerState.toList(states) }
            advanceTimeBy(3_100)
            job.cancel()

            val remainingValues = states.map { it.remainingSeconds }
            assertTrue(remainingValues.contains(4))
            assertTrue(remainingValues.contains(3))
        }
}
