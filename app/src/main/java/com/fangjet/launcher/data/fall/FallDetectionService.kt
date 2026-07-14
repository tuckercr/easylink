package com.fangjet.launcher.data.fall

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.fangjet.launcher.data.preferences.FallDetectionPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FallDetectionService"

/**
 * Foreground service that monitors the accelerometer and fires a fall alert
 * when the [FallDetector] state machine confirms a fall event.
 *
 * Lifecycle:
 *  - Started by [FallDetectionManager] whenever the preference is enabled.
 *  - Uses START_STICKY so Android restarts it automatically if killed.
 *  - Stopped by [FallDetectionManager] when the preference is disabled.
 *  - Also restarted after reboot by [BootCompletedReceiver].
 *
 * Uses [LifecycleService] so we can use lifecycleScope for coroutines.
 */
@AndroidEntryPoint
class FallDetectionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var detector: FallDetector

    @Inject
    lateinit var notifHelper: FallDetectionNotificationHelper

    @Inject
    lateinit var fallPrefs: FallDetectionPreferences

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Guard: only fire one alert at a time until the user dismisses
    private var alertInFlight = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Prefer linear acceleration (gravity removed) for cleaner detection,
        // fall back to raw accelerometer if unavailable.
        accelerometer =
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer found — service stopping.")
            stopSelf()
            return
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        // Promote to foreground immediately to satisfy Android's 5-second rule
        startForeground(NOTIF_ID_FALL_SERVICE, notifHelper.buildServiceNotification())

        // Sync sensitivity from DataStore whenever it changes
        scope.launch {
            fallPrefs.sensitivity.collect { sens ->
                detector.sensitivity = sens
            }
        }

        // Register accelerometer listener
        accelerometer?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_GAME, // ~50 Hz — good for fall detection
            )
        }

        Log.i(TAG, "Fall detection started (sensitivity=${detector.sensitivity})")
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(sensorListener)
        scope.cancel()
        Log.i(TAG, "Fall detection stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    // ── Sensor listener ───────────────────────────────────────────────────────

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val v = event.values
            if (!alertInFlight && detector.process(v[0], v[1], v[2])) {
                Log.i(TAG, "Fall detected! Firing alert.")
                alertInFlight = true
                notifHelper.showFallAlert()
                // alertInFlight is cleared when FallAlertActivity sends the
                // RESET_GUARD action back, or we reset after 60 s to avoid
                // getting permanently stuck.
                android.os.Handler(mainLooper).postDelayed({ alertInFlight = false }, 60_000)
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit
    }
}
