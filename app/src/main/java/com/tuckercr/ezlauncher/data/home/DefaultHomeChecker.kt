package com.tuckercr.ezlauncher.data.home

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
            context
                .getSystemService(RoleManager::class.java)
                .isRoleHeld(RoleManager.ROLE_HOME)
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            context.packageManager
                .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName == context.packageName
        }
}
