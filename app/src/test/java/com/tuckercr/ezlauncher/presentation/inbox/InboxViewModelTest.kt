package com.tuckercr.ezlauncher.presentation.inbox

import app.cash.turbine.test
import com.tuckercr.ezlauncher.domain.model.CallType
import com.tuckercr.ezlauncher.domain.model.InboxItem
import com.tuckercr.ezlauncher.domain.repository.InboxRepository
import com.tuckercr.ezlauncher.domain.usecase.GetRecentInboxItemsUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InboxRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeViewModel() =
        InboxViewModel(
            context = mockk(relaxed = true),
            getRecentItems = GetRecentInboxItemsUseCase(repository),
        )

    private fun call(
        id: Long = 1L,
        type: CallType = CallType.INCOMING,
        timestamp: Long = System.currentTimeMillis(),
    ) = InboxItem.Call(
        id = id,
        displayName = "Alice",
        phoneNumber = "555-0101",
        photoUri = null,
        timestamp = timestamp,
        type = type,
        durationSeconds = 90,
    )

    private fun message(
        id: Long = 10L,
        isRead: Boolean = true,
        timestamp: Long = System.currentTimeMillis(),
    ) = InboxItem.Message(
        id = id,
        displayName = "Bob",
        phoneNumber = "555-0202",
        photoUri = null,
        timestamp = timestamp,
        snippet = "Hey, how are you?",
        isRead = isRead,
        threadId = 99L,
        isIncoming = true,
    )

    // ── State transitions ──────────────────────────────────────────────────

    @Test
    fun `initial state is Loading`() =
        runTest {
            every { repository.getRecentItems() } returns flowOf(emptyList())
            val vm = makeViewModel()
            assertEquals(InboxUiState.Loading, vm.uiState.value)
        }

    @Test
    fun `emits Empty when repository returns empty list`() =
        runTest {
            every { repository.getRecentItems() } returns flowOf(emptyList())
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1) // Loading
                assertEquals(InboxUiState.Empty, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Success with items from repository`() =
        runTest {
            val items = listOf(call(1L), message(2L))
            every { repository.getRecentItems() } returns flowOf(items)
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem() as InboxUiState.Success
                assertEquals(items, state.items)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Badge counts ───────────────────────────────────────────────────────

    @Test
    fun `missedCallCount counts only MISSED calls`() =
        runTest {
            val items = listOf(
                call(1L, CallType.MISSED),
                call(2L, CallType.MISSED),
                call(3L, CallType.INCOMING),
                message(4L, isRead = true),
            )
            every { repository.getRecentItems() } returns flowOf(items)
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem() as InboxUiState.Success
                assertEquals(2, state.missedCallCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unreadMessageCount counts only unread messages`() =
        runTest {
            val items = listOf(
                message(1L, isRead = false),
                message(2L, isRead = false),
                message(3L, isRead = true),
                call(4L, CallType.INCOMING),
            )
            every { repository.getRecentItems() } returns flowOf(items)
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem() as InboxUiState.Success
                assertEquals(2, state.unreadMessageCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `all-read all-answered list has zero badge counts`() =
        runTest {
            val items = listOf(call(1L, CallType.INCOMING), message(2L, isRead = true))
            every { repository.getRecentItems() } returns flowOf(items)
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val state = awaitItem() as InboxUiState.Success
                assertEquals(0, state.missedCallCount)
                assertEquals(0, state.unreadMessageCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Reactivity ─────────────────────────────────────────────────────────

    @Test
    fun `uiState updates when repository emits a new list`() =
        runTest {
            val itemsFlow = MutableStateFlow<List<InboxItem>>(emptyList())
            every { repository.getRecentItems() } returns itemsFlow
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                assertEquals(InboxUiState.Empty, awaitItem())

                // Simulate a new call arriving
                itemsFlow.value = listOf(call(1L, CallType.MISSED))
                val updated = awaitItem() as InboxUiState.Success
                assertEquals(1, updated.missedCallCount)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Error handling ─────────────────────────────────────────────────────

    @Test
    fun `emits Error state when repository throws`() =
        runTest {
            every { repository.getRecentItems() } returns kotlinx.coroutines.flow.flow {
                throw RuntimeException("ContentResolver denied")
            }
            val vm = makeViewModel()

            vm.uiState.test {
                skipItems(1)
                val error = awaitItem() as InboxUiState.Error
                assertEquals("ContentResolver denied", error.message)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
