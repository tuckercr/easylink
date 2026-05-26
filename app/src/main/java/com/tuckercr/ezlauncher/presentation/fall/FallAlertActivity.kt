package com.tuckercr.ezlauncher.presentation.fall

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tuckercr.ezlauncher.ui.theme.EzLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen activity displayed when a fall is detected.
 *
 * Launched via a full-screen notification intent from [FallDetectionService],
 * so it appears even when the device is locked or the screen is off.
 *
 * Design notes for an elderly-user launcher:
 *  - Red background, massive text — impossible to miss
 *  - 30-second countdown before auto-calling the primary emergency contact
 *  - Single "I'm OK" green button to cancel; "Call Now" red button to skip
 */
@AndroidEntryPoint
class FallAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show above the keyguard and wake the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )

        setContent {
            EzLauncherTheme {
                FallAlertScreen(onDismiss = { finish() })
            }
        }
    }
}
