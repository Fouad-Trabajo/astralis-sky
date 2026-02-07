package com.fouadaha.astralis.features.astronomicalcalendar.domain

interface CalendarRepository {

    fun getCalendarEvents(): List<CalendarEvent>
}