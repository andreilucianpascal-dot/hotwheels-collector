package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.UserDao
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.UserEntity
import kotlinx.coroutines.flow.first
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import com.example.hotwheelscollectors.data.repository.UserStorageRepository
import com.example.hotwheelscollectors.data.repository.UserCloudSyncRepository
import com.example.hotwheelscollectors.utils.PhotoOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import javax.inject.Inject

@HiltViewModel
class AddFromBrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val carDao: CarDao,
    private val auth: FirebaseAuth,
    private val photoOptimizer: PhotoOptimizer,
    private val storage: FirebaseStorage,
    private val userStorageRepository: UserStorageRepository,
    private val userCloudSyncRepository: UserCloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddFromBrowseUiState>(AddFromBrowseUiState.Idle)
    val uiState: StateFlow<AddFromBrowseUiState> = _uiState.asStateFlow()
    
    /**
     * Persistent CoroutineScope for background backup operations.
     * Uses SupervisorJob to ensure that if backup fails, it doesn't affect the save operation.
     */
    private val backupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun addCarToCollection(globalCar: GlobalCarData) {
        viewModelScope.launch {
            try {
                _uiState.value = AddFromBrowseUiState.Loading
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = AddFromBrowseUiState.Error("User not authenticated")
                    return@launch
                }
                
                val currentUserId = currentUser.uid
                
                // Ensure UserEntity exists in local database to prevent FOREIGN KEY constraint errors
                ensureUserEntityExists(currentUser)

                Log.d("AddFromBrowse", "=== ADDING CAR FROM BROWSE ===")
                Log.d("AddFromBrowse", "Car: ${globalCar.carName}")
                Log.d("AddFromBrowse", "Brand: ${globalCar.brand}")
                Log.d("AddFromBrowse", "Series: ${globalCar.series}")
                Log.d("AddFromBrowse", "Category: ${globalCar.category}")
                Log.d("AddFromBrowse", "Subcategory: ${globalCar.subcategory}")
                Log.d("AddFromBrowse", "Front photo URL: ${globalCar.frontPhotoUrl}")

                // ✅ STEP 0: Check for duplicate BEFORE downloading photo (prevents unnecessary download)
                val isDuplicate = checkForDuplicateFromBrowse(globalCar, currentUserId)
                if (isDuplicate) {
                    _uiState.value = AddFromBrowseUiState.Error("The car is already in your collection")
                    Log.w("AddFromBrowse", "⚠️ Duplicate car detected - preventing save")
                    return@launch
                }
                
                Log.d("AddFromBrowse", "✅ No duplicate found - proceeding with save")

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

                // Step 3: Determine car category flags based on globalCar.category
                val categoryLower = globalCar.category.lowercase()
                val seriesLower = globalCar.series.lowercase()
                val isPremium = categoryLower.contains("premium")
                val isTH = categoryLower.contains("treasure") && !categoryLower.contains("super")
                val isSTH = categoryLower.contains("super") && categoryLower.contains("treasure")
                
                // Determine series (normalize Premium and Silver Series)
                val normalizedSeries = when {
                    isPremium -> "Premium"  // ✅ Fix: Premium cars must have series = "Premium"
                    seriesLower.contains("silver") -> "Silver Series"
                    else -> globalCar.series
                }
                
                // ✅ FIX: Parse Premium category and subcategory correctly
                // For Premium: globalCar.subcategory = "Pop Culture/Back to the Future" or "Boulevard"
                // We need to split into category (e.g., "Pop Culture") and subcategory (e.g., "Back to the Future")
                val (premiumCategory, premiumSubcategory) = if (isPremium && !globalCar.subcategory.isNullOrEmpty()) {
                    val parts = globalCar.subcategory.split("/", limit = 2)
                    if (parts.size == 2) {
                        // Has subcategory: "Pop Culture/Back to the Future" -> category="Pop Culture", subcategory="Back to the Future"
                        Pair(parts[0].trim(), parts[1].trim())
                    } else {
                        // No subcategory: "Boulevard" -> category="Boulevard", subcategory=null
                        Pair(parts[0].trim(), null)
                    }
                } else {
                    // Not Premium: use subcategory as-is (for Mainline)
                    Pair(globalCar.subcategory ?: "", null)
                }
                
                Log.d("AddFromBrowse", "Category mapping: category='${globalCar.category}', subcategory='${globalCar.subcategory}' -> isPremium=$isPremium, isTH=$isTH, isSTH=$isSTH, normalizedSeries='$normalizedSeries'")
                if (isPremium) {
                    Log.d("AddFromBrowse", "Premium car parsed: category='$premiumCategory', subcategory='$premiumSubcategory'")
                }
                
                // Step 4: Create CarDataToSync and save via UserStorageRepository
                // ✅ This will automatically choose between Local and Google Drive based on user preferences
                val carData = CarDataToSync(
                    userId = currentUserId,
                    name = globalCar.carName,
                    brand = globalCar.brand,
                    series = normalizedSeries,
                    category = if (isPremium) premiumCategory else (globalCar.subcategory ?: ""),
                    subcategory = if (isPremium) premiumSubcategory else null,
                    color = globalCar.color,
                    year = globalCar.year,
                    barcode = globalCar.barcode,
                    notes = "",
                    isTH = isTH,
                    isSTH = isSTH,
                    isPremium = isPremium,
                    screenType = "Browse",
                    pendingPhotos = emptyList(), // No pending photos - already optimized
                    preOptimizedThumbnailPath = optimizedVersions.thumbnailPath,
                    preOptimizedFullPath = optimizedVersions.fullSizePath,
                    originalBrowsePhotoUrl = globalCar.frontPhotoUrl // ✅ Firebase URL from Browse (for duplicate prevention)
                )
                
                Log.d("AddFromBrowse", "Saving car via UserStorageRepository (will use Local or Google Drive based on user preference)...")
                Log.d("AddFromBrowse", "Thumbnail path: ${optimizedVersions.thumbnailPath}")
                Log.d("AddFromBrowse", "Full path: ${optimizedVersions.fullSizePath}")
                Log.d("AddFromBrowse", "Thumbnail exists: ${File(optimizedVersions.thumbnailPath).exists()}")
                Log.d("AddFromBrowse", "Full exists: ${File(optimizedVersions.fullSizePath).exists()}")
                
                val saveResult = userStorageRepository.saveCar(
                    data = carData,
                    localThumbnail = optimizedVersions.thumbnailPath,
                    localFull = optimizedVersions.fullSizePath,
                    barcode = globalCar.barcode
                )
                
                if (saveResult.isFailure) {
                    val error = saveResult.exceptionOrNull()?.message ?: "Unknown error"
                    val exception = saveResult.exceptionOrNull()
                    Log.e("AddFromBrowse", "❌ Failed to save car: $error")
                    if (exception != null) {
                        Log.e("AddFromBrowse", "Exception details:", exception)
                    }
                    throw Exception("Failed to save car: $error")
                }
                
                val carId = saveResult.getOrNull() ?: throw Exception("Car ID is null after save")
                Log.d("AddFromBrowse", "✅ Car saved successfully with ID: $carId")
                Log.d("AddFromBrowse", "✅ Storage repository used: ${userStorageRepository::class.simpleName}")
                
                // Step 5: Update backup to cloud (background, non-blocking)
                backupScope.launch {
                    try {
                        userCloudSyncRepository.syncLatestBackup()
                        Log.d("AddFromBrowse", "✅ Latest backup updated successfully")
                    } catch (e: Exception) {
                        Log.w("AddFromBrowse", "⚠️ Backup sync failed: ${e.message}")
                        // Don't fail the entire operation if backup sync fails
                    }
                }
                Log.d("AddFromBrowse", "✅ Latest backup sync initiated (non-blocking)")
                
                // Step 6: Delete temporary files ONLY after successful save
                // ✅ FIX: GoogleDriveRepository copies files during upload, so we can safely delete temp files
                // ✅ FIX: LocalRepository copies files to permanent storage, so we can safely delete temp files
                try {
                    File(optimizedVersions.thumbnailPath).delete()
                    File(optimizedVersions.fullSizePath).delete()
                    File(downloadedPhotoPath).delete()
                    Log.d("AddFromBrowse", "✅ Temporary files deleted")
                } catch (e: Exception) {
                    Log.w("AddFromBrowse", "Failed to delete some temporary files: ${e.message}")
                    // Don't fail the operation if cleanup fails
                }

                _uiState.value = AddFromBrowseUiState.Success("Car added to your collection!")

            } catch (e: Exception) {
                Log.e("AddFromBrowse", "Failed to add car: ${e.message}", e)
                _uiState.value = AddFromBrowseUiState.Error("Failed to add car: ${e.message}")
            }
        }
    }

    /**
     * Checks if a car from Browse already exists in the user's collection.
     * Verification is done BEFORE downloading photo to prevent unnecessary downloads.
     * 
     * Logic:
     * - If car has barcode: check barcode + originalBrowsePhotoUrl (Firebase URL)
     * - If car has no barcode: check model + brand + year + series + color + originalBrowsePhotoUrl
     * 
     * @param globalCar Car from Browse to check
     * @param userId Current user ID
     * @return true if duplicate exists, false otherwise
     */
    private suspend fun checkForDuplicateFromBrowse(globalCar: GlobalCarData, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingCars = carDao.getCarsForUser(userId).first()
            
            // Check for duplicate based on originalBrowsePhotoUrl (Firebase URL from Browse)
            val duplicate = existingCars.firstOrNull { existing ->
                // Must have originalBrowsePhotoUrl (was added from Browse)
                if (existing.originalBrowsePhotoUrl.isNullOrEmpty()) {
                    return@firstOrNull false
                }
                
                // Check if Firebase URLs match (same photo = same car from Browse)
                if (existing.originalBrowsePhotoUrl == globalCar.frontPhotoUrl) {
                    // Same Firebase URL - check additional criteria
                    if (globalCar.barcode.isNotEmpty()) {
                        // Has barcode: check barcode + originalBrowsePhotoUrl
                        existing.barcode == globalCar.barcode
                    } else {
                        // No barcode: check model + brand + year + series + color
                        existing.model.equals(globalCar.carName, ignoreCase = true) &&
                        existing.brand.equals(globalCar.brand, ignoreCase = true) &&
                        existing.year == globalCar.year &&
                        existing.series.equals(globalCar.series, ignoreCase = true) &&
                        existing.color.equals(globalCar.color, ignoreCase = true)
                    }
                } else {
                    false
                }
            }
            
            if (duplicate != null) {
                Log.d("AddFromBrowse", "⚠️ Duplicate car found:")
                Log.d("AddFromBrowse", "  - Existing: id=${duplicate.id}, model='${duplicate.model}', barcode='${duplicate.barcode}'")
                Log.d("AddFromBrowse", "  - Existing originalBrowsePhotoUrl: ${duplicate.originalBrowsePhotoUrl}")
                Log.d("AddFromBrowse", "  - New frontPhotoUrl: ${globalCar.frontPhotoUrl}")
                return@withContext true
            }
            
            Log.d("AddFromBrowse", "✅ No duplicate found - car is safe to add")
            false
        } catch (e: Exception) {
            Log.e("AddFromBrowse", "Error checking for duplicate: ${e.message}", e)
            // If duplicate check fails, allow the save to proceed (fail-safe)
            false
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
     * Ensures UserEntity exists in local database to prevent FOREIGN KEY constraint errors.
     */
    private suspend fun ensureUserEntityExists(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val existingUser = userDao.getById(firebaseUser.uid)
        if (existingUser == null) {
            Log.d("AddFromBrowse", "Creating UserEntity for user: ${firebaseUser.uid}")
            val userEntity = UserEntity(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                lastLoginAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            userDao.insert(userEntity)
            Log.d("AddFromBrowse", "✅ UserEntity created successfully")
        } else {
            Log.d("AddFromBrowse", "UserEntity already exists for user: ${firebaseUser.uid}")
        }
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

