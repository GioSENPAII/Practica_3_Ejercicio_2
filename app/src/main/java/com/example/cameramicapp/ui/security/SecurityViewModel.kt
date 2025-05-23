package com.example.cameramicapp.ui.security

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cameramicapp.sensors.AppSensorManager
import com.example.cameramicapp.sensors.AuthResult
import com.example.cameramicapp.sensors.BiometricAuthManager
import com.example.cameramicapp.sensors.BiometricAvailability
import com.example.cameramicapp.sensors.ProximityData

class SecurityViewModel(
    private val activity: FragmentActivity
) : ViewModel() {

    private val biometricAuthManager = BiometricAuthManager(activity)
    private val sensorManager = AppSensorManager(activity)

    // Datos de autenticación
    val isAuthenticated: LiveData<Boolean> = biometricAuthManager.isAuthenticated
    val authenticationResult: LiveData<AuthResult> = biometricAuthManager.authenticationResult

    // Datos de sensores
    val proximityData: LiveData<ProximityData> = sensorManager.proximityData

    private val _isProximityTesting = MutableLiveData<Boolean>()
    val isProximityTesting: LiveData<Boolean> = _isProximityTesting

    init {
        _isProximityTesting.value = false
    }

    fun checkBiometricAvailability(): BiometricAvailability {
        return biometricAuthManager.checkBiometricAvailability()
    }

    fun authenticateUser() {
        biometricAuthManager.authenticateUser(
            title = "Acceso a funciones avanzadas",
            subtitle = "Usa tu huella dactilar para acceder a las funciones de sensores",
            negativeButtonText = "Cancelar"
        )
    }

    fun logout() {
        biometricAuthManager.logout()
        stopProximityListening()
        _isProximityTesting.value = false
    }

    fun toggleProximityTesting() {
        val currentState = _isProximityTesting.value ?: false
        if (currentState) {
            stopProximityListening()
            _isProximityTesting.value = false
        } else {
            startProximityListening()
            _isProximityTesting.value = true
        }
    }

    fun startProximityListening() {
        if (sensorManager.isProximitySensorAvailable()) {
            sensorManager.startProximityListening()
        }
    }

    fun stopProximityListening() {
        sensorManager.stopProximityListening()
    }

    fun stopAllSensors() {
        sensorManager.stopAllSensors()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllSensors()
    }
}

class SecurityViewModelFactory(
    private val activity: FragmentActivity
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(activity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}