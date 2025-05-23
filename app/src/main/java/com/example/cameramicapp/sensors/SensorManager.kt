package com.example.cameramicapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AppSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // LiveData para observar cambios en los sensores
    private val _proximityData = MutableLiveData<ProximityData>()
    val proximityData: LiveData<ProximityData> = _proximityData

    private val _isNear = MutableLiveData<Boolean>()
    val isNear: LiveData<Boolean> = _isNear

    // Estado interno
    private var isProximityListening = false

    init {
        // Verificar si los sensores están disponibles
        checkSensorAvailability()
    }

    private fun checkSensorAvailability() {
        Log.d(TAG, "Verificando disponibilidad de sensores:")
        Log.d(TAG, "Sensor de proximidad: ${if (proximitySensor != null) "Disponible" else "No disponible"}")

        proximitySensor?.let { sensor ->
            Log.d(TAG, "Rango máximo del sensor de proximidad: ${sensor.maximumRange} cm")
            Log.d(TAG, "Resolución: ${sensor.resolution}")
        }
    }

    fun startProximityListening() {
        if (!isProximityListening && proximitySensor != null) {
            sensorManager.registerListener(
                this,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isProximityListening = true
            Log.d(TAG, "Sensor de proximidad iniciado")
        }
    }

    fun stopProximityListening() {
        if (isProximityListening) {
            sensorManager.unregisterListener(this, proximitySensor)
            isProximityListening = false
            Log.d(TAG, "Sensor de proximidad detenido")
        }
    }

    fun stopAllSensors() {
        sensorManager.unregisterListener(this)
        isProximityListening = false
        Log.d(TAG, "Todos los sensores detenidos")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val distance = sensorEvent.values[0]
                    val maxRange = sensorEvent.sensor.maximumRange
                    val isNearObject = distance < maxRange

                    val proximityData = ProximityData(
                        distance = distance,
                        maxRange = maxRange,
                        isNear = isNearObject,
                        timestamp = System.currentTimeMillis()
                    )

                    _proximityData.postValue(proximityData)
                    _isNear.postValue(isNearObject)

                    Log.d(TAG, "Proximidad: ${distance}cm, Cerca: $isNearObject")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión del sensor ${sensor?.name} cambió a: $accuracy")
    }

    fun isProximitySensorAvailable(): Boolean = proximitySensor != null

    companion object {
        private const val TAG = "AppSensorManager"
    }
}

data class ProximityData(
    val distance: Float,
    val maxRange: Float,
    val isNear: Boolean,
    val timestamp: Long
)