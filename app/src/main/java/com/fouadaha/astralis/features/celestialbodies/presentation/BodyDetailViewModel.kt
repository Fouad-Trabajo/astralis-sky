package com.fouadaha.astralis.features.celestialbodies.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fouadaha.astralis.core.domain.model.CelestialBodyCore
import com.fouadaha.astralis.features.celestialbodies.domain.GetCelestialBodyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class BodyDetailViewModel(private val useCase: GetCelestialBodyUseCase) :
    ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun getBody(id: String) {
        _uiState.value = UiState()
        viewModelScope.launch(Dispatchers.IO) {
            val body = useCase(id)
            _uiState.postValue(UiState(body = body.getOrNull()))
        }
    }

    data class UiState(
        val body: CelestialBodyCore? = null
    )
}