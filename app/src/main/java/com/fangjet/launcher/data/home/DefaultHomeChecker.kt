package com.fangjet.launcher.data.home

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHomeChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun isDefault(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Some OEM ROMs return a null RoleManager before the role subsystem finishes
            // initializing post-OTA — fall back to "not default" rather than crashing.
            context
                .getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) ?: false
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            context.packageManager
                .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName == context.packageName
        }

    /**
     * User-visible label of whichever app is currently the default home
     * (e.g. "Pixel Launcher"), or null when no default is set — the system
     * resolver stub ("android") answers in that case.
     */
    fun defaultHomeLabel(): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolved = context.packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null
        if (resolved.activityInfo?.packageName == "android") return null
        return resolved.loadLabel(context.packageManager)?.toString()
    }
}
