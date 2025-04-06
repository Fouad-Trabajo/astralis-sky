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
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.fouadaha.astralis.R
import com.fouadaha.astralis.core.domain.ErrorApp
import com.fouadaha.astralis.core.presentation.hide
import com.fouadaha.astralis.core.presentation.views.ErrorAppFactory
import com.fouadaha.astralis.core.presentation.visible
import com.fouadaha.astralis.databinding.DialogFiltersBinding
import com.fouadaha.astralis.databinding.FragmentCelestialBodiesBinding
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBodyType
import com.fouadaha.astralis.features.celestialbodies.presentation.adapter.CelestialBodiesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CelestialBodiesFragment : Fragment() {

    private val celestialBodiesViewModel: CelestialBodiesViewModel by viewModel()

    private var _binding: FragmentCelestialBodiesBinding? = null
    private var _dialogBinding: DialogFiltersBinding? = null
    private val binding get() = _binding!!
    private val dialogBinding get() = _dialogBinding!!

    private lateinit var skeleton: Skeleton
    private val errorFactory: ErrorAppFactory by inject()
    private lateinit var celestialBodiesAdapter: CelestialBodiesAdapter
    private var allBodies: List<CelestialBody> = emptyList()
    private val selectedFilters = mutableSetOf<CelestialBodyType>()

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
            checkLoading(uiState.isLoading)
            checkError(uiState.errorApp)
            bindData(uiState.bodies)
        }
        celestialBodiesViewModel.uiState.observe(viewLifecycleOwner, bodyObserver)
    }

    private fun checkLoading(isLoading: Boolean) {
        if (isLoading) {
            skeleton.showSkeleton()
        } else {
            skeleton.showOriginal()
        }
    }

    private fun checkError(errorApp: ErrorApp?) {
        errorApp?.let {
            binding.errorAppView.visible()
            binding.listCelestialBodies.hide()
            binding.mainHeader.topAppBar.hide()
            binding.filterButton.hide()

            val error = errorFactory.build(it) {
                celestialBodiesViewModel.getCelestialBodies()
            }
            binding.errorAppView.render(error)
        } ?: run {
            binding.errorAppView.hide()
            binding.listCelestialBodies.visible()
            binding.mainHeader.topAppBar.visible()
            binding.filterButton.visible()
        }
    }


    private fun bindData(celestialBodies: List<CelestialBody>?) {
        celestialBodies?.let {
            allBodies = it // Variable auxiliar para filtrar los cuerpos celestes
            applyFilters() // Aplica los filtros actuales antes de mostrar la lista
        }
    }

    private fun setupView() {
        celestialBodiesAdapter = CelestialBodiesAdapter { body ->
            val action = CelestialBodiesFragmentDirections
                .actionCelestialBodiesFragmentToBodyDetailFragment(body.id)
            findNavController().navigate(action)
        }

        binding.apply {
            // RecyclerView
            listCelestialBodies.layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false
            )
            listCelestialBodies.adapter = celestialBodiesAdapter

            mainHeader.topAppBar.title = getString(R.string.bodies)
            filterButton.setOnClickListener {
                filterDialog()
            }
            skeleton = listCelestialBodies.applySkeleton(R.layout.item_celestial_body, 20)
        }
    }


    private fun filterDialog() {
        _dialogBinding = DialogFiltersBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(getString(R.string.filters))
            .setPositiveButton(R.string.apply, null)
            .show()

        dialogBinding.apply {
            val typeMap = mapOf(
                checkboxPlanet to CelestialBodyType.PLANET,
                checkboxStar to CelestialBodyType.STAR,
                checkboxDwarfPlanet to CelestialBodyType.DWARF_PLANET,
                checkboxAsteroid to CelestialBodyType.ASTEROID,
                checkboxBlackHole to CelestialBodyType.BLACK_HOLE,
                checkboxSatellite to CelestialBodyType.SATELLITE,
                checkboxArtificialSatellite to CelestialBodyType.ARTIFICIAL_SATELLITE,
                checkboxGalaxy to CelestialBodyType.GALAXY,
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                selectedFilters.clear() // limpiar el estado de los CheckBox
                val selectedTypes = typeMap.filter { it.key.isChecked }.values
                selectedFilters.addAll(selectedTypes)

                applyFilters() // Aplicar los filtros
                dialog.dismiss() //Cerrar dialog
            }

            // Restablecer el estado de los CheckBox al cerrar el dialog
            typeMap.forEach { (checkbox, type) ->
                checkbox.isChecked = selectedFilters.contains(type)
            }
        }
    }

    // De esta manera se aplicn los filtros incluso si se navega a otro fragmento o vista detalle
    private fun applyFilters() {
        val filteredBodies = if (selectedFilters.isEmpty()) {
            allBodies
        } else {
            allBodies.filter { it.characteristics.celestialBodyType in selectedFilters }
        }
        celestialBodiesAdapter.submitList(filteredBodies)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogBinding = null
    }
}