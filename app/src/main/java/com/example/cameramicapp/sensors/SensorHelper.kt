package com.example.cameramicapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.biometric.BiometricManager

class SensorHelper(private val context: Context) {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val biometricManager = BiometricManager.from(context)

    data class SensorInfo(
        val name: String,
        val isAvailable: Boolean,
        val details: String
    )

    fun getAllAvailableSensors(): List<SensorInfo> {
        val sensorsList = mutableListOf<SensorInfo>()

        // Verificar sensor de proximidad
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensorsList.add(
            SensorInfo(
                name = "Sensor de Proximidad",
                isAvailable = proximitySensor != null,
                details = if (proximitySensor != null) {
                    "Rango: ${proximitySensor.maximumRange} cm, Resolución: ${proximitySensor.resolution}"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar sensor de acelerómetro
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorsList.add(
            SensorInfo(
                name = "Acelerómetro",
                isAvailable = accelerometerSensor != null,
                details = if (accelerometerSensor != null) {
                    "Rango: ±${accelerometerSensor.maximumRange} m/s², Resolución: ${accelerometerSensor.resolution}"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar giroscopio
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorsList.add(
            SensorInfo(
                name = "Giroscopio",
                isAvailable = gyroscopeSensor != null,
                details = if (gyroscopeSensor != null) {
                    "Rango: ±${gyroscopeSensor.maximumRange} rad/s, Resolución: ${gyroscopeSensor.resolution}"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar sensor de luz
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorsList.add(
            SensorInfo(
                name = "Sensor de Luz Ambiental",
                isAvailable = lightSensor != null,
                details = if (lightSensor != null) {
                    "Rango: 0-${lightSensor.maximumRange} lux"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar magnetómetro
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorsList.add(
            SensorInfo(
                name = "Magnetómetro",
                isAvailable = magnetometerSensor != null,
                details = if (magnetometerSensor != null) {
                    "Rango: ±${magnetometerSensor.maximumRange} μT"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar barómetro
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        sensorsList.add(
            SensorInfo(
                name = "Barómetro (Presión)",
                isAvailable = pressureSensor != null,
                details = if (pressureSensor != null) {
                    "Rango: 0-${pressureSensor.maximumRange} hPa"
                } else {
                    "No disponible en este dispositivo"
                }
            )
        )

        // Verificar autenticación biométrica
        val biometricAvailability = when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Disponible y configurada"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Sin hardware biométrico"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware no disponible"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Sin huellas registradas"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Requiere actualización de seguridad"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "No soportado"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Estado desconocido"
            else -> "Estado desconocido"
        }

        sensorsList.add(
            SensorInfo(
                name = "Autenticación Biométrica",
                isAvailable = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS,
                details = biometricAvailability
            )
        )

        return sensorsList
    }

    fun getProximitySensorInfo(): String {
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        return if (proximitySensor != null) {
            """
            Nombre: ${proximitySensor.name}
            Fabricante: ${proximitySensor.vendor}
            Versión: ${proximitySensor.version}
            Rango máximo: ${proximitySensor.maximumRange} cm
            Resolución: ${proximitySensor.resolution}
            Potencia: ${proximitySensor.power} mA
            """.trimIndent()
        } else {
            "Sensor de proximidad no disponible"
        }
    }

    fun getBiometricInfo(): String {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                "✅ Autenticación biométrica disponible\n" +
                        "Puedes usar tu huella dactilar para autenticarte"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "❌ Este dispositivo no tiene hardware biométrico"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "⚠️ Hardware biométrico temporalmente no disponible"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "⚙️ No hay huellas dactilares registradas\n" +
                        "Ve a Configuración > Seguridad para registrar tu huella"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "🔄 Se requiere una actualización de seguridad"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                "❌ Autenticación biométrica no soportada"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                "❓ Estado de autenticación biométrica desconocido"
            else ->
                "❓ Estado desconocido"
        }
    }
}