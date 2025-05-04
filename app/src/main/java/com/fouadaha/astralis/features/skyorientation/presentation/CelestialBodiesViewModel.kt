package com.fouadaha.astralis.features.skyorientation.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fouadaha.astralis.features.skyorientation.domain.CelestialBody
import com.fouadaha.astralis.features.skyorientation.domain.GetCelestialBodiesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CelestialBodiesViewModel(private val useCase: GetCelestialBodiesUseCase) :
    ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun getCelestialBodies() {
        _uiState.value = UiState()
        viewModelScope.launch(Dispatchers.IO) {
            val bodies = useCase()
            _uiState.postValue(UiState(bodies = bodies.getOrNull()))
        }
    }

    data class UiState(
        val bodies: List<CelestialBody?>? = emptyList()
    )
}