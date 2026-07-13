package com.tuckercr.ezlauncher.presentation.main

import android.Manifest
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
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.data.home.DefaultHomeChecker
import com.tuckercr.ezlauncher.data.home.HomeScreenNotificationHelper
import com.tuckercr.ezlauncher.navigation.EzLauncherNavHost
import com.tuckercr.ezlauncher.ui.theme.EzLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var homeScreenNotifHelper: HomeScreenNotificationHelper

    @Inject
    lateinit var defaultHomeChecker: DefaultHomeChecker

    private val requestHomeRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* re-checked on next onResume */ }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* showNotification() re-checks permission before posting */ }

    private val themeViewModel: ThemeViewModel by viewModels()

    /** Route to navigate to on startup, set via [EXTRA_NAVIGATE_TO] intent extra. */
    private var pendingNavTarget by mutableStateOf<String?>(null)

    /** Currently-visible "set as home" prompt, if any — prevents stacking a new one on every resume. */
    private var homeDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This app is always dark — force light (white) status bar icons unconditionally.
        // The default SystemBarStyle.auto() reads the DayNight XML theme and flips to dark
        // icons when the system is in light mode, which is wrong for an always-dark app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        pendingNavTarget = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        requestNotificationPermissionIfNeeded()
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
        // Only overwrite when the new intent actually carries a target — otherwise an
        // unrelated intent (e.g. the home-reminder notification tap) would silently
        // clobber a navigation target set by another in-flight intent (e.g. a fall alert).
        intent.getStringExtra(EXTRA_NAVIGATE_TO)?.let { pendingNavTarget = it }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }

    override fun onResume() {
        super.onResume()
        if (defaultHomeChecker.isDefault()) {
            // Dismiss any pending reminder notification — the user already set us as home
            homeScreenNotifHelper.cancel()
        } else {
            promptSetAsHome()
        }
    }

    override fun onPause() {
        super.onPause()
        homeDialog?.dismiss()
        homeDialog = null
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun promptSetAsHome() {
        if (homeDialog?.isShowing == true) return
        val dialog = AlertDialog
            .Builder(this)
            .setTitle(getString(R.string.home_dialog_title))
            .setMessage(getString(R.string.home_dialog_message))
            .setPositiveButton(getString(R.string.home_dialog_positive)) { _, _ -> requestDefaultHome() }
            .setNegativeButton(getString(R.string.home_dialog_negative)) { d, _ -> d.dismiss() }
            .create()
        homeDialog = dialog
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
