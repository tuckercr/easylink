package com.tuckercr.ezlauncher.presentation.main

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.tuckercr.ezlauncher.navigation.EzLauncherNavHost
import com.tuckercr.ezlauncher.ui.theme.EzLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestHomeRole = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* re-checked on next onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EzLauncherTheme {
                EzLauncherNavHost()
            }
        }
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
        AlertDialog
            .Builder(this)
            .setTitle("Set EZ Launcher as Home")
            .setMessage("Would you like to use EZ Launcher as your home screen?")
            .setPositiveButton("Set Now") { _, _ -> requestDefaultHome() }
            .setNegativeButton("Not Now") { dialog, _ -> dialog.dismiss() }
            .show()
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
