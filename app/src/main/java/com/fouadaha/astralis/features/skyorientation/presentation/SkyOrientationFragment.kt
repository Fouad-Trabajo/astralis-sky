package com.fouadaha.astralis.features.skyorientation.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fouadaha.astralis.databinding.FragmentSkyOrientationBinding
import com.fouadaha.astralis.features.skyorientation.presentation.sensors.DeviceOrientationManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class SkyOrientationFragment : Fragment() {

    private var _binding: FragmentSkyOrientationBinding? = null
    private val binding get() = _binding!!
    private lateinit var orientationManager: DeviceOrientationManager
    private val viewModel: CelestialBodiesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkyOrientationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orientationManager = DeviceOrientationManager(requireContext())
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            state.bodies?.let {
                binding.orbitView.updateBodies(it)
            }
        }
        orientationManager.startListening { orientation ->
            binding.orbitView.deviceOrientation = orientation
        }
        viewModel.getCelestialBodies()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orientationManager.stopListening()
        _binding = null
    }
}