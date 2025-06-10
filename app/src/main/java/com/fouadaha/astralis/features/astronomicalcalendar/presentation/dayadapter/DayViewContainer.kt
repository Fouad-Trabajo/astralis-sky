package com.fouadaha.astralis.features.astronomicalcalendar.presentation.dayadapter

import android.view.View
import com.fouadaha.astralis.databinding.CalendarDayLayoutBinding
import com.kizitonwose.calendar.view.ViewContainer


class DayViewContainer(view: View) : ViewContainer(view) {

    private lateinit var binding: CalendarDayLayoutBinding
    val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
}