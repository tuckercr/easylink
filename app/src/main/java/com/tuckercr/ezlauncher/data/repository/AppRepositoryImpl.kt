package com.tuckercr.ezlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.tuckercr.ezlauncher.data.cache.AppListCache
import com.tuckercr.ezlauncher.domain.model.AppInfo
import com.tuckercr.ezlauncher.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AppRepository"

/**
 * Concrete implementation of [AppRepository].
 *
 * The installed-apps list is pre-warmed by [com.tuckercr.ezlauncher.data.cache.AppListInitializer]
 * (Jetpack Startup) before [android.app.Application.onCreate], so [getInstalledApps] returns
 * cached data with no perceptible delay. This class is responsible only for keeping
 * that cache fresh as packages are installed or removed.
 */
@OptIn(FlowPreview::class)
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // If the Startup initializer was disabled this would be required
        AppListCache.prewarm(context)

        // Re-query on Dispatchers.Default whenever packages change.
        // Debounce handles batch updates (e.g. Play Store updating multiple apps).
        packageChangeFlow()
            .debounce(1000.milliseconds)
            .onEach {
                Log.d(TAG, "debounce elapsed — triggering cache refresh")
                AppListCache.refresh(context)
            }.launchIn(scope)
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
    private fun packageChangeFlow(): Flow<Unit> =
        callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    Log.d(TAG, "package change received: action=${intent.action} pkg=${intent.data}")
                    trySend(Unit)
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            awaitClose { context.unregisterReceiver(receiver) }
        }
}
