package com.tuckercr.ezlauncher.presentation.main

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuckercr.ezlauncher.navigation.EzLauncherNavHost
import com.tuckercr.ezlauncher.ui.theme.EzLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestHomeRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* re-checked on next onResume */ }

    private val themeViewModel: ThemeViewModel by viewModels()

    /** Route to navigate to on startup, set via [EXTRA_NAVIGATE_TO] intent extra. */
    private var pendingNavTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This app is always dark — force light (white) status bar icons unconditionally.
        // The default SystemBarStyle.auto() reads the DayNight XML theme and flips to dark
        // icons when the system is in light mode, which is wrong for an always-dark app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        pendingNavTarget = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        setContent {
            val highContrast by themeViewModel.highContrastEnabled.collectAsStateWithLifecycle()
            EzLauncherTheme(highContrast = highContrast) {
                EzLauncherNavHost(
                    pendingNavTarget = pendingNavTarget,
                    onNavTargetConsumed = { pendingNavTarget = null },
                )
            }
        }
    }

    /**
     * Called when this Activity is already running and receives a new intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingNavTarget = intent.getStringExtra(EXTRA_NAVIGATE_TO)
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }

    override fun onResume() {
        super.onResume()
        if (!isDefaultHome()) {
            promptSetAsHome()
        }
    }

    private fun isDefaultHome(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName == packageName
        }

    private fun promptSetAsHome() {
        val dialog = AlertDialog
            .Builder(this)
            .setTitle("Set ClearHome as Home")
            .setMessage("Would you like to use ClearHome as your home screen?")
            .setPositiveButton("Set Now") { _, _ -> requestDefaultHome() }
            .setNegativeButton("Not Now") { d, _ -> d.dismiss() }
            .create()
        dialog.show()
        // Override the theme's accent colour so the buttons are readable on a dark background
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE)
    }

    private fun requestDefaultHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            requestHomeRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }
}
