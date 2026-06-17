package com.tuckercr.ezlauncher.presentation.settings

import app.cash.turbine.test
import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import com.tuckercr.ezlauncher.domain.usecase.GetEmergencyContactsUseCase
import com.tuckercr.ezlauncher.domain.usecase.SaveEmergencyContactUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencySettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val contactsFlow = MutableStateFlow(emptyList<EmergencyContact>())
    private lateinit var getContacts: GetEmergencyContactsUseCase
    private lateinit var saveContact: SaveEmergencyContactUseCase
    private lateinit var repository: SosRepository
    private lateinit var viewModel: EmergencySettingsViewModel

    private val alice =
        EmergencyContact(id = 1, name = "Alice", phoneNumber = "5551234567", isPrimary = true)
    private val bob =
        EmergencyContact(id = 2, name = "Bob", phoneNumber = "5559876543", isPrimary = false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getContacts = mockk()
        saveContact = mockk()
        repository = mockk(relaxed = true)

        every { getContacts() } returns contactsFlow
        coEvery { saveContact(any()) } returns SaveEmergencyContactUseCase.Result.Success(alice)

        viewModel = EmergencySettingsViewModel(getContacts, saveContact, repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Ready with empty contacts`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertTrue(state.contacts.isEmpty())
                assertNull(state.editingContact)
            }
        }

    @Test
    fun `contacts list updates when the flow emits new values`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // empty initial state

                contactsFlow.value = listOf(alice)
                val updated = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals(listOf(alice), updated.contacts)
            }
        }

    // ── onAddContactClicked ───────────────────────────────────────────────────

    @Test
    fun `onAddContactClicked sets editingContact to an empty contact`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertNotNull(state.editingContact)
                assertEquals("", state.editingContact!!.name)
                assertEquals("", state.editingContact!!.phoneNumber)
            }
        }

    @Test
    fun `onAddContactClicked increments dialogKey each time`() =
        runTest {
            viewModel.uiState.test {
                val first = awaitItem() as EmergencySettingsUiState.Ready
                val initialKey = first.dialogKey

                viewModel.onAddContactClicked()
                val second = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals(initialKey + 1, second.dialogKey)

                viewModel.onDismissDialog()
                awaitItem()

                viewModel.onAddContactClicked()
                val third = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals(initialKey + 2, third.dialogKey)
            }
        }

    // ── onEditContactClicked ──────────────────────────────────────────────────

    @Test
    fun `onEditContactClicked pre-populates the dialog with the given contact`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onEditContactClicked(alice)
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals(alice, state.editingContact)
            }
        }

    // ── onDismissDialog ───────────────────────────────────────────────────────

    @Test
    fun `onDismissDialog clears editingContact`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                awaitItem()
                viewModel.onDismissDialog()
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertNull(state.editingContact)
            }
        }

    @Test
    fun `onDismissDialog clears any validation error`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                awaitItem()
                // Force a validation error by saving with a bad name
                coEvery { saveContact(any()) } returns
                    SaveEmergencyContactUseCase.Result.InvalidName("Name required")
                viewModel.onSaveContact("", "5551234567", false)
                awaitItem() // validation error state

                viewModel.onDismissDialog()
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertNull(state.validationError)
            }
        }

    // ── onSaveContact ─────────────────────────────────────────────────────────

    @Test
    fun `onSaveContact success clears editingContact and shows snackbar`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                awaitItem()

                coEvery { saveContact(any()) } returns
                    SaveEmergencyContactUseCase.Result.Success(alice)
                viewModel.onSaveContact("Alice", "5551234567", true)
                val state = awaitItem() as EmergencySettingsUiState.Ready

                assertNull(state.editingContact)
                assertEquals("Contact saved", state.snackbarMessage)
            }
        }

    @Test
    fun `onSaveContact with invalid name shows validation error`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                awaitItem()

                coEvery { saveContact(any()) } returns
                    SaveEmergencyContactUseCase.Result.InvalidName("Name cannot be empty")
                viewModel.onSaveContact("", "5551234567", false)
                val state = awaitItem() as EmergencySettingsUiState.Ready

                assertEquals("Name cannot be empty", state.validationError)
                assertNotNull(state.editingContact) // dialog still open
            }
        }

    @Test
    fun `onSaveContact with invalid phone shows validation error`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onAddContactClicked()
                awaitItem()

                coEvery { saveContact(any()) } returns
                    SaveEmergencyContactUseCase.Result.InvalidPhone("Invalid phone number")
                viewModel.onSaveContact("Alice", "abc", false)
                val state = awaitItem() as EmergencySettingsUiState.Ready

                assertEquals("Invalid phone number", state.validationError)
            }
        }

    @Test
    fun `onSaveContact does nothing when no contact is being edited`() =
        runTest {
            // No dialog open — should be a no-op
            viewModel.onSaveContact("Alice", "5551234567", true)
            coVerify(exactly = 0) { saveContact(any()) }
        }

    // ── onDeleteContact ───────────────────────────────────────────────────────

    @Test
    fun `onDeleteContact delegates to repository`() =
        runTest {
            viewModel.onDeleteContact(alice)
            coVerify { repository.deleteEmergencyContact(alice) }
        }

    @Test
    fun `onDeleteContact shows snackbar with contact name`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onDeleteContact(alice)
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals("${alice.name} removed", state.snackbarMessage)
            }
        }

    // ── onSnackbarShown ───────────────────────────────────────────────────────

    @Test
    fun `onSnackbarShown clears snackbarMessage`() =
        runTest {
            viewModel.uiState.test {
                awaitItem()
                viewModel.onDeleteContact(alice)
                awaitItem() // snackbar shown
                viewModel.onSnackbarShown()
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertNull(state.snackbarMessage)
            }
        }

    // ── Multiple contacts ─────────────────────────────────────────────────────

    @Test
    fun `multiple contacts are all reflected in the state`() =
        runTest {
            contactsFlow.value = listOf(alice, bob)
            viewModel.uiState.test {
                val state = awaitItem() as EmergencySettingsUiState.Ready
                assertEquals(2, state.contacts.size)
                assertTrue(state.contacts.contains(alice))
                assertTrue(state.contacts.contains(bob))
            }
        }
}
