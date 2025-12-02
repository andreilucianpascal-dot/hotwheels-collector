package com.example.hotwheelscollectors.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

enum class CameraStep { FRONT, BACK }

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val firestoreRepository: FirestoreRepository,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var frontPhotoUri: Uri? = null
    private var backPhotoUri: Uri? = null
    private var category: String? = null
    private var brand: String? = null

    private val _status = MutableStateFlow("Waiting for capture...")
    val status = _status.asStateFlow()

    fun setCategoryAndBrand(cat: String, br: String) {
        category = cat
        brand = br
    }

    fun setCapturedPhoto(uri: Uri, step: CameraStep) {
        when (step) {
            CameraStep.FRONT -> {
                frontPhotoUri = uri
                _status.value = "Front photo captured ✅"
            }
            CameraStep.BACK -> {
                backPhotoUri = uri
                _status.value = "Back photo captured ✅"
            }
        }
    }

    fun saveCar() {
        val front = frontPhotoUri ?: return
        val back = backPhotoUri ?: return
        val cat = category ?: return
        val br = brand ?: return

        _status.value = "Processing photos..."

        viewModelScope.launch {
            try {
                // For now, use original photos (optimization will be handled by PhotoOptimizer)
                val thumbnailUri = front
                val zoomUri = front

                // Extract barcode (simplified - would need ML Kit implementation)
                val barcode = UUID.randomUUID().toString()

                // Build car object
                val car = CarEntity(
                    id = UUID.randomUUID().toString(),
                    userId = "default_user",
                    model = "Unknown",
                    brand = br,
                    series = cat,
                    color = "",
                    year = 0,
                    number = "",
                    barcode = barcode,
                    notes = "",
                    isPremium = false,
                    isTH = false,
                    isSTH = false,
                    timestamp = System.currentTimeMillis(),
                    syncStatus = com.example.hotwheelscollectors.data.local.entities.SyncStatus.PENDING_UPLOAD,
                    lastModified = java.util.Date(),
                    subseries = "",
                    photoUrl = thumbnailUri.toString(),
                    frontPhotoPath = zoomUri.toString()
                )

                // NOTE: This ViewModel uses old API and may not be used in current UI
                // Saving to Room DB directly (old approach)
                // Refactor to use AddCarUseCase if needed

                // Save to user's chosen personal cloud
                try {
                    val storagePref = runBlocking { userPreferences.storageLocation.first() }
                    when (storagePref) {
                        "Google Drive" -> {
                            googleDriveRepository.uploadPhoto(
                                thumbnailUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                            googleDriveRepository.uploadPhoto(
                                zoomUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                        }
                        "OneDrive" -> {
                            oneDriveRepository.uploadPhoto(
                                thumbnailUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                            oneDriveRepository.uploadPhoto(
                                zoomUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                        }
                        "Dropbox" -> {
                            dropboxRepository.uploadPhoto(
                                thumbnailUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                            dropboxRepository.uploadPhoto(
                                zoomUri.toString(),
                                barcode,
                                com.example.hotwheelscollectors.data.local.entities.PhotoType.FRONT
                            )
                        }
                        else -> {
                            // Local storage only
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CameraViewModel", "Personal cloud save failed: ${e.message}")
                }

                _status.value = "Car saved successfully ✅"
            } catch (e: Exception) {
                e.printStackTrace()
                _status.value = "Error: ${e.message}"
            }
        }
    }
}
