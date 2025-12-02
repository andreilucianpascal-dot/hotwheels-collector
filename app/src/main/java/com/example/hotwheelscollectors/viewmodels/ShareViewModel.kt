package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val carDao: CarDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadCar(carId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val car = carDao.getCarById(carId)

                if (car != null) {
                    _uiState.value = UiState.Success(car)
                } else {
                    _uiState.value = UiState.Error("Car not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load car: ${e.message}")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val car: CarEntity) : UiState()
        data class Error(val message: String) : UiState()
    }
}