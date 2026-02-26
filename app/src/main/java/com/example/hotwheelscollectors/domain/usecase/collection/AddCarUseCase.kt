package com.example.hotwheelscollectors.domain.usecase.collection

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.data.repository.CarSyncRepository
import com.example.hotwheelscollectors.data.repository.PhotoProcessingRepository
import com.example.hotwheelscollectors.data.repository.UserStorageRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.UserCloudSyncRepository
import com.example.hotwheelscollectors.data.local.dao.UserDao
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * AddCarUseCase is the central brain for adding cars to the collection.
 * 
 * RESPONSIBILITIES:
 * 1. Validate input data
 * 2. Process photos (optimize, extract barcode)
 * 3. Delegate saving to the correct storage repository (Local or Drive)
 * 4. Sync to Firebase Firestore via CarSyncRepository
 * 
 * This UseCase coordinates all repositories and ensures a consistent flow
 * for ALL add screens (Mainline, Premium, TH, STH, Others).
 */
class AddCarUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userStorageRepository: UserStorageRepository,
    private val photoProcessingRepository: PhotoProcessingRepository,
    private val carSyncRepository: CarSyncRepository,
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    private val carDao: CarDao,
    private val firestoreRepository: FirestoreRepository,
    private val userCloudSyncRepository: UserCloudSyncRepository
) {
    /**
     * Persistent CoroutineScope for background sync operations.
     * Uses SupervisorJob to ensure that if one sync fails, others continue.
     * Uses Dispatchers.IO for I/O operations (Firebase uploads).
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /**
     * Main entry point for adding a car.
     * 
     * @param data Complete car data including photos, metadata, and flags
     * @return Result containing the car ID if successful, or error message if failed
     */
    suspend operator fun invoke(data: CarDataToSync): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("AddCarUseCase", "=== STARTING CAR ADDITION ===")
            Log.d("AddCarUseCase", "Screen type: ${data.screenType}")
            Log.d("AddCarUseCase", "Series: ${data.series}")
            Log.d("AddCarUseCase", "Category: ${data.category}")
            Log.d("AddCarUseCase", "Brand: ${data.brand}")
            Log.d("AddCarUseCase", "Pending photos: ${data.pendingPhotos.size}")
            
            // Step 1: Validate input
            val validationError = validateInput(data)
            if (validationError != null) {
                Log.e("AddCarUseCase", "Validation failed: $validationError")
                return@withContext Result.failure(IllegalArgumentException(validationError))
            }
            
            // Step 2: Ensure user is authenticated
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.e("AddCarUseCase", "User not authenticated")
                return@withContext Result.failure(IllegalStateException("User must be authenticated"))
            }
            
            val userId = currentUser.uid
            
            // Step 2.5: Ensure UserEntity exists in local database
            ensureUserEntityExists(currentUser)
            
            // Step 2.6: Check for duplicates before processing
            val duplicateCheck = checkForDuplicates(data, userId)
            if (duplicateCheck.isDuplicate) {
                Log.w("AddCarUseCase", "Duplicate car detected: ${duplicateCheck.message}")
                return@withContext Result.failure(IllegalArgumentException(duplicateCheck.message))
            }
            
            // Step 3: Process photos (optimize and extract barcode)
            val (localThumbnail, localFull, extractedBarcode) = processPhotos(data)
            val finalBarcode = extractedBarcode.ifEmpty { data.barcode }
            
            Log.d("AddCarUseCase", "Photo processing complete:")
            Log.d("AddCarUseCase", "  - Thumbnail: $localThumbnail")
            Log.d("AddCarUseCase", "  - Full size: $localFull")
            Log.d("AddCarUseCase", "  - Barcode: $finalBarcode")
            
            // Step 4: Save to storage (Local or Drive) via UserStorageRepository
            val saveResult = userStorageRepository.saveCar(
                data = data,
                localThumbnail = localThumbnail,
                localFull = localFull,
                barcode = finalBarcode
            )
            
            if (saveResult.isFailure) {
                Log.e("AddCarUseCase", "Storage save failed: ${saveResult.exceptionOrNull()?.message}")
                return@withContext saveResult
            }
            
            val carId = saveResult.getOrNull()
            if (carId == null) {
                Log.e("AddCarUseCase", "Car ID is null after successful save")
                return@withContext Result.failure(IllegalStateException("Failed to retrieve car ID"))
            }
            
            Log.i("AddCarUseCase", "✅ Car saved to storage with ID: $carId")
            
            // Step 5: Sync to Firebase Firestore (INCREMENTAL - background, non-blocking)
            // ✅ FIX: Folosim sync incremental cu priorități pentru apariție rapidă în Browse
            // Thumbnail + Data apare în Browse după ~5-6 secunde
            // Full Photo se sync-ează lazy în background
            // ✅ CRITICAL: Launch sync in background WITHOUT blocking - return immediately!
            applicationScope.launch {
                try {
                    carSyncRepository.syncCarIncremental(carId)
                    Log.i("AddCarUseCase", "✅ Car incremental sync completed - appeared in Browse after thumbnail upload")
                } catch (e: Exception) {
                    Log.w("AddCarUseCase", "⚠️ Firestore incremental sync failed: ${e.message}")
                    // Don't fail the entire operation if Firestore sync fails
                    // Car is still saved locally and will appear in My Collection
                    // Sync can be retried later via WorkManager
                }
            }
            Log.i("AddCarUseCase", "✅ Car incremental sync initiated (non-blocking) - will appear in Browse after thumbnail upload")
            
            // Step 6: Update backup to cloud (background, non-blocking)
            // ✅ CRITICAL: Launch backup sync in background WITHOUT blocking - return immediately!
            applicationScope.launch {
                try {
                    userCloudSyncRepository.syncLatestBackup()
                    Log.i("AddCarUseCase", "✅ Latest backup updated successfully")
                } catch (e: Exception) {
                    Log.w("AddCarUseCase", "⚠️ Backup sync failed: ${e.message}")
                    // Don't fail the entire operation if backup sync fails
                    // Car is still saved locally and will appear in My Collection
                    // Backup can be retried later via WorkManager
                }
            }
            Log.i("AddCarUseCase", "✅ Latest backup sync initiated (non-blocking)")
            
            // ✅ CRITICAL: Return Success IMMEDIATELY after local save
            // Navigation happens instantly, sync continues in background
            Log.i("AddCarUseCase", "=== CAR ADDITION COMPLETE ===")
            Result.success(carId)
            
        } catch (e: Exception) {
            Log.e("AddCarUseCase", "Unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Validates input data before processing.
     */
    private fun validateInput(data: CarDataToSync): String? {
        if (data.userId.isEmpty()) {
            return "User ID is required"
        }
        
        if (data.pendingPhotos.isEmpty() && 
            data.preOptimizedThumbnailPath.isEmpty() && 
            data.preOptimizedFullPath.isEmpty()) {
            return "At least one photo is required"
        }
        
        // For Mainline: category and brand are required
        if (data.series == "Mainline" && data.screenType == "Mainline") {
            if (data.category.isEmpty()) {
                return "Category is required for Mainline cars"
            }
            if (data.brand.isEmpty()) {
                return "Brand is required for Mainline cars"
            }
        }
        
        // For Premium: category is required
        if (data.series == "Premium" && data.screenType == "Premium") {
            if (data.category.isEmpty()) {
                return "Category is required for Premium cars"
            }
        }
        
        return null // All good
    }
    
    /**
     * Processes photos: creates optimized versions and extracts barcode.
     * Returns (thumbnailPath, fullSizePath, barcode).
     */
    private suspend fun processPhotos(data: CarDataToSync): Triple<String, String, String> {
        // If photos are already optimized (from CameraCaptureScreen), use them
        if (data.preOptimizedThumbnailPath.isNotEmpty() && data.preOptimizedFullPath.isNotEmpty()) {
            Log.d("AddCarUseCase", "Using pre-optimized photos")
            return Triple(data.preOptimizedThumbnailPath, data.preOptimizedFullPath, "")
        }
        
        val photosToProcess = data.pendingPhotos
        if (photosToProcess.isEmpty()) {
            Log.w("AddCarUseCase", "No photos to process")
            return Triple("", "", "")
        }
        
        // Order-based identification: first photo = FRONT, second photo = BACK
        val frontPhoto = photosToProcess.getOrNull(0)
        val backPhoto = photosToProcess.getOrNull(1)
        
        var extractedBarcode = ""
        
        // Extract barcode from back photo first
        if (backPhoto != null && data.barcode.isEmpty()) {
            extractedBarcode = photoProcessingRepository.extractBarcodeFromImage(backPhoto.savedPath)
            Log.d("AddCarUseCase", "Barcode extracted from back photo: '$extractedBarcode'")
            
            // Delete back photo after barcode extraction
            photoProcessingRepository.deleteTemporaryPhoto(backPhoto.savedPath)
            Log.d("AddCarUseCase", "Back photo deleted after barcode extraction")
        }
        
        // Create optimized versions from front photo
        if (frontPhoto != null) {
            // Use app's internal cache directory for optimized photos
            val optimizedDir = File(context.cacheDir, "optimized_photos")
            val photoVersions = photoProcessingRepository.createOptimizedVersions(
                frontPhoto.savedPath,
                optimizedDir,
                "car_${System.currentTimeMillis()}"
            )
            
            Log.d("AddCarUseCase", "Optimized versions created:")
            Log.d("AddCarUseCase", "  - Thumbnail: ${photoVersions.thumbnailPath}")
            Log.d("AddCarUseCase", "  - Full size: ${photoVersions.fullSizePath}")
            
            return Triple(photoVersions.thumbnailPath, photoVersions.fullSizePath, extractedBarcode)
        }
        
        return Triple("", "", extractedBarcode)
    }
    
    /**
     * Ensures UserEntity exists in local database to prevent FOREIGN KEY constraint errors.
     */
    private suspend fun ensureUserEntityExists(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val existingUser = userDao.getById(firebaseUser.uid)
        if (existingUser == null) {
            Log.d("AddCarUseCase", "Creating UserEntity for user: ${firebaseUser.uid}")
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
            Log.d("AddCarUseCase", "✅ UserEntity created successfully")
        } else {
            Log.d("AddCarUseCase", "UserEntity already exists for user: ${firebaseUser.uid}")
        }
    }
    
    /**
     * Checks for duplicate cars before saving.
     * 
     * ✅ FIX: Un barcode poate aparține unui lot de producție cu multe mașini diferite (culori, loturi diferite).
     * Prin urmare, NU verificăm duplicate după barcode - permite salvarea tuturor mașinilor cu același barcode.
     * 
     * Verificăm duplicate DOAR pentru mașini FĂRĂ barcode (după model + brand + year),
     * dar fără a verifica câmpurile editabile (color, notes) care nu influențează procesul de salvare.
     * 
     * @param data Car data to check
     * @param userId Current user ID
     * @return DuplicateCheckResult indicating if duplicate exists and reason
     */
    private suspend fun checkForDuplicates(data: CarDataToSync, userId: String): DuplicateCheckResult {
        try {
            // ✅ REMOVED: Verificarea după barcode
            // Un barcode poate corespunde unui lot de producție cu 1000+ mașini diferite.
            // Utilizatorul poate să salveze câte mașini dorește cu același barcode (culori diferite, loturi diferite).
            
            // Verificare duplicate DOAR pentru mașini FĂRĂ barcode (după model + brand + year)
            // Fără a verifica câmpurile editabile (color, notes) care nu influențează salvare
            if (data.barcode.isEmpty() && data.name.isNotEmpty() && data.brand.isNotEmpty()) {
                val existingCars = carDao.getCarsForUser(userId).first()
                val duplicateByName = existingCars.firstOrNull { car ->
                    car.barcode.isEmpty() && // Doar mașini fără barcode
                    car.model.equals(data.name, ignoreCase = true) &&
                    car.brand.equals(data.brand, ignoreCase = true) &&
                    car.year == (data.year ?: 0)
                    // ✅ NU verificăm color - permite salvarea aceluiași model cu culori diferite
                }
                
                if (duplicateByName != null) {
                    Log.w("AddCarUseCase", "Duplicate found by name (no barcode): ${duplicateByName.model} (${duplicateByName.brand} ${duplicateByName.year})")
                    return DuplicateCheckResult(
                        isDuplicate = true,
                        message = "A similar car already exists in your collection: ${duplicateByName.model} (${duplicateByName.brand} ${duplicateByName.year})"
                    )
                }
            }
            
            Log.d("AddCarUseCase", "No duplicates found - car is safe to save")
            return DuplicateCheckResult(isDuplicate = false, message = "")
            
        } catch (e: Exception) {
            Log.e("AddCarUseCase", "Error checking for duplicates: ${e.message}", e)
            // If duplicate check fails, allow the save to proceed (fail-safe)
            return DuplicateCheckResult(isDuplicate = false, message = "")
        }
    }
}

/**
 * Result of duplicate checking.
 */
data class DuplicateCheckResult(
    val isDuplicate: Boolean,
    val message: String
)
