package com.fouadaha.astralis.features.astronomicalcalendar.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fouadaha.astralis.databinding.FragmentCalendarBinding
import com.fouadaha.astralis.features.astronomicalcalendar.presentation.dayadapter.DayViewContainer
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.YearMonth


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
            //val dateEvents = state.events.map { it.toCalendarDay() }
            //val decorator = EventDecorator(Color.RED, dateEvents)
            // binding.calendarView.addDecorator(decorator)
            binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
                // Called only when a new container is needed.
                override fun create(view: View) = DayViewContainer(view)

                // Called every time we need to reuse a container.
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.textView.text = data.date.dayOfMonth.toString()
                }
            }

            val currentMonth = YearMonth.now()
            val startMonth = currentMonth.minusMonths(100) // Adjust as needed
            val endMonth = currentMonth.plusMonths(100) // Adjust as needed
            val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library
            binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
            binding.calendarView.scrollToMonth(currentMonth)
        }
        viewModel.getEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}