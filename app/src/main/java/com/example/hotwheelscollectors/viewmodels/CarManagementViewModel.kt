package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CarManagementViewModel @Inject constructor(
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Photo management functions
    fun copyPhoto(photoId: String, destinationPath: String): Boolean {
        return try {
            val photo = runBlocking { photoDao.getPhotoById(photoId) }
            if (photo != null) {
                val sourceFile = File(photo.localPath)
                val destFile = File(destinationPath)
                
                if (sourceFile.exists()) {
                    // Create parent directories if they don't exist
                    destFile.parentFile?.mkdirs()
                    
                    // Copy file
                    FileInputStream(sourceFile as File).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Photo copy failed: ${e.message}")
            false
        }
    }

    fun movePhoto(photoId: String, newPath: String): Boolean {
        return try {
            val photo = runBlocking { photoDao.getPhotoById(photoId) }
            if (photo != null) {
                val sourceFile = File(photo.localPath)
                val destFile = File(newPath)
                
                if (sourceFile.exists()) {
                    // Create parent directories if they don't exist
                    destFile.parentFile?.mkdirs()
                    
                    // Move file
                    val success = sourceFile.renameTo(destFile)
                    if (success) {
                        // Update database with new path
                        val updatedPhoto = photo.copy(localPath = newPath)
                        runBlocking { photoDao.updatePhoto(updatedPhoto) }
                    }
                    success
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Photo move failed: ${e.message}")
            false
        }
    }

    fun exportPhoto(photoId: String, exportPath: String): Boolean {
        return try {
            val photo = runBlocking { photoDao.getPhotoById(photoId) }
            if (photo != null) {
                val sourceFile = File(photo.localPath)
                val destFile = File(exportPath)
                
                if (sourceFile.exists()) {
                    // Create parent directories if they don't exist
                    destFile.parentFile?.mkdirs()
                    
                    // Copy file to export location
                    FileInputStream(sourceFile as File).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Photo export failed: ${e.message}")
            false
        }
    }

    fun sharePhoto(photoId: String): String? {
        return try {
            val photo = runBlocking { photoDao.getPhotoById(photoId) }
            if (photo != null) {
                val file = File(photo.localPath)
                if (file.exists()) {
                    file.absolutePath
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Photo share failed: ${e.message}")
            null
        }
    }

    // Car management functions
    fun copyCar(carId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val carWithPhotos = carDao.getCarWithPhotosById(carId).first()
                if (carWithPhotos != null) {
                    val originalCar = carWithPhotos.car
                    val newCarId = UUID.randomUUID().toString()
                    
                    // Create a copy of the car
                    val copiedCar = originalCar.copy(
                        id = newCarId,
                        model = "${originalCar.model} (Copy)",
                        timestamp = System.currentTimeMillis(),
                        lastModified = Date(),
                        syncStatus = SyncStatus.PENDING_UPLOAD
                    )
                    
                    // Save the copied car
                    carDao.insertCar(copiedCar)
                    
                    // Copy photos
                    for (photo in carWithPhotos.photos) {
                        val newPhotoId = UUID.randomUUID().toString()
                        val copiedPhoto = photo.copy(
                            id = newPhotoId,
                            carId = newCarId
                        )
                        photoDao.insertPhoto(copiedPhoto)
                    }
                    
                    android.util.Log.i("CarManagementViewModel", "Car copied successfully: $newCarId")
                }
            } catch (e: Exception) {
                android.util.Log.e("CarManagementViewModel", "Car copy failed: ${e.message}")
            }
        }
    }

    fun moveCar(carId: String, newCategory: String, newBrand: String) {
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val carWithPhotos = carDao.getCarWithPhotosById(carId).first()
                if (carWithPhotos != null) {
                    val updatedCar = carWithPhotos.car.copy(
                        series = newCategory,
                        brand = newBrand,
                        lastModified = Date()
                    )
                    carDao.updateCar(updatedCar)
                    android.util.Log.i("CarManagementViewModel", "Car moved successfully: $carId")
                }
            } catch (e: Exception) {
                android.util.Log.e("CarManagementViewModel", "Car move failed: ${e.message}")
            }
        }
    }

    fun deleteCar(carId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val carWithPhotos = carDao.getCarWithPhotosById(carId).first()
                if (carWithPhotos != null) {
                    // Delete photos first
                    for (photo in carWithPhotos.photos) {
                        val photoFile = File(photo.localPath)
                        if (photoFile.exists()) {
                            photoFile.delete()
                        }
                        photoDao.hardDeletePhoto(photo.id)
                    }
                    
                    // Delete the car
                    carDao.deleteCarById(carId)
                    android.util.Log.i("CarManagementViewModel", "Car deleted successfully: $carId")
                }
            } catch (e: Exception) {
                android.util.Log.e("CarManagementViewModel", "Car delete failed: ${e.message}")
            }
        }
    }

    fun exportCar(carId: String): String {
        return try {
            val carWithPhotos = runBlocking { carDao.getCarWithPhotosById(carId).first() }
            if (carWithPhotos != null) {
                val car = carWithPhotos.car
                val exportData = """
                    {
                        "id": "${car.id}",
                        "model": "${car.model}",
                        "brand": "${car.brand}",
                        "series": "${car.series}",
                        "color": "${car.color}",
                        "year": ${car.year},
                        "number": "${car.number}",
                        "barcode": "${car.barcode}",
                        "notes": "${car.notes}",
                        "isPremium": ${car.isPremium},
                        "isTH": ${car.isTH},
                        "isSTH": ${car.isSTH},
                        "timestamp": ${car.timestamp},
                        "photos": [
                            ${carWithPhotos.photos.joinToString(",") { "\"${it.localPath}\"" }}
                        ]
                    }
                """.trimIndent()
                exportData
            } else {
                "{}"
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Car export failed: ${e.message}")
            "{}"
        }
    }

    fun shareCar(carId: String): String {
        return try {
            val carWithPhotos = runBlocking { carDao.getCarWithPhotosById(carId).first() }
            if (carWithPhotos != null) {
                val car = carWithPhotos.car
                val shareText = """
                    Car: ${car.brand} ${car.model}
                    Series: ${car.series}
                    Year: ${car.year}
                    Color: ${car.color}
                    Barcode: ${car.barcode}
                    Notes: ${car.notes}
                    ${if (car.isTH) "Treasure Hunt" else ""}
                    ${if (car.isSTH) "Super Treasure Hunt" else ""}
                """.trimIndent()
                shareText
            } else {
                "Car not found"
            }
        } catch (e: Exception) {
            android.util.Log.e("CarManagementViewModel", "Car share failed: ${e.message}")
            "Error sharing car"
        }
    }

    private fun getCurrentUserId(): String {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            throw IllegalStateException("User must be authenticated to manage cars")
        }
        return userId
    }
}
