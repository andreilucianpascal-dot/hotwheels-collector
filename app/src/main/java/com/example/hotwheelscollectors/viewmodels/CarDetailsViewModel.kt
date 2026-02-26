package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.PriceHistoryDao
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.data.local.entities.PriceHistoryEntity
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.model.PersonalStorageType
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
    private val priceHistoryDao: PriceHistoryDao,
    private val userPreferences: UserPreferences,
    private val googleDriveRepository: GoogleDriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _priceHistory = MutableStateFlow<List<PriceHistoryEntity>>(emptyList())
    val priceHistory = _priceHistory.asStateFlow()

    fun loadCar(carId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                // ✅ FIX: Add null check and error handling
                if (carId.isBlank()) {
                    _uiState.value = UiState.Error("Invalid car ID")
                    return@launch
                }
                val carWithPhotos = carDao.getCarWithPhotosById(carId).first()
                if (carWithPhotos != null) {
                    _uiState.value = UiState.Success(carWithPhotos)
                    loadPriceHistory(carId)
                } else {
                    _uiState.value = UiState.Error("Car not found")
                }
            } catch (e: Exception) {
                android.util.Log.e("CarDetailsViewModel", "Error loading car: ${e.message}", e)
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
                persistToDriveIfPrimary(car)
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
                    persistToDriveIfPrimary(updatedCar)
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
                    persistToDriveIfPrimary(updatedCar)
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
                    persistToDriveIfPrimary(updatedCar)
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
                    persistToDriveIfPrimary(updatedCar)
                    loadCar(carId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update brand")
            }
        }
    }

    fun updatePurchasePrice(carId: String, newPurchasePrice: Double) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        purchasePrice = newPurchasePrice,
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    persistToDriveIfPrimary(updatedCar)
                    loadCar(carId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update purchase price")
            }
        }
    }

    fun updatePurchaseInfo(carId: String, newPurchasePrice: Double, currencyCode: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedCar = currentState.carWithPhotos.car.copy(
                        purchasePrice = newPurchasePrice,
                        purchaseCurrency = currencyCode.trim().uppercase(),
                        lastModified = Date(),
                        updatedAt = System.currentTimeMillis()
                    )
                    carDao.updateCar(updatedCar)
                    persistToDriveIfPrimary(updatedCar)
                    loadCar(carId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update purchase info")
            }
        }
    }

    private suspend fun persistToDriveIfPrimary(updatedCar: CarEntity) {
        val storageType = userPreferences.storageType.first()
        if (storageType == PersonalStorageType.GOOGLE_DRIVE) {
            val r = googleDriveRepository.upsertCarInDbJsonIfDrivePrimary(updatedCar)
            if (r.isFailure) {
                // Make it visible: otherwise user thinks data is saved, but it will reset on next restart.
                throw r.exceptionOrNull() ?: Exception("Failed to save changes to Google Drive")
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