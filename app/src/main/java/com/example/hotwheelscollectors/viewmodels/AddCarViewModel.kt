package com.example.hotwheelscollectors.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

/**
 * DEPRECATED: This ViewModel uses old API with direct DAO access.
 * 
 * NOT USED IN CURRENT UI - Kept for backward compatibility only.
 * 
 * For adding cars, use specific ViewModels that follow Clean Architecture:
 * - AddMainlineViewModel
 * - AddPremiumViewModel  
 * - AddTreasureHuntViewModel
 * - AddSuperTreasureHuntViewModel
 * - AddOthersViewModel
 * 
 * All these ViewModels use AddCarUseCase which handles:
 * - Photo processing
 * - Local storage (Room database)
 * - Firebase sync
 * - Validation and error handling
 * 
 * @deprecated Use AddMainlineViewModel, AddPremiumViewModel, etc. instead
 */
@Deprecated("Use AddMainlineViewModel, AddPremiumViewModel, etc. instead. This ViewModel uses old architecture.")
@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val firestoreRepository: FirestoreRepository,
    private val localRepository: LocalRepository,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState = _uiState.asStateFlow()

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos = _photos.asStateFlow()

    fun addCar(car: CarEntity, photos: List<Uri>) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Save car locally
                carDao.insertCar(car)

                // Save photos
                photos.forEachIndexed { index, uri ->
                    val photoEntity = PhotoEntity(
                        id = UUID.randomUUID().toString(),
                        carId = car.id,
                        localPath = uri.toString(),
                        type = when (index) {
                            0 -> PhotoType.FRONT
                            1 -> PhotoType.BACK
                            else -> PhotoType.OTHER
                        },
                        order = index
                    )
                    photoDao.insertPhoto(photoEntity)
                }

                // NOTE: This ViewModel uses old API and is not used in current UI
                // Remove or refactor to use AddCarUseCase if needed in future
                // Skipping old repository calls for now

                // Sync with Firestore using the new method
                firestoreRepository.addCarEntity(car)

                _uiState.value = UiState.Success(car.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to add car")
            }
        }
    }

    fun addPhoto(uri: Uri) {
        val currentPhotos = _photos.value.toMutableList()
        currentPhotos.add(uri)
        _photos.value = currentPhotos
    }

    fun removePhoto(uri: Uri) {
        val currentPhotos = _photos.value.toMutableList()
        currentPhotos.remove(uri)
        _photos.value = currentPhotos
    }

    fun lookupByBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val currentUserId = firestoreRepository.userId
                val existingCar = carDao.getCarByBarcode(barcode, currentUserId)
                if (existingCar != null) {
                    _uiState.value = UiState.CarExists(existingCar.id)
                } else {
                    _uiState.value = UiState.Initial
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Barcode lookup failed")
            }
        }
    }

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Success(val carId: String) : UiState()
        data class CarExists(val carId: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}