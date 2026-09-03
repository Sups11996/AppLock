package com.applock.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages querying and filtering installed launcher applications.
 */
class AppManager(private val context: Context) {

    /**
     * Queries all installed applications that have a launcher activity (i.e. appear on the home screen / app drawer).
     * Excludes non-launchable system components and this AppLock application itself.
     *
     * @return List of [AppInfo] sorted alphabetically by label (case-insensitive).
     */
    suspend fun getInstalledLauncherApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Query all activities matching the launcher intent
        val resolveInfos = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )

        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName

            // Skip our own AppLock app to prevent accidental self-lockout
            if (packageName == context.packageName) continue

            // Deduplicate if an app registers multiple launcher activities
            if (seenPackages.add(packageName)) {
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim() ?: packageName
                val icon = resolveInfo.loadIcon(packageManager)
                appList.add(
                    AppInfo(
                        packageName = packageName,
                        label = label,
                        icon = icon
                    )
                )
            }
        }

        // Sort alphabetically by app name
        appList.sortedBy { it.label.lowercase() }
    }
}
