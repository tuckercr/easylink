package com.fangjet.launcher.presentation.speeddial.add

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.fangjet.launcher.data.contacts.ContactPhotoStore
import com.fangjet.launcher.domain.model.SpeedDialContact
import com.fangjet.launcher.domain.repository.SpeedDialRepository
import com.fangjet.launcher.domain.usecase.AddSpeedDialContactUseCase
import com.fangjet.launcher.domain.usecase.RemoveSpeedDialContactUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class AddSpeedDialViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SpeedDialRepository
    private lateinit var photoStore: ContactPhotoStore

    private val storedContact = SpeedDialContact(
        id = 7L,
        contactId = -1L,
        name = "Sarah Chen",
        phoneNumber = "5550101",
        photoUri = null,
        displayOrder = 0,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        photoStore = mockk()
        coEvery { repository.getSpeedDialContacts() } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeViewModel(editId: Long? = null): AddSpeedDialViewModel {
        val handle = if (editId != null) {
            SavedStateHandle(mapOf("speedDialId" to editId))
        } else {
            SavedStateHandle()
        }
        return AddSpeedDialViewModel(
            savedStateHandle = handle,
            addContact = AddSpeedDialContactUseCase(repository),
            removeContact = RemoveSpeedDialContactUseCase(repository),
            repository = repository,
            photoStore = photoStore,
        )
    }

    // ── Edit mode ─────────────────────────────────────────────────────────

    @Test
    fun `edit mode prefills the form from the stored contact`() =
        runTest {
            coEvery { repository.getContact(7L) } returns storedContact
            val vm = makeViewModel(editId = 7L)
            testDispatcher.scheduler.advanceUntilIdle()

            with(vm.uiState.value) {
                assertTrue(isEditMode)
                assertTrue(isManualEntry)
                assertEquals("Sarah Chen", manualName)
                assertEquals("5550101", manualPhone)
                assertNull(manualPhotoUri)
            }
        }

    @Test
    fun `edit mode save updates the contact with new values`() =
        runTest {
            coEvery { repository.getContact(7L) } returns storedContact
            val vm = makeViewModel(editId = 7L)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onManualNameChanged("Sarah C. Chen")
            vm.onManualPhoneChanged("5559999")
            vm.onManualSave()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                repository.updateContact(
                    storedContact.copy(name = "Sarah C. Chen", phoneNumber = "5559999"),
                )
            }
            assertEquals(
                AddSpeedDialUiState.SaveResult.Success,
                vm.uiState.value.saveResult,
            )
        }

    @Test
    fun `onDelete removes the contact and reports success`() =
        runTest {
            coEvery { repository.getContact(7L) } returns storedContact
            val vm = makeViewModel(editId = 7L)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onDelete()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { repository.removeContact(storedContact) }
            assertEquals(
                AddSpeedDialUiState.SaveResult.Success,
                vm.uiState.value.saveResult,
            )
        }

    @Test
    fun `onDelete is a no-op in add mode`() =
        runTest {
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onDelete()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { repository.removeContact(any()) }
            assertNull(vm.uiState.value.saveResult)
        }

    // ── Photo picking ─────────────────────────────────────────────────────

    @Test
    fun `onPhotoPicked imports the photo and stores the permanent uri`() =
        runTest {
            val picked = mockk<Uri>()
            val stored = mockk<Uri>()
            coEvery { photoStore.import(picked) } returns stored
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onPhotoPicked(picked)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(stored, vm.uiState.value.manualPhotoUri)
        }

    @Test
    fun `onPhotoPicked with null (picker cancelled) keeps existing photo`() =
        runTest {
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onPhotoPicked(null)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(vm.uiState.value.manualPhotoUri)
        }

    @Test
    fun `manual save includes the picked photo on the new contact`() =
        runTest {
            val picked = mockk<Uri>()
            val stored = mockk<Uri>()
            coEvery { photoStore.import(picked) } returns stored
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onManualNameChanged("Dr. Smith")
            vm.onManualPhoneChanged("5551234567")
            vm.onPhotoPicked(picked)
            testDispatcher.scheduler.advanceUntilIdle()
            vm.onManualSave()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                repository.addContact(
                    match { it.name == "Dr. Smith" && it.photoUri == stored },
                )
            }
        }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    fun `manual save with blank name sets an error and does not save`() =
        runTest {
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onManualPhoneChanged("5551234567")
            vm.onManualSave()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(vm.uiState.value.manualNameError)
            coVerify(exactly = 0) { repository.addContact(any()) }
        }

    @Test
    fun `manual save with short phone sets an error and does not save`() =
        runTest {
            val vm = makeViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.onManualNameChanged("Dr. Smith")
            vm.onManualPhoneChanged("123")
            vm.onManualSave()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(vm.uiState.value.manualPhoneError)
            coVerify(exactly = 0) { repository.addContact(any()) }
        }
}
