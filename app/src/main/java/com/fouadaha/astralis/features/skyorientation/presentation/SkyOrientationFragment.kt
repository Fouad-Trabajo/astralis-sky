package com.fouadaha.astralis.features.skyorientation.presentation

import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fouadaha.astralis.databinding.FragmentSkyOrientationBinding

class SkyOrientationFragment : Fragment(), SensorEventListener {

    //private val skyViewModel: SkyViewModel by viewModel()
    private var _binding: FragmentSkyOrientationBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    //private lateinit var gyroListener: SensorEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkyOrientationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //skyViewModel.getSkyOrientation()
        orientationSensors()
        sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun orientationSensors() {
        //sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        binding.apply {
//            ejeX.text
//            ejeY
//            ejeZ
//        }
        //setRequestOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent) {

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        binding.apply {
            ejeX.text = "$x"
            ejeY.text = "$y"
            ejeZ.text = "$z"
        }
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}