package com.fangjet.launcher.presentation.main

import app.cash.turbine.test
import com.fangjet.launcher.data.preferences.HomePreferencesDataSource
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val highContrastFlow = MutableStateFlow(false)
    private lateinit var homePrefs: HomePreferencesDataSource
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        homePrefs = mockk(relaxed = true)
        every { homePrefs.highContrastEnabled } returns highContrastFlow
        viewModel = ThemeViewModel(homePrefs)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `highContrastEnabled is false by default`() {
        assertFalse(viewModel.highContrastEnabled.value)
    }

    @Test
    fun `highContrastEnabled reflects preference flow changes`() =
        runTest {
            viewModel.highContrastEnabled.test {
                assertFalse(awaitItem())
                highContrastFlow.value = true
                assertTrue(awaitItem())
                highContrastFlow.value = false
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `setHighContrastEnabled true calls data source`() =
        runTest {
            viewModel.setHighContrastEnabled(true)
            coVerify { homePrefs.setHighContrastEnabled(true) }
        }

    @Test
    fun `setHighContrastEnabled false calls data source`() =
        runTest {
            viewModel.setHighContrastEnabled(false)
            coVerify { homePrefs.setHighContrastEnabled(false) }
        }
}
