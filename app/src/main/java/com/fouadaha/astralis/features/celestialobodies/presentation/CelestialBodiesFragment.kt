package com.fouadaha.astralis.features.celestialobodies.presentation


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fouadaha.astralis.R
import com.fouadaha.astralis.databinding.DialogFiltersBinding
import com.fouadaha.astralis.databinding.FragmentCelestialBodiesBinding
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBodyType
import com.fouadaha.astralis.features.celestialobodies.presentation.adapter.CelestialBodiesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class CelestialBodiesFragment : Fragment() {

    private val celestialBodiesViewModel: CelestialBodiesViewModel by viewModel()

    private var _binding: FragmentCelestialBodiesBinding? = null
    private var _dialogBinding: DialogFiltersBinding? = null
    private val binding get() = _binding!!
    private val dialogBinding get() = _dialogBinding!!

    private val celestialBodiesAdapter = CelestialBodiesAdapter()
    private var allBodies: List<CelestialBody> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCelestialBodiesBinding.inflate(inflater, container, false)
        setupView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
        celestialBodiesViewModel.getCelestialBodies()
    }

    private fun setupObserver() {
        val bodyObserver = Observer<CelestialBodiesViewModel.UiState> { uiState ->
            //checkLoading(uiState.isLoading)
            //checkError(uiState.errorApp)
            bindData(uiState.bodies)
        }
        celestialBodiesViewModel.uiState.observe(viewLifecycleOwner, bodyObserver)
    }

    private fun setupView() {
        binding.apply {
            listCelestialBodies.layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false
            )
            listCelestialBodies.adapter = celestialBodiesAdapter
            mainHeader.topAppBar.title = "Astros"
        }
        celestialBodiesAdapter.setEvent { body ->
            val bundle = Bundle().apply { putString("bodyId", body) }
            findNavController().navigate(
                R.id.action_celestialBodiesFragment_to_bodyDetailFragment,
                bundle
            )
        }
        binding.filterButton.setOnClickListener {
            showCharacteristicsDialog()
        }
    }

    private fun bindData(celestialBodies: List<CelestialBody>?) {
        celestialBodies?.let {
            celestialBodiesAdapter.submitList(it)
            allBodies = it
        }
        Log.d("Bodies", "Bodies: $allBodies")
    }

    private fun showCharacteristicsDialog() {
        // Inflar el layout del dialog usando ViewBinding
        _dialogBinding = DialogFiltersBinding.inflate(LayoutInflater.from(context))

        // Crear y mostrar el AlertDialog con ViewBinding
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Filtros")
            .setPositiveButton("Aplicar", null) // Botón para cerrar el diálogo
            .show()

        dialog.setOnShowListener {
            dialogBinding.checkboxPlanet.setOnCheckedChangeListener { _, isChecked ->
                Log.d("CheckboxState", "Planeta seleccionado: $isChecked")
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                Log.d("Filter", "Aplicando filtros...")
                val typeMap = mapOf(
                    dialogBinding.checkboxPlanet to CelestialBodyType.PLANET,
                    dialogBinding.checkboxStar to CelestialBodyType.STAR,
                    dialogBinding.checkboxDwarfPlanet to CelestialBodyType.DWARF_PLANET,
                    dialogBinding.checkboxAsteroid to CelestialBodyType.ASTEROID,
                    dialogBinding.checkboxBlackHole to CelestialBodyType.BLACK_HOLE,
                    dialogBinding.checkboxSatellite to CelestialBodyType.SATELLITE,
                    dialogBinding.checkboxArtificialSatellite to CelestialBodyType.ARTIFICIAL_SATELLITE,
                    dialogBinding.checkboxGalaxy to CelestialBodyType.GALAXY,
                )

                val selectedTypes = typeMap.filter { it.key.isChecked }.values.toSet()
                Log.d("SelectedTypes", "Seleccionados: $selectedTypes")


                val filteredBodies = if (selectedTypes.isEmpty()) {
                    allBodies
                } else {
                    val result =
                        allBodies.filter { it.characteristics.celestialBodyType in selectedTypes }
                    Log.d("FilteredBodies", "Bodies filtrados: $result")
                    result
                }
                Log.d("FilteredBodies", "Bodies filtrados: $filteredBodies")

                celestialBodiesAdapter.submitList(filteredBodies)
                Log.d("SelectedTypes", "Seleccionados: $selectedTypes")
                Log.d("Bodies filtrados", "Seleccionados: $filteredBodies")

                dialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}