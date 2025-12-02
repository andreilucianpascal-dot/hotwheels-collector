package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.PriceHistoryDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.data.local.entities.PriceHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CarDetailsViewModel @Inject constructor(
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val priceHistoryDao: PriceHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _priceHistory = MutableStateFlow<List<PriceHistoryEntity>>(emptyList())
    val priceHistory = _priceHistory.asStateFlow()

    fun loadCar(carId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val carWithPhotos = carDao.getCarWithPhotosById(carId).first()
                if (carWithPhotos != null) {
                    _uiState.value = UiState.Success(carWithPhotos)
                    loadPriceHistory(carId)
                } else {
                    _uiState.value = UiState.Error("Car not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load car details")
            }
        }
    }

    private fun loadPriceHistory(carId: String) {
        viewModelScope.launch {
            priceHistoryDao.getPriceHistoryForCar(carId)
                .catch { _priceHistory.value = emptyList() }
                .collect { history -> _priceHistory.value = history }
        }
    }

    fun updateCar(car: CarEntity) {
        viewModelScope.launch {
            try {
                carDao.updateCar(car)
                loadCar(car.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update car")
            }
        }
    }

    fun deleteCar(carId: String) {
        viewModelScope.launch {
            try {
                // Using the existing deleteCarById method instead of softDeleteCar
                carDao.deleteCarById(carId)
                _uiState.value = UiState.Deleted
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete car")
            }
        }
    }

    fun updateModel(carId: String, newModel: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        model = newModel,
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    loadCar(carId) // Reload to show updated data
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update model")
            }
        }
    }

    fun updateYear(carId: String, newYear: Int) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        year = newYear,
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    loadCar(carId) // Reload to show updated data
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update year")
            }
        }
    }

    fun updateColor(carId: String, newColor: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        color = newColor,
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    loadCar(carId) // Reload to show updated data
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update color")
            }
        }
    }

    fun updateBrand(carId: String, newBrand: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        brand = newBrand,
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    loadCar(carId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update brand")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val carWithPhotos: CarWithPhotos) : UiState()
        data class Error(val message: String) : UiState()
        object Deleted : UiState()
    }
}