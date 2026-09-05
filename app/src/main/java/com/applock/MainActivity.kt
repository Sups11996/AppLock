package com.applock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.applock.auth.BiometricAuthManager
import com.applock.auth.BiometricAuthResult
import com.applock.data.LockedAppsRepository
import com.applock.data.UnlockSessionManager
import com.applock.service.AppLockAccessibilityService
import com.applock.ui.lock.AppLockActivity
import com.applock.ui.screens.AppListScreen
import com.applock.ui.screens.AppListUiState
import com.applock.ui.screens.AppListViewModel
import com.applock.ui.theme.AppLockTheme

class MainActivity : FragmentActivity() {

    private val viewModel: AppListViewModel by viewModels()
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            AppLockTheme {
                val uiState by viewModel.uiState.collectAsState()
                AppListScreen(
                    uiState = uiState,
                    onToggleLock = { packageName, isLocked ->
                        viewModel.toggleAppLock(packageName, isLocked)
                    },
                    onRequestUnlock = { packageName ->
                        biometricAuthManager.authenticate(
                            activity = this@MainActivity,
                            title = "Confirm to unlock",
                            subtitle = "Authenticate to remove lock from this app"
                        ) { result ->
                            if (result is BiometricAuthResult.Success) {
                                viewModel.toggleAppLock(packageName, false)
                            }
                        }
                    },
                    onSelectRelockTimeout = { timeoutSeconds ->
                        viewModel.setRelockTimeout(timeoutSeconds)
                    },
                    onSelectSelfLockTimeout = { seconds ->
                        viewModel.setSelfLockTimeout(seconds)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        val effectiveTimeout = resolveEffectiveSelfLockTimeout()
        if (!UnlockSessionManager.isAppUnlocked(packageName, effectiveTimeout, this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            AppLockActivity.launch(this, packageName)
            return
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStop() {
        super.onStop()
        if (!UnlockSessionManager.isLockScreenVisible) {
            val effectiveTimeout = resolveEffectiveSelfLockTimeout()
            UnlockSessionManager.lockAppIfImmediate(packageName, effectiveTimeout)
            AppLockAccessibilityService.recentlyBackgroundedPackages[packageName] = System.currentTimeMillis()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            if (UnlockSessionManager.isAppUnlocked(packageName, resolveEffectiveSelfLockTimeout(), this)) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun resolveEffectiveSelfLockTimeout(): Int {
        val state = viewModel.uiState.value
        val selfLockTimeout = if (state is AppListUiState.Success) state.selfLockTimeoutSeconds
                              else Int.MIN_VALUE
        val relockTimeout = if (state is AppListUiState.Success) state.relockTimeoutSeconds
                            else Int.MAX_VALUE
        return when (selfLockTimeout) {
            Int.MIN_VALUE -> if (relockTimeout <= 0) 0 else relockTimeout
            -1 -> 0
            0 -> Int.MAX_VALUE
            else -> selfLockTimeout
        }
    }
}
