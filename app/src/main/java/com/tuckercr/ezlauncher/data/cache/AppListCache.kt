package com.tuckercr.ezlauncher.data.cache

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.tuckercr.ezlauncher.data.cache.AppListCache.apps
import com.tuckercr.ezlauncher.data.cache.AppListCache.prewarm
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Hot state — emits an empty list immediately, then the real list once loaded. */
    internal val apps: MutableStateFlow<List<AppInfo>> = MutableStateFlow(emptyList())

    private val initialLoadStarted = AtomicBoolean(false)

    /**
     * Kicks off a package-manager query on [Dispatchers.Default].
     */
    internal fun refresh(context: Context) {
        scope.launch {
            apps.value = queryApps(context)
        }
    }

    /**
     * Kicks off the initial package-manager query on [Dispatchers.Default].
     * Safe to call multiple times — only the first call does any work.
     */
    internal fun prewarm(context: Context) {
        if (!initialLoadStarted.compareAndSet(false, true)) return
        refresh(context)
    }

    /**
     * Queries the package manager for all user-launchable apps, excluding
     * the host app itself and any other home-screen launchers.
     *
     * Performs a single query for all activities with ACTION_MAIN and filters
     * for CATEGORY_LAUNCHER and CATEGORY_HOME in memory for efficiency.
     */
    internal fun queryApps(context: Context): List<AppInfo> {
        val pm: PackageManager = context.packageManager

        // One query for all main activities. GET_RESOLVED_FILTER allows us to
        // check categories (Launcher vs Home) in memory.
        val allActivities = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN),
            PackageManager.GET_RESOLVED_FILTER,
        )

        val homePackages = mutableSetOf<String>()
        val candidates = mutableListOf<ResolveInfo>()

        for (ri in allActivities) {
            val filter = ri.filter ?: continue
            val pkg = ri.activityInfo.packageName
            if (filter.hasCategory(Intent.CATEGORY_HOME)) {
                homePackages.add(pkg)
            }
            if (filter.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                candidates.add(ri)
            }
        }

        return candidates
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                // Exclude this app and other launchers
                if (pkg == context.packageName || pkg in homePackages) return@mapNotNull null

                AppInfo(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                )
            }.distinctBy { it.packageName }
            .sorted()
    }
}
