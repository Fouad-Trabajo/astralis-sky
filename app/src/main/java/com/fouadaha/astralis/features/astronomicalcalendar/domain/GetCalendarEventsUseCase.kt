package com.fouadaha.astralis.features.astronomicalcalendar.domain

import org.koin.core.annotation.Single

@Single
class GetCalendarEventsUseCase(private val calendarRepository: CalendarRepository) {

    operator fun invoke(): List<CalendarEvent> {
        return calendarRepository.getCalendarEvents()
    }
}