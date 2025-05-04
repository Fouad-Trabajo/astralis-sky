package com.fouadaha.astralis.features.astronomicalcalendar.domain

import org.threeten.bp.LocalDate

data class CalendarEvent(
    val id: String,
    val name: String,
    val description: String,
    val date: LocalDate
)