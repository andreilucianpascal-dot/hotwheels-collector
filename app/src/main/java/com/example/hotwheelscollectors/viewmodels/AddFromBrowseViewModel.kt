package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import com.example.hotwheelscollectors.utils.PhotoOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddFromBrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val auth: FirebaseAuth,
    private val photoOptimizer: PhotoOptimizer,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddFromBrowseUiState>(AddFromBrowseUiState.Idle)
    val uiState: StateFlow<AddFromBrowseUiState> = _uiState.asStateFlow()

    fun addCarToCollection(globalCar: GlobalCarData) {
        viewModelScope.launch {
            try {
                _uiState.value = AddFromBrowseUiState.Loading
                
                val currentUserId = auth.currentUser?.uid
                if (currentUserId.isNullOrEmpty()) {
                    _uiState.value = AddFromBrowseUiState.Error("User not authenticated")
                    return@launch
                }

                Log.d("AddFromBrowse", "=== ADDING CAR FROM BROWSE ===")
                Log.d("AddFromBrowse", "Car: ${globalCar.carName}")
                Log.d("AddFromBrowse", "Brand: ${globalCar.brand}")
                Log.d("AddFromBrowse", "Series: ${globalCar.series}")
                Log.d("AddFromBrowse", "Category: ${globalCar.category}")
                Log.d("AddFromBrowse", "Subcategory: ${globalCar.subcategory}")
                Log.d("AddFromBrowse", "Front photo URL: ${globalCar.frontPhotoUrl}")

                // Step 1: Download photo from Firebase Storage
                val downloadedPhotoPath = downloadPhotoFromFirebase(globalCar.frontPhotoUrl)
                Log.d("AddFromBrowse", "Photo downloaded to: $downloadedPhotoPath")

                // Step 2: Generate thumbnail and full size using PhotoOptimizer
                val timestamp = System.currentTimeMillis()
                val photoUri = android.net.Uri.fromFile(File(downloadedPhotoPath))
                val outputDir = File(context.cacheDir, "optimized_photos")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val optimizedVersions = photoOptimizer.createOptimizedVersions(
                    originalUri = photoUri,
                    outputDir = outputDir,
                    filename = "car_$timestamp"
                )
                
                Log.d("AddFromBrowse", "Optimized versions created:")
                Log.d("AddFromBrowse", "  - Thumbnail: ${optimizedVersions.thumbnailPath}")
                Log.d("AddFromBrowse", "  - Full size: ${optimizedVersions.fullSizePath}")

                // Step 3: Move photos from cache to permanent storage
                val carId = UUID.randomUUID().toString()
                val photoDir = createPhotoDirectory(currentUserId, carId)
                Log.d("AddFromBrowse", "Photo directory: ${photoDir.absolutePath}")
                
                // Copy thumbnail and full to permanent storage
                val permanentThumbnail = copyPhotoToInternalStorage(
                    optimizedVersions.thumbnailPath, 
                    photoDir, 
                    "thumbnail.jpg"
                )
                val permanentFull = copyPhotoToInternalStorage(
                    optimizedVersions.fullSizePath, 
                    photoDir, 
                    "full.jpg"
                )
                
                Log.d("AddFromBrowse", "Photos moved to permanent storage:")
                Log.d("AddFromBrowse", "  - Thumbnail: $permanentThumbnail")
                Log.d("AddFromBrowse", "  - Full: $permanentFull")
                
                // Delete temporary optimized files from cache
                File(optimizedVersions.thumbnailPath).delete()
                File(optimizedVersions.fullSizePath).delete()
                Log.d("AddFromBrowse", "Temporary optimized files deleted from cache")

                // Step 4: Save to local database
                val car = CarEntity(
                    id = carId,
                    userId = currentUserId,
                    model = globalCar.carName,
                    brand = globalCar.brand,
                    series = globalCar.series,
                    subseries = globalCar.subcategory,
                    folderPath = globalCar.series,
                    color = globalCar.color,
                    year = globalCar.year,
                    barcode = globalCar.barcode,
                    notes = "",
                    isTH = false,
                    isSTH = false,
                    timestamp = timestamp,
                    lastModified = Date(),
                    syncStatus = SyncStatus.SYNCED,
                    photoUrl = permanentFull, // ✅ Permanent path pentru full
                    frontPhotoPath = permanentFull, // ✅ Permanent path pentru full
                    combinedPhotoPath = permanentThumbnail // ✅ Permanent path pentru thumbnail
                )

                carDao.insertCar(car)
                Log.d("AddFromBrowse", "Car saved to local DB with ID: $carId")

                // Step 5: Save photo entity
                val photoEntity = PhotoEntity(
                    id = UUID.randomUUID().toString(),
                    carId = carId,
                    localPath = permanentThumbnail, // ✅ Permanent path
                    thumbnailPath = permanentThumbnail, // ✅ Permanent path
                    fullSizePath = permanentFull, // ✅ Permanent path
                    cloudPath = globalCar.frontPhotoUrl,
                    type = PhotoType.FRONT,
                    syncStatus = SyncStatus.SYNCED,
                    isTemporary = false,
                    barcode = globalCar.barcode,
                    contributorUserId = currentUserId
                )
                photoDao.insertPhoto(photoEntity)
                Log.d("AddFromBrowse", "Photo saved to local DB")

                // Step 6: Delete temporary downloaded file
                File(downloadedPhotoPath).delete()
                Log.d("AddFromBrowse", "Temporary download file deleted")

                _uiState.value = AddFromBrowseUiState.Success("Car added to your collection!")

            } catch (e: Exception) {
                Log.e("AddFromBrowse", "Failed to add car: ${e.message}", e)
                _uiState.value = AddFromBrowseUiState.Error("Failed to add car: ${e.message}")
            }
        }
    }

    private suspend fun downloadPhotoFromFirebase(photoUrl: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d("AddFromBrowse", "Downloading photo from Firebase Storage: $photoUrl")
            
            // Extract the Firebase Storage path from the URL
            val storageRef = try {
                // Convert download URL to storage reference
                val path = photoUrl.substringAfter("firebasestorage.googleapis.com/v0/b/")
                    .substringAfter("/o/")
                    .substringBefore("?")
                    .replace("%2F", "/")
                
                storage.reference.child(path)
            } catch (e: Exception) {
                Log.e("AddFromBrowse", "Failed to parse Firebase Storage URL: ${e.message}")
                throw IllegalArgumentException("Invalid Firebase Storage URL: $photoUrl")
            }

            // Download the photo with EXIF metadata preserved
            val maxDownloadSize = 10L * 1024 * 1024 // 10MB max
            val bytes = storageRef.getBytes(maxDownloadSize).await()
            
            val tempFile = File(context.cacheDir, "temp_download_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                output.write(bytes, 0, bytes.size)
            }

            Log.d("AddFromBrowse", "Photo downloaded successfully with EXIF metadata: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
            tempFile.absolutePath

        } catch (e: Exception) {
            Log.e("AddFromBrowse", "Failed to download photo from Firebase Storage: ${e.message}", e)
            throw e
        }
    }

    /**
     * Creates the photo storage directory for a user's car.
     * Structure: /data/data/package/files/photos/{userId}/{carId}/
     */
    private fun createPhotoDirectory(userId: String, carId: String): File {
        val photoDir = File(context.filesDir, "photos/$userId/$carId")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
            Log.d("AddFromBrowse", "Created photo directory: ${photoDir.absolutePath}")
        }
        return photoDir
    }
    
    /**
     * Copies a photo from temporary cache to permanent internal storage.
     * 
     * @param sourcePath Path to temporary photo
     * @param destDir Destination directory
     * @param fileName Desired file name
     * @return Path to the copied file
     */
    private fun copyPhotoToInternalStorage(sourcePath: String, destDir: File, fileName: String): String {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Source file does not exist: $sourcePath")
        }
        
        val destFile = File(destDir, fileName)
        
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        
        Log.d("AddFromBrowse", "Photo copied: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
        
        return destFile.absolutePath
    }

    fun resetState() {
        _uiState.value = AddFromBrowseUiState.Idle
    }
}

sealed class AddFromBrowseUiState {
    object Idle : AddFromBrowseUiState()
    object Loading : AddFromBrowseUiState()
    data class Success(val message: String) : AddFromBrowseUiState()
    data class Error(val message: String) : AddFromBrowseUiState()
}

