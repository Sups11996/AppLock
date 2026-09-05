package com.applock.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Utility functions for checking and requesting system permissions such as Usage Access.
 */
object PermissionUtils {

    /**
     * Checks whether PACKAGE_USAGE_STATS permission has been granted via system Settings.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks whether the "Draw over other apps" (SYSTEM_ALERT_WINDOW) permission is granted.
     * This is required on Android 10+ for AppLockActivity to appear over other apps
     * when launched from a background service.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not required below Android 6.0
        }
    }

    /**
     * Creates an Intent to navigate to the "Draw over other apps" settings page for this app.
     */
    fun getOverlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Creates an Intent to navigate directly to Usage Access settings.
     * Attempts deep-linking with package URI first, falling back to general Usage Access settings.
     */
    fun getUsageAccessSettingsIntent(context: Context): Intent {
        val directIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (directIntent.resolveActivity(context.packageManager) != null) {
            directIntent
        } else {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Checks whether AppLockAccessibilityService is enabled in system Accessibility Settings.
     */
    fun hasAccessibilityPermission(context: Context): Boolean {
        return com.applock.service.AppLockAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    /**
     * Creates an Intent to navigate to system Accessibility Settings.
     */
    fun getAccessibilitySettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
