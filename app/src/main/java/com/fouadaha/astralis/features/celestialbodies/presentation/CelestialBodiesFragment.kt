package com.fouadaha.astralis.features.celestialbodies.presentation


import android.app.AlertDialog
import android.os.Bundle
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
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBodyType
import com.fouadaha.astralis.features.celestialbodies.presentation.adapter.CelestialBodiesAdapter
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
            mainHeader.topAppBar.title = getString(R.string.bodies)
        }
        celestialBodiesAdapter.setEvent { body ->
            val bundle = Bundle().apply { putString("bodyId", body) }
            findNavController().navigate(
                R.id.action_celestialBodiesFragment_to_bodyDetailFragment,
                bundle
            )
        }
        binding.filterButton.setOnClickListener {
            filterDialog()
        }
    }

    private fun bindData(celestialBodies: List<CelestialBody>?) {
        celestialBodies?.let {
            celestialBodiesAdapter.submitList(it)
            allBodies = it
        }
    }
    private val selectedFilters = mutableSetOf<CelestialBodyType>()
    private fun filterDialog() {

        _dialogBinding = DialogFiltersBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(getString(R.string.filters))
            .setPositiveButton(R.string.apply, null)
            .show()


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

        // Restablecer el estado de los CheckBox
        typeMap.forEach { (checkbox, type) ->
            checkbox.isChecked = selectedFilters.contains(type)
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            selectedFilters.clear()
            val selectedTypes = typeMap.filter { it.key.isChecked }.values

            selectedFilters.addAll(selectedTypes)

            val filteredBodies = if (selectedTypes.isEmpty()) {
                allBodies
            } else {
                allBodies.filter { it.characteristics.celestialBodyType in selectedTypes }
            }

            celestialBodiesAdapter.submitList(filteredBodies)
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}