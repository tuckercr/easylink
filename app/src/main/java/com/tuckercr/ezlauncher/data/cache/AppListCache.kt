package com.tuckercr.ezlauncher.data.cache

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.tuckercr.ezlauncher.domain.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-lifetime singleton that holds the pre-warmed installed-apps list.
 *
 * [prewarm] is called by [AppListInitializer] (via Jetpack Startup) before
 * [Application.onCreate], so the list is ready — or nearly so — by the time
 * the user can tap "All Apps". [AppRepositoryImpl] also calls [prewarm] as a
 * fallback (the [AtomicBoolean] guard makes the second call a no-op).
 *
 * [apps] is the single source of truth. [AppRepositoryImpl] exposes it as a
 * [kotlinx.coroutines.flow.Flow] and updates it whenever packages are installed
 * or removed.
 */
internal object AppListCache {

    /** Application-lifetime scope for background work. */
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Hot state — emits an empty list immediately, then the real list once loaded. */
    internal val apps: MutableStateFlow<List<AppInfo>> = MutableStateFlow(emptyList())

    private val initialLoadStarted = AtomicBoolean(false)

    /**
     * Kicks off the initial package-manager query on [Dispatchers.Default].
     * Safe to call multiple times — only the first call does any work.
     */
    internal fun prewarm(context: Context) {
        if (!initialLoadStarted.compareAndSet(false, true)) return
        scope.launch {
            apps.value = queryApps(context)
        }
    }

    /**
     * Queries the package manager for all user-launchable apps, excluding
     * the host app itself and any other home-screen launchers.
     */
    internal fun queryApps(context: Context): List<AppInfo> {
        val pm: PackageManager = context.packageManager

        val launcherPackages: Set<String> = pm
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0,
            )
            .map { it.activityInfo.packageName }
            .toSet()

        return pm
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
            )
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                if (pkg in launcherPackages) return@mapNotNull null
                AppInfo(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                )
            }
            .distinctBy { it.packageName }
            .sorted()
    }
}
