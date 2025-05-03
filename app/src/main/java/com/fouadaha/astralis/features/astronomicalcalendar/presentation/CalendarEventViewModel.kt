package com.fouadaha.astralis.features.astronomicalcalendar.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fouadaha.astralis.features.astronomicalcalendar.domain.CalendarEvent
import com.fouadaha.astralis.features.astronomicalcalendar.domain.GetCalendarEventsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CalendarEventViewModel(private val useCase: GetCalendarEventsUseCase) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun getEvents() {
        _uiState.value = UiState()
        viewModelScope.launch(Dispatchers.IO) {
            val events = useCase()
            _uiState.postValue(UiState(events = events))
        }
    }

    data class UiState(
        val events: List<CalendarEvent> = emptyList()
    )
}