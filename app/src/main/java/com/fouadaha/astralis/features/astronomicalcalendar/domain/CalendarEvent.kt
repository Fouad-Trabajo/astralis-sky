package com.fouadaha.astralis.features.astronomicalcalendar.domain

import java.time.LocalDate


data class CalendarEvent(
    val id: String,
    val name: String,
    val description: String,
    val date: LocalDate
)