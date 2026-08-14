package com.fangjet.launcher.data.apps

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The Facebook app — its home button only exists on devices that have it. */
const val FACEBOOK_PACKAGE = "com.facebook.katana"

/**
 * Cheap synchronous "is this app installed?" check, used to gate home buttons
 * that launch a specific third-party app. Checked at emission time rather than
 * observed reactively: an install/uninstall mid-session is picked up on the
 * next preferences emission or app start, which is close enough for a launcher
 * button, and a launch attempt on a just-uninstalled app simply no-ops.
 */
@Singleton
class InstalledAppChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }.getOrDefault(false)
}
