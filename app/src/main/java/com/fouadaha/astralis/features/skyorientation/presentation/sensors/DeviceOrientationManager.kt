package com.fouadaha.astralis.features.skyorientation.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import com.fouadaha.astralis.features.skyorientation.domain.DeviceOrientation

class DeviceOrientationManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var listener: ((DeviceOrientation) -> Unit)? = null

    fun startListening(onOrientationChanged: (DeviceOrientation) -> Unit) {
        listener = onOrientationChanged
        rotationVectorSensor?.let { //Verify if the sensor is available
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            Toast.makeText(
                context, "El sensor de rotación no está disponible", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix3x3 = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix3x3, event.values)

            val orientationAxes = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix3x3, orientationAxes)

            val azimuth = Math.toDegrees(orientationAxes[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationAxes[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAxes[2].toDouble()).toFloat()

            listener?.invoke(DeviceOrientation(azimuth, pitch, roll))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
