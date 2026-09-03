package com.applock.data

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active unlocked sessions for locked applications, including grace period timeout
 * and screen-off session resets.
 */
object UnlockSessionManager {

    private const val TAG = "UnlockSessionManager"

    data class AppSession(
        val unlockedAt: Long,
        var lastForegroundAt: Long,
        var backgroundedAt: Long? = null
    )

    private val sessions = ConcurrentHashMap<String, AppSession>()

    data class ExitCooldown(
        val exitTime: Long,
        val durationMs: Long
    )

    // Tracks when the user exited a locked app or lock screen.
    // Suppresses re-triggers during transition animations for durationMs.
    private val exitCooldowns = ConcurrentHashMap<String, ExitCooldown>()
    private const val MIN_EXIT_COOLDOWN_MS = 1000L

    /**
     * True if the AppLockActivity (lock screen) is currently visible or starting.
     * Prevents the background detector from locking the AppLock package itself when the lock screen is open.
     */
    @Volatile
    var isLockScreenVisible: Boolean = false

    /**
     * True after a successful Settings-mode authentication (Device Admin deactivation screen or
     * app-info page). Resets to false automatically after 30 seconds or when explicitly cleared.
     * Accessed from the accessibility service thread — must be @Volatile.
     */
    @Volatile
    var isSettingsUnlocked: Boolean = false

    /**
     * Checks if the given package currently has an active, non-expired unlocked session.
     *
     * @param packageName The application package name.
     * @param timeoutSeconds Grace period in seconds (0 = lock immediately on background).
     */
    fun isAppUnlocked(packageName: String, timeoutSeconds: Int): Boolean {
        val session = sessions[packageName] ?: return false
        val bgTime = session.backgroundedAt

        // If the app is currently in foreground or hasn't recorded background time yet
        if (bgTime == null) {
            return true
        }

        // If configured to lock immediately when leaving the app
        if (timeoutSeconds <= 0) {
            Log.d(TAG, "Session expired for $packageName (immediate relock configured)")
            sessions.remove(packageName)
            return false
        }

        val elapsedMillis = System.currentTimeMillis() - bgTime
        val elapsedSeconds = elapsedMillis / 1000

        return if (elapsedSeconds <= timeoutSeconds) {
            true
        } else {
            Log.d(TAG, "Session expired for $packageName ($elapsedSeconds s > $timeoutSeconds s grace period)")
            sessions.remove(packageName)
            false
        }
    }

    /**
     * Marks an app as unlocked upon successful authentication.
     */
    fun setAppUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        sessions[packageName] = AppSession(
            unlockedAt = now,
            lastForegroundAt = now,
            backgroundedAt = null
        )
        Log.i(TAG, "Unlocked session registered for: $packageName")
    }

    /**
     * Called when an app is detected actively in the foreground.
     */
    fun onAppForeground(packageName: String) {
        val session = sessions[packageName] ?: return
        session.lastForegroundAt = System.currentTimeMillis()
        // Only clear backgroundedAt if it was set more than 200ms ago.
        // This prevents rapid reopen from clearing backgroundedAt before isAppUnlocked checks it.
        val bgTime = session.backgroundedAt
        if (bgTime == null || System.currentTimeMillis() - bgTime > 200) {
            session.backgroundedAt = null
        }
    }

    /**
     * Called when an app transitions from foreground to background.
     * Starts an exit cooldown to suppress false re-triggers during trailing window events
     * if the app had an active unlocked session.
     */
    fun onAppBackgrounded(packageName: String, timeoutSeconds: Int = 0) {
        val session = sessions[packageName]
        if (session != null && session.backgroundedAt == null) {
            session.backgroundedAt = System.currentTimeMillis()
            Log.d(TAG, "App backgrounded: $packageName at ${session.backgroundedAt}")
            markExited(packageName, timeoutSeconds)
        }
    }

    /**
     * Clears the unlock session for a specific package (forces immediate re-authentication next time).
     */
    fun lockApp(packageName: String) {
        sessions.remove(packageName)
    }

    fun lockAppIfImmediate(packageName: String, timeoutSeconds: Int) {
        if (timeoutSeconds <= 0) {
            sessions.remove(packageName)
            Log.d(TAG, "Immediate lock: session removed for $packageName")
        }
    }

    /**
     * Returns true if a session exists for the package (regardless of background/foreground state).
     */
    fun hasSession(packageName: String): Boolean {
        return sessions.containsKey(packageName)
    }

    /**
     * Called when the user exits a locked app or lock screen.
     * Fixed transition cooldown of MIN_EXIT_COOLDOWN_MS (1000ms) to suppress transition animation re-triggers.
     */
    fun markExited(packageName: String, relockTimeoutSeconds: Int = 0) {
        val durationMs = MIN_EXIT_COOLDOWN_MS
        exitCooldowns[packageName] = ExitCooldown(System.currentTimeMillis(), durationMs)
        Log.d(TAG, "Exit cooldown started for: $packageName ($durationMs ms)")
    }

    /**
     * Returns true if the given package is within its exit cooldown window.
     */
    fun isInExitCooldown(packageName: String): Boolean {
        val cooldown = exitCooldowns[packageName] ?: return false
        return if (System.currentTimeMillis() - cooldown.exitTime < cooldown.durationMs) {
            true
        } else {
            exitCooldowns.remove(packageName)
            false
        }
    }

    /**
     * Clears all active unlocked sessions (e.g. on device screen-off / lock).
     */
    fun clearAllSessions() {
        val count = sessions.size
        sessions.clear()
        Log.i(TAG, "Cleared all unlock sessions ($count active sessions cleared)")
    }
}
