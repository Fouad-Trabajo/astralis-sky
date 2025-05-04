package com.fouadaha.astralis.features.astronomicalcalendar.data

import com.fouadaha.astralis.features.astronomicalcalendar.data.local.json.CalendarEventsJsonLocalDataSource
import com.fouadaha.astralis.features.astronomicalcalendar.domain.CalendarEvent
import com.fouadaha.astralis.features.astronomicalcalendar.domain.CalendarRepository
import org.koin.core.annotation.Single

@Single
class CalendarEventDataRepository(private val eventsLocalJson: CalendarEventsJsonLocalDataSource) :
    CalendarRepository {
    override fun getCalendarEvents(): List<CalendarEvent> {
        return eventsLocalJson.getCalendarEvents()
    }
}