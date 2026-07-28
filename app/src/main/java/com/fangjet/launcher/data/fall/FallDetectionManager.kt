package com.fangjet.launcher.data.fall

import android.content.Context
import android.content.Intent
import android.os.Build
import com.fangjet.launcher.data.config.FeatureFlags
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops [FallDetectionService] from anywhere in the app
 * (ViewModel, BroadcastReceiver, etc.) without each caller needing
 * to know the API-level workaround for foreground services.
 */
@Singleton
class FallDetectionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val featureFlags: FeatureFlags,
) {
    fun start() {
        // The standard flavor never declares the service or its foreground
        // permissions — starting it would crash. Gating here covers every
        // caller (settings toggle, boot receiver, app-restart restore).
        if (!featureFlags.safetyFeatures) return
        val intent = Intent(context, FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop() {
        context.stopService(Intent(context, FallDetectionService::class.java))
    }
}
