package com.fangjet.launcher.data.home

import android.app.NotificationManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class HomeScreenNotificationHelperTest {

    private val notifManager: NotificationManager = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private lateinit var helper: HomeScreenNotificationHelper

    @Before
    fun setup() {
        every { context.getSystemService(NotificationManager::class.java) } returns notifManager
        helper = HomeScreenNotificationHelper(context)
    }

    @Test
    fun `createChannel creates a notification channel`() {
        helper.createChannel()

        verify { notifManager.createNotificationChannel(any()) }
    }

    @Test
    fun `cancel dismisses the home reminder notification by id`() {
        helper.cancel()

        verify { notifManager.cancel(NOTIF_ID_HOME_REMINDER) }
    }
}
