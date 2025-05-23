package com.example.cameramicapp.sensors

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BiometricAuthManager(private val activity: FragmentActivity) {

    private val biometricManager = BiometricManager.from(activity)

    private val _authenticationResult = MutableLiveData<AuthResult>()
    val authenticationResult: LiveData<AuthResult> = _authenticationResult

    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    init {
        _isAuthenticated.value = false
    }

    fun checkBiometricAvailability(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }

    fun authenticateUser(
        title: String = "Autenticación biométrica",
        subtitle: String = "Usa tu huella dactilar para autenticarte",
        negativeButtonText: String = "Cancelar"
    ) {
        if (checkBiometricAvailability() != BiometricAvailability.AVAILABLE) {
            _authenticationResult.postValue(
                AuthResult.Error("Autenticación biométrica no disponible")
            )
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                _isAuthenticated.postValue(false)
                _authenticationResult.postValue(
                    AuthResult.Error("Error de autenticación: $errString")
                )
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isAuthenticated.postValue(true)
                _authenticationResult.postValue(
                    AuthResult.Success("Autenticación exitosa")
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                _authenticationResult.postValue(
                    AuthResult.Failed("Autenticación fallida. Inténtalo de nuevo.")
                )
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun logout() {
        _isAuthenticated.postValue(false)
        _authenticationResult.postValue(AuthResult.LoggedOut)
    }
}

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

sealed class AuthResult {
    data class Success(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class Failed(val message: String) : AuthResult()
    object LoggedOut : AuthResult()
}