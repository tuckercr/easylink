package com.fangjet.launcher.data.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class HomeScreenCheckSchedulerTest {

    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private lateinit var scheduler: HomeScreenCheckScheduler

    @Before
    fun setup() {
        every { context.getSystemService(AlarmManager::class.java) } returns alarmManager
        scheduler = HomeScreenCheckScheduler(context)
    }

    @Test
    fun `schedule calls setInexactRepeating with RTC_WAKEUP and INTERVAL_DAY`() {
        scheduler.schedule()

        verify {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                any(),
                AlarmManager.INTERVAL_DAY,
                any(),
            )
        }
    }

    @Test
    fun `schedule trigger time is in the future`() {
        val now = System.currentTimeMillis()

        scheduler.schedule()

        verify {
            alarmManager.setInexactRepeating(
                any(),
                more(now), // trigger must be after now
                any(),
                any(),
            )
        }
    }

    @Test
    fun `cancel calls alarmManager cancel`() {
        scheduler.cancel()

        // PendingIntent.getBroadcast returns null in unit tests (returnDefaultValues = true)
        verify { alarmManager.cancel(isNull<PendingIntent>()) }
    }
}
