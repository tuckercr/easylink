package com.tuckercr.ezlauncher.presentation.home.customize

import app.cash.turbine.test
import com.tuckercr.ezlauncher.data.fall.FallDetectionManager
import com.tuckercr.ezlauncher.data.preferences.FallDetectionPreferences
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import com.tuckercr.ezlauncher.domain.model.FallSensitivity
import com.tuckercr.ezlauncher.domain.model.HomeButton
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class CustomizeHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var homePrefs: HomePreferencesDataSource
    private lateinit var fallPrefs: FallDetectionPreferences
    private lateinit var fallManager: FallDetectionManager
    private lateinit var viewModel: CustomizeHomeViewModel

    private val enabledButtonsFlow = MutableStateFlow(setOf(HomeButton.PHONE))
    private val voiceEnabledFlow = MutableStateFlow(false)
    private val fallEnabledFlow = MutableStateFlow(false)
    private val fallSensitivityFlow = MutableStateFlow(FallSensitivity.MEDIUM)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        homePrefs = mockk(relaxed = true)
        fallPrefs = mockk(relaxed = true)
        fallManager = mockk(relaxed = true)

        every { homePrefs.enabledButtons } returns enabledButtonsFlow
        every { homePrefs.voiceButtonEnabled } returns voiceEnabledFlow
        every { fallPrefs.isEnabled } returns fallEnabledFlow
        every { fallPrefs.sensitivity } returns fallSensitivityFlow

        viewModel = CustomizeHomeViewModel(homePrefs, fallPrefs, fallManager)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects preferences correctly`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.buttons.any { it.button == HomeButton.PHONE && it.isEnabled })
            assertFalse(initial.voiceButtonEnabled)
            assertFalse(initial.fallDetectionEnabled)
            assertEquals(FallSensitivity.MEDIUM, initial.fallSensitivity)

            // Update preferences
            enabledButtonsFlow.value = setOf(HomeButton.PHONE, HomeButton.CAMERA)
            voiceEnabledFlow.value = true
            fallEnabledFlow.value = true
            fallSensitivityFlow.value = FallSensitivity.HIGH

            val updated = awaitItem()
            assertTrue(updated.buttons.any { it.button == HomeButton.CAMERA && it.isEnabled })
            assertTrue(updated.voiceButtonEnabled)
            assertTrue(updated.fallDetectionEnabled)
            assertEquals(FallSensitivity.HIGH, updated.fallSensitivity)
        }
    }

    @Test
    fun `toggle button calls data source`() = runTest {
        viewModel.toggle(HomeButton.CAMERA, true)
        coVerify { homePrefs.setButtonEnabled(HomeButton.CAMERA, true) }
    }

    @Test
    fun `setVoiceButtonEnabled calls data source`() = runTest {
        viewModel.setVoiceButtonEnabled(true)
        coVerify { homePrefs.setVoiceButtonEnabled(true) }
    }

    @Test
    fun `setFallDetectionEnabled true calls prefs and starts manager`() = runTest {
        viewModel.setFallDetectionEnabled(true)
        coVerify { fallPrefs.setEnabled(true) }
        coVerify { fallManager.start() }
    }

    @Test
    fun `setFallDetectionEnabled false calls prefs and stops manager`() = runTest {
        viewModel.setFallDetectionEnabled(false)
        coVerify { fallPrefs.setEnabled(false) }
        coVerify { fallManager.stop() }
    }

    @Test
    fun `setFallSensitivity calls data source`() = runTest {
        viewModel.setFallSensitivity(FallSensitivity.LOW)
        coVerify { fallPrefs.setSensitivity(FallSensitivity.LOW) }
    }
}
