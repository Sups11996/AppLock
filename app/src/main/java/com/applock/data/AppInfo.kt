package com.applock.data

import android.graphics.drawable.Drawable

/**
 * Represents an installed user-facing launcher application.
 *
 * @param packageName The unique Android package identifier (e.g. "com.whatsapp").
 * @param label The human-readable application name (e.g. "WhatsApp").
 * @param icon The app icon Drawable resolved from PackageManager.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
