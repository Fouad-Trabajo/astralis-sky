package com.fouadaha.astralis.features.celestialobodies.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import coil.load
import com.fouadaha.astralis.databinding.DialogCharacteristicsBodyBinding
import com.fouadaha.astralis.databinding.FragmentBodyDetailBinding
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class BodyDetailFragment : Fragment() {

    private val bodyDetailViewModel: BodyDetailViewModel by viewModel()
    private var _binding: FragmentBodyDetailBinding? = null
    private val binding get() = _binding!!

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
        arguments?.getString("bodyId")?.let { bodyDetailViewModel.getBody(it) }
    }

    private fun setupObserver() {
        val bodyObserver = Observer<BodyDetailViewModel.UiState> { uiState ->
            // checkLoading(uiState.isLoading)
            // checkError(uiState.errorApp)
            bindData(uiState.body)
        }
        bodyDetailViewModel.uiState.observe(viewLifecycleOwner, bodyObserver)
    }

    private fun bindData(body: CelestialBody?) {
        body?.let {
            binding.apply {
                imageBodyDetail.load(body.imageUrl)
                descriptionBodyDetail.text = body.description
                characteristicsButton.setOnClickListener {
                    showCharacteristicsDialog(body)
                }
                detailHeader.topAppBar.title = it.name

                detailHeader.topAppBar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun showCharacteristicsDialog(body: CelestialBody) {
        // Inflar el layout del dialog usando ViewBinding
        val dialogBinding = DialogCharacteristicsBodyBinding.inflate(LayoutInflater.from(context))

        // Establecer los valores dinámicamente con Binding
        dialogBinding.bodyDensity.text = "Densidad: ${body.characteristics.density}"
        dialogBinding.bodyTemperature.text = "Temperatura: ${body.characteristics.temperature}"

        // Crear y mostrar el AlertDialog con ViewBinding
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Características del Cuerpo Celeste")
            .setPositiveButton("Cerrar", null) // Botón para cerrar el diálogo
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
