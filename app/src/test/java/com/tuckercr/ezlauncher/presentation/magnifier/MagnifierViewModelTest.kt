package com.tuckercr.ezlauncher.presentation.magnifier

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Unit tests for [MagnifierViewModel].
 *
 * Because the ViewModel has zero Android dependencies (no Context, no
 * camera hardware), these tests run as pure JVM tests — fast, reliable,
 * and no emulator required. This is the payoff of Clean Architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MagnifierViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MagnifierViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MagnifierViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zoom level 1 and all toggles off`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(MagnifierUiState.MIN_ZOOM, state.zoomLevel)
        assertFalse(state.isHighContrast)
        assertFalse(state.isFlashlightOn)
    }

    @Test
    fun `zoomIn increases zoom by ZOOM_STEP`() = runTest {
        viewModel.zoomIn()
        assertEquals(1.5f, viewModel.uiState.value.zoomLevel)
    }

    @Test
    fun `zoomIn does not exceed MAX_ZOOM`() = runTest {
        repeat(20) { viewModel.zoomIn() }
        assertEquals(MagnifierUiState.MAX_ZOOM, viewModel.uiState.value.zoomLevel)
    }

    @Test
    fun `zoomOut decreases zoom by ZOOM_STEP`() = runTest {
        viewModel.zoomIn()
        viewModel.zoomIn()
        viewModel.zoomOut()
        assertEquals(1.5f, viewModel.uiState.value.zoomLevel)
    }

    @Test
    fun `zoomOut does not go below MIN_ZOOM`() = runTest {
        repeat(20) { viewModel.zoomOut() }
        assertEquals(MagnifierUiState.MIN_ZOOM, viewModel.uiState.value.zoomLevel)
    }

    @Test
    fun `toggleHighContrast flips the flag`() = runTest {
        assertFalse(viewModel.uiState.value.isHighContrast)
        viewModel.toggleHighContrast()
        assertTrue(viewModel.uiState.value.isHighContrast)
        viewModel.toggleHighContrast()
        assertFalse(viewModel.uiState.value.isHighContrast)
    }

    @Test
    fun `toggleFlashlight flips the flag`() = runTest {
        assertFalse(viewModel.uiState.value.isFlashlightOn)
        viewModel.toggleFlashlight()
        assertTrue(viewModel.uiState.value.isFlashlightOn)
    }

    @Test
    fun `resetToDefaults restores all values`() = runTest {
        viewModel.zoomIn()
        viewModel.zoomIn()
        viewModel.toggleHighContrast()
        viewModel.toggleFlashlight()

        viewModel.resetToDefaults()

        val state = viewModel.uiState.value
        assertEquals(MagnifierUiState.MIN_ZOOM, state.zoomLevel)
        assertFalse(state.isHighContrast)
        assertFalse(state.isFlashlightOn)
    }
}
