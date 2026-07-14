package com.fangjet.launcher.presentation.speeddial

import app.cash.turbine.test
import com.fangjet.launcher.domain.model.SpeedDialContact
import com.fangjet.launcher.domain.repository.SpeedDialRepository
import com.fangjet.launcher.domain.usecase.GetSpeedDialContactsUseCase
import com.fangjet.launcher.domain.usecase.RemoveSpeedDialContactUseCase
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedDialViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SpeedDialRepository
    private lateinit var getContacts: GetSpeedDialContactsUseCase
    private lateinit var removeContact: RemoveSpeedDialContactUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        getContacts = GetSpeedDialContactsUseCase(repository)
        removeContact = RemoveSpeedDialContactUseCase(repository)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeViewModel() =
        SpeedDialViewModel(
            context = mockk(relaxed = true),
            getContacts = getContacts,
            removeContact = removeContact,
            repository = repository,
        )

    private fun contact(id: Long = 1L) =
        SpeedDialContact(
            id = id,
            contactId = id * 100,
            name = "Contact $id",
            phoneNumber = "555-000$id",
            photoUri = null,
            displayOrder = id.toInt() - 1,
        )

    @Test
    fun `initial state is Loading`() =
        runTest {
            every { repository.getSpeedDialContacts() } returns flowOf(emptyList())
            val vm = makeViewModel()
            assertEquals(SpeedDialUiState.Loading, vm.uiState.value)
        }

    @Test
    fun `emits Empty when no contacts are pinned`() =
        runTest {
            every { repository.getSpeedDialContacts() } returns flowOf(emptyList())
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1) // Loading
                assertEquals(SpeedDialUiState.Empty, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Success with contacts when pinned list is non-empty`() =
        runTest {
            val contacts = listOf(contact(1L), contact(2L))
            every { repository.getSpeedDialContacts() } returns flowOf(contacts)
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem() as SpeedDialUiState.Success
                assertEquals(contacts, state.contacts)
                assertFalse(state.callInProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState transitions from Success to Empty when last contact is removed`() =
        runTest {
            val contactsFlow = MutableStateFlow(listOf(contact(1L)))
            every { repository.getSpeedDialContacts() } returns contactsFlow
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1) // Loading
                assertTrue(awaitItem() is SpeedDialUiState.Success)

                contactsFlow.value = emptyList()
                assertEquals(SpeedDialUiState.Empty, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onRemoveContact delegates to RemoveSpeedDialContactUseCase`() =
        runTest {
            val c = contact(1L)
            every { repository.getSpeedDialContacts() } returns flowOf(listOf(c))
            val vm = makeViewModel()

            vm.onRemoveContact(c)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { repository.removeContact(c) }
        }

    @Test
    fun `onContactsReordered persists new order via repository`() =
        runTest {
            val contacts = listOf(contact(1L), contact(2L), contact(3L))
            every { repository.getSpeedDialContacts() } returns flowOf(contacts)
            val vm = makeViewModel()

            val reordered = listOf(contacts[2], contacts[0], contacts[1])
            vm.onContactsReordered(reordered)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                repository.reorderContacts(listOf(3L, 1L, 2L))
            }
        }
}
