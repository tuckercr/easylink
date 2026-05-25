package com.tuckercr.ezlauncher

import android.app.Application
import com.tuckercr.ezlauncher.data.alarm.ReminderNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EZLauncherApplication : Application() {

    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
    }
}
