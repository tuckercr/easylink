package com.fangjet.launcher.data.home

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class HomeScreenCheckReceiverTest {

    private val notificationHelper: HomeScreenNotificationHelper = mockk(relaxed = true)
    private val defaultHomeChecker: DefaultHomeChecker = mockk()

    // Directly instantiate and set @Inject fields.
    // We call handleAlarm() instead of onReceive() to bypass the Hilt_HomeScreenCheckReceiver
    // wrapper, which requires a live Application context to resolve the Hilt component.
    private lateinit var receiver: HomeScreenCheckReceiver

    @Before
    fun setup() {
        receiver = HomeScreenCheckReceiver().also {
            it.notificationHelper = notificationHelper
            it.defaultHomeChecker = defaultHomeChecker
        }
    }

    @Test
    fun `when not default home shows notification`() {
        every { defaultHomeChecker.isDefault() } returns false

        receiver.handleAlarm()

        verify { notificationHelper.showNotification() }
        verify(exactly = 0) { notificationHelper.cancel() }
    }

    @Test
    fun `when already default home cancels any pending notification`() {
        every { defaultHomeChecker.isDefault() } returns true

        receiver.handleAlarm()

        verify { notificationHelper.cancel() }
        verify(exactly = 0) { notificationHelper.showNotification() }
    }
}
