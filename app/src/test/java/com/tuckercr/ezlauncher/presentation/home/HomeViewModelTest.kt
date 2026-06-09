package com.tuckercr.ezlauncher.presentation.home

import app.cash.turbine.test
import com.tuckercr.ezlauncher.data.preferences.HomePreferencesDataSource
import com.tuckercr.ezlauncher.data.weather.WeatherService
import com.tuckercr.ezlauncher.domain.model.HomeButton
import com.tuckercr.ezlauncher.domain.model.WeatherInfo
import com.tuckercr.ezlauncher.domain.usecase.LaunchAppUseCase
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var launchAppUseCase: LaunchAppUseCase
    private lateinit var homePrefs: HomePreferencesDataSource
    private lateinit var weatherService: WeatherService
    private lateinit var viewModel: HomeViewModel

    private val enabledButtonsFlow = MutableStateFlow(setOf(HomeButton.PHONE))
    private val voiceEnabledFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        launchAppUseCase = mockk(relaxed = true)
        homePrefs = mockk(relaxed = true)
        weatherService = mockk(relaxed = true)

        every { homePrefs.enabledButtons } returns enabledButtonsFlow
        every { homePrefs.voiceButtonEnabled } returns voiceEnabledFlow
        coEvery { weatherService.fetch() } returns WeatherInfo.Unavailable("Network error")

        viewModel = HomeViewModel(launchAppUseCase, homePrefs, weatherService)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects preferences and flashlight correctly`() = runTest {
        viewModel.uiState.test {
            // Skip the initial Loading if it's there
            var item = awaitItem()
            if (item is HomeUiState.Loading) item = awaitItem()
            
            val success = item as HomeUiState.Success
            assertFalse(success.isFlashlightOn)
            assertEquals(listOf(HomeButton.PHONE), success.enabledButtons)
            assertFalse(success.voiceButtonEnabled)

            // Toggle flashlight
            viewModel.toggleFlashlight()
            val afterFlashlight = awaitItem() as HomeUiState.Success
            assertTrue(afterFlashlight.isFlashlightOn)

            // Update preferences
            enabledButtonsFlow.value = setOf(HomeButton.PHONE, HomeButton.CAMERA)
            voiceEnabledFlow.value = true
            
            val afterPrefs = awaitItem() as HomeUiState.Success
            assertEquals(listOf(HomeButton.PHONE, HomeButton.CAMERA), afterPrefs.enabledButtons)
            assertTrue(afterPrefs.voiceButtonEnabled)
        }
    }

    @Test
    fun `refreshWeather calls weather service`() = runTest {
        viewModel.refreshWeather()
        coVerify { weatherService.fetch() }
    }

    @Test
    fun `onAppTapped calls launchAppUseCase`() = runTest {
        viewModel.onAppTapped("com.test.app")
        coVerify { launchAppUseCase("com.test.app") }
    }

    @Test
    fun `setButtonEnabled calls data source`() = runTest {
        viewModel.setButtonEnabled(HomeButton.CAMERA, true)
        coVerify { homePrefs.setButtonEnabled(HomeButton.CAMERA, true) }
    }
}
