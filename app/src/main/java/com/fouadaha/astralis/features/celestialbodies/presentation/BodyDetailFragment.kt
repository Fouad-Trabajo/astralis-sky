package com.fouadaha.astralis.features.celestialbodies.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.fouadaha.astralis.R
import com.fouadaha.astralis.databinding.DialogCharacteristicsBodyBinding
import com.fouadaha.astralis.databinding.FragmentBodyDetailBinding
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class BodyDetailFragment : Fragment() {

    private val bodyDetailViewModel: BodyDetailViewModel by viewModel()
    private var _binding: FragmentBodyDetailBinding? = null
    private var _dialogBinding: DialogCharacteristicsBodyBinding? = null
    private val dialogBinding get() = _dialogBinding!!
    private val binding get() = _binding!!

    private val arguments: BodyDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBodyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
        val bodyId = arguments.bodyId
        bodyDetailViewModel.getBody(bodyId)
    }

    private fun setupObserver() {
        val bodyObserver = Observer<BodyDetailViewModel.UiState> { uiState ->
            bindData(uiState.body)
        }
        bodyDetailViewModel.uiState.observe(viewLifecycleOwner, bodyObserver)
    }

    private fun bindData(body: CelestialBody?) {
        body?.let {
            binding.apply {
                // Toolbar
                detailHeader.topAppBar.title = it.name
                detailHeader.topAppBar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
                // Body detail
                imageBodyDetail.load(body.imageUrl)
                descriptionBodyDetail.text = body.description
                characteristicsButton.setOnClickListener {
                    characteristicsDialog(body)
                }
            }
        }
    }

    private fun characteristicsDialog(body: CelestialBody) {
        _dialogBinding = DialogCharacteristicsBodyBinding.inflate(LayoutInflater.from(context))

        dialogBinding.apply { // Celestial body characteristics
            bodyMass.text = getString(R.string.mass, body.characteristics.mass)
            bodyDensity.text = getString(R.string.density, body.characteristics.density)
            bodyTemperature.text = getString(R.string.temperature, body.characteristics.temperature)
            bodyRadius.text = getString(R.string.radius, body.characteristics.radius)
            bodyGravity.text = getString(R.string.gravity, body.characteristics.gravity)
            bodyType.text = getString(
                R.string.celestial_body_type,
                body.characteristics.celestialBodyType.toString()
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(R.string.characteristics_dialog)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogBinding = null
    }
}
