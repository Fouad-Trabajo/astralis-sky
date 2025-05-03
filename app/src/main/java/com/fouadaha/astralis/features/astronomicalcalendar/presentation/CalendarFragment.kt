package com.fouadaha.astralis.features.astronomicalcalendar.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fouadaha.astralis.databinding.FragmentCalendarBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarEventViewModel by viewModel()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val dateEvents = state.events.map { it.toCalendarDay() }
            val decorator = EventDecorator(Color.RED, dateEvents)
            binding.calendarView.addDecorator(decorator)
        }
        viewModel.getEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}