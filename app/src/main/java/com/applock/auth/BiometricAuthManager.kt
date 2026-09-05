package com.applock.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Result of a biometric authentication attempt.
 */
sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data class Failed(val message: String = "Authentication failed") : BiometricAuthResult
    data class Error(val errorCode: Int, val errString: String) : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
}

/**
 * Wrapper for Android's BiometricPrompt and BiometricManager.
 */
class BiometricAuthManager(private val context: Context) {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Checks if biometric or device credential authentication is available on the device.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Displays the BiometricPrompt dialog to the user.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(BiometricAuthResult.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(BiometricAuthResult.Failed("Authentication failed. Please try again."))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            onResult(BiometricAuthResult.Cancelled)
                        }
                        else -> {
                            onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
                        }
                    }
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
