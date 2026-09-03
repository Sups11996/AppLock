package com.applock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "applock_preferences")

/**
 * Repository layer for managing the local persistence of locked applications and settings using Jetpack DataStore Preferences.
 */
class LockedAppsRepository(private val context: Context) {

    companion object {
        val KEY_LOCKED_PACKAGES = stringSetPreferencesKey("locked_packages")
        val KEY_RELOCK_TIMEOUT_SECONDS = intPreferencesKey("relock_timeout_seconds")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_SELF_LOCK_TIMEOUT = intPreferencesKey("self_lock_timeout")
        const val DEFAULT_RELOCK_TIMEOUT_SECONDS = 0
        const val SELF_LOCK_SAME_AS_APPS = Int.MIN_VALUE
    }

    /**
     * Continuous reactive stream of service enabled preference.
     */
    val isServiceEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SERVICE_ENABLED] ?: false
        }

    /**
     * Set the service enabled preference state.
     */
    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ENABLED] = enabled
        }
    }

    /**
     * Synchronously read if the service is configured to be enabled.
     */
    suspend fun isServiceEnabled(): Boolean {
        return isServiceEnabledFlow.first()
    }

    /**
     * Continuous reactive stream of relock timeout in seconds (e.g. 0 for immediate, 30 for 30s).
     */
    val relockTimeoutSecondsFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_RELOCK_TIMEOUT_SECONDS] ?: DEFAULT_RELOCK_TIMEOUT_SECONDS
        }

    /**
     * Set the relock timeout in seconds.
     */
    suspend fun setRelockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RELOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    /**
     * Continuous reactive stream of locked package names.
     */
    val lockedAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LOCKED_PACKAGES] ?: emptySet()
        }

    /**
     * Set the locked state of a package.
     *
     * @param packageName The package name of the app to lock or unlock.
     * @param isLocked True to lock, false to unlock.
     */
    suspend fun setAppLocked(packageName: String, isLocked: Boolean) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[KEY_LOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (isLocked) {
                currentSet.add(packageName)
            } else {
                currentSet.remove(packageName)
            }
            preferences[KEY_LOCKED_PACKAGES] = currentSet
        }
    }

    /**
     * Check if a specific package is currently locked.
     */
    suspend fun isAppLocked(packageName: String): Boolean {
        return packageName in getLockedApps()
    }

    /**
     * Synchronously read the current set of locked apps.
     */
    suspend fun getLockedApps(): Set<String> {
        return lockedAppsFlow.first()
    }

    /**
     * Returns true if the user has already completed the onboarding flow.
     */
    suspend fun isOnboardingComplete(): Boolean {
        return context.dataStore.data.first()[KEY_ONBOARDING_COMPLETE] ?: false
    }

    /**
     * Marks the onboarding flow as completed.
     */
    suspend fun setOnboardingComplete() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] = true
        }
    }

    /**
     * Continuous reactive stream of self-lock timeout in seconds (-1 for immediate, 0 for never, >0 for N seconds).
     */
    val selfLockTimeoutFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SELF_LOCK_TIMEOUT] ?: Int.MIN_VALUE
        }

    /**
     * Set the self-lock timeout in seconds.
     */
    suspend fun setSelfLockTimeout(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELF_LOCK_TIMEOUT] = seconds
        }
    }
}
