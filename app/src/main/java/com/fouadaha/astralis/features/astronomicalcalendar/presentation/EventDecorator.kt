package com.fouadaha.astralis.features.astronomicalcalendar.presentation


import com.fouadaha.astralis.features.astronomicalcalendar.domain.CalendarEvent
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class EventDecorator(
    private val color: Int,
    private val dates: Collection<CalendarDay>
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(8F, color))
    }
}

fun CalendarEvent.toCalendarDay(): CalendarDay {
    return this.date.let { CalendarDay.from(it) }
}