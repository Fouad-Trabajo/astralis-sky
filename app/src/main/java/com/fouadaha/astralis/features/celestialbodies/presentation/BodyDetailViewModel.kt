package com.fouadaha.astralis.features.celestialbodies.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fouadaha.astralis.core.domain.ErrorApp
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody
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
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val body = useCase(id)
            _uiState.postValue(
                UiState(
                    isLoading = false,
                    body = body.getOrNull(),
                    errorApp = body.exceptionOrNull() as? ErrorApp
                )
            )
        }
    }


    data class UiState(
        val isLoading: Boolean = false,
        val errorApp: ErrorApp? = null,
        val body: CelestialBody? = null
    )
}