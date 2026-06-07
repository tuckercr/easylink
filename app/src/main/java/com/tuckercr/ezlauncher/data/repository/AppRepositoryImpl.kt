package com.tuckercr.ezlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tuckercr.ezlauncher.data.cache.AppListCache
import com.tuckercr.ezlauncher.domain.model.AppInfo
import com.tuckercr.ezlauncher.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AppRepository].
 *
 * The installed-apps list is pre-warmed by [com.tuckercr.ezlauncher.data.cache.AppListInitializer]
 * (Jetpack Startup) before [android.app.Application.onCreate], so [getInstalledApps] returns
 * cached data with no perceptible delay. This class is responsible only for keeping
 * that cache fresh as packages are installed or removed.
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRepository {

    init {
        // If the Startup initializer was disabled this would be required
        AppListCache.prewarm(context)

        // Re-query on Dispatchers.Default whenever packages change.
        packageChangeFlow()
            .onEach { AppListCache.apps.value = AppListCache.queryApps(context) }
            .launchIn(AppListCache.scope)
    }

    override fun getInstalledApps(): Flow<List<AppInfo>> = AppListCache.apps

    override suspend fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    /**
     * Emits whenever a package is installed, removed, or replaced.
     */
    private fun packageChangeFlow(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
