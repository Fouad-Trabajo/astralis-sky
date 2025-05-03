package com.fouadaha.astralis.features.astronomicalcalendar.data.local.json

import android.content.Context
import com.fouadaha.astralis.R
import com.fouadaha.astralis.features.astronomicalcalendar.domain.CalendarEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class CalendarEventsJsonLocalDataSource(val context: Context) {

    private val gson = Gson()


    fun getCalendarEvents(): List<CalendarEvent> {
        val inputStream = context.resources.openRawResource(R.raw.calendar_events)
        val reader = InputStreamReader(inputStream)
        val typeToken = object : TypeToken<Array<CalendarEvent>>() {}.type
        return gson.fromJson(reader, typeToken)
    }
}