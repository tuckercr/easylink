package com.fangjet.launcher.presentation.medications

import app.cash.turbine.test
import com.fangjet.launcher.domain.model.Medication
import com.fangjet.launcher.domain.model.MedicationColor
import com.fangjet.launcher.domain.model.TodayReminder
import com.fangjet.launcher.domain.repository.MedicationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: MedicationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun medication(id: Long = 1L) =
        Medication(
            id = id,
            name = "Aspirin",
            dosage = "100mg",
            notes = "",
            reminderTimes = listOf(LocalTime.of(9, 0)),
            activeDays = setOf(DayOfWeek.MONDAY),
            isActive = true,
            color = MedicationColor.BLUE,
        )

    private fun reminder(
        status: TodayReminder.Status = TodayReminder.Status.UPCOMING,
        med: Medication = medication(),
    ) = TodayReminder(
        medication = med,
        scheduledAt = LocalDateTime.now().withHour(9).withMinute(0),
        status = status,
    )

    @Test
    fun `initial state is Loading`() =
        runTest {
            every { repository.getTodayReminders() } returns flowOf(emptyList())
            val viewModel = MedicationsViewModel(repository)

            assertEquals(MedicationsUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `emits Success with empty list when no reminders today`() =
        runTest {
            every { repository.getTodayReminders() } returns flowOf(emptyList())
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1) // Loading
                val state = awaitItem() as MedicationsUiState.Success
                assertTrue(state.reminders.isEmpty())
                assertFalse(state.allDone)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Success with reminders from repository`() =
        runTest {
            val reminders = listOf(reminder(TodayReminder.Status.UPCOMING))
            every { repository.getTodayReminders() } returns flowOf(reminders)
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1) // Loading
                val state = awaitItem() as MedicationsUiState.Success
                assertEquals(reminders, state.reminders)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `allDone is true when all reminders are TAKEN`() =
        runTest {
            val reminders = listOf(
                reminder(TodayReminder.Status.TAKEN, medication(1L)),
                reminder(TodayReminder.Status.TAKEN, medication(2L)),
            )
            every { repository.getTodayReminders() } returns flowOf(reminders)
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                val state = awaitItem() as MedicationsUiState.Success
                assertTrue(state.allDone)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `allDone is false when any reminder is UPCOMING`() =
        runTest {
            val reminders = listOf(
                reminder(TodayReminder.Status.TAKEN, medication(1L)),
                reminder(TodayReminder.Status.UPCOMING, medication(2L)),
            )
            every { repository.getTodayReminders() } returns flowOf(reminders)
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                val state = awaitItem() as MedicationsUiState.Success
                assertFalse(state.allDone)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState updates reactively when repository emits new list`() =
        runTest {
            val remindersFlow = MutableStateFlow<List<TodayReminder>>(emptyList())
            every { repository.getTodayReminders() } returns remindersFlow
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1) // Loading
                val empty = awaitItem() as MedicationsUiState.Success
                assertTrue(empty.reminders.isEmpty())

                remindersFlow.value = listOf(reminder(TodayReminder.Status.TAKEN))
                val updated = awaitItem() as MedicationsUiState.Success
                assertEquals(1, updated.reminders.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Error when repository throws`() =
        runTest {
            every { repository.getTodayReminders() } returns kotlinx.coroutines.flow.flow {
                throw RuntimeException("DB error")
            }
            val viewModel = MedicationsViewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                val error = awaitItem() as MedicationsUiState.Error
                assertEquals("DB error", error.message)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
