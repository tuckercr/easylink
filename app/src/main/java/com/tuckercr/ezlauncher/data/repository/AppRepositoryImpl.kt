package com.tuckercr.ezlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.tuckercr.ezlauncher.domain.model.AppInfo
import com.tuckercr.ezlauncher.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AppRepository].
 *
 * Lives in the data layer — it is the only class that knows about:
 *   - [PackageManager] (installed apps)
 *   - [BroadcastReceiver] (package changes, battery)
 *   - [Context] (launching intents)
 *
 * The domain layer never imports this class. Hilt injects it wherever
 * the [AppRepository] interface is requested.
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRepository {

    private val packageManager: PackageManager get() = context.packageManager

    // ── Installed apps ────────────────────────────────────────────────────────

    override fun getInstalledApps(): Flow<List<AppInfo>> =
        merge(
            // Emit immediately on collection
            flow { emit(queryInstalledApps()) },
            // Re-emit whenever a package is added/removed/replaced
            packageChangeFlow(),
        )

    private fun packageChangeFlow(): Flow<List<AppInfo>> =
        callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    trySend(queryInstalledApps())
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

    private fun queryInstalledApps(): List<AppInfo> {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager
            .queryIntentActivities(launchIntent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                // Exclude ourselves — launchers shouldn't list themselves
                if (pkg == context.packageName) return@mapNotNull null
                AppInfo(
                    packageName = pkg,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }.sorted()
    }

    // ── App launching ─────────────────────────────────────────────────────────

    override suspend fun launchApp(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
