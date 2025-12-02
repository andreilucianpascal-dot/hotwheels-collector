package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

/**
 * LocalRepository handles saving cars and photos to local device storage.
 * 
 * RESPONSIBILITIES:
 * 1. Save car metadata to Room Database
 * 2. Copy photos from temporary cache to permanent internal storage
 * 3. Create PhotoEntity records in Room Database
 * 4. Handle cleanup on errors
 */
class LocalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao
) : UserStorageRepository {
    
    /**
     * Saves a car with its photos to local device storage.
     * 
     * @param data Complete car data including metadata
     * @param localThumbnail Path to optimized thumbnail (already processed)
     * @param localFull Path to optimized full-size photo (already processed)
     * @param barcode Extracted barcode
     * @return Result containing car ID if successful, or error if failed
     */
    override suspend fun saveCar(
        data: CarDataToSync,
        localThumbnail: String,
        localFull: String,
        barcode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val carId = UUID.randomUUID().toString()
        
        try {
            Log.d("LocalRepository", "=== STARTING LOCAL SAVE ===")
            Log.d("LocalRepository", "Car ID: $carId")
            Log.d("LocalRepository", "Thumbnail: $localThumbnail")
            Log.d("LocalRepository", "Full size: $localFull")
            
            // ✅ CONTRACT: Paths are guaranteed valid from CameraManager → AddCarUseCase
            // CameraManager generates photos and AddCarUseCase validates them
            // Trust the contract - no redundant checks!
            val cleanThumbnailPath = localThumbnail.replace("file://", "")
            val cleanFullPath = localFull.replace("file://", "")
            
            // Create permanent storage directory
            val photoDir = createPhotoDirectory(data.userId, carId)
            Log.d("LocalRepository", "Photo directory: ${photoDir.absolutePath}")
            
            // Copy photos to permanent storage
            val permanentThumbnail = copyPhotoToInternalStorage(cleanThumbnailPath, photoDir, "thumbnail.jpg")
            val permanentFull = copyPhotoToInternalStorage(cleanFullPath, photoDir, "full.jpg")
            
            Log.d("LocalRepository", "Photos copied to permanent storage:")
            Log.d("LocalRepository", "  - Thumbnail: $permanentThumbnail")
            Log.d("LocalRepository", "  - Full: $permanentFull")
            
            // 🔍 DEBUG: Verify files exist after copying
            Log.d("LocalRepository", "File verification:")
            Log.d("LocalRepository", "  - Thumbnail exists: ${File(permanentThumbnail).exists()}")
            Log.d("LocalRepository", "  - Full exists: ${File(permanentFull).exists()}")
            Log.d("LocalRepository", "  - Thumbnail size: ${File(permanentThumbnail).length()} bytes")
            Log.d("LocalRepository", "  - Full size: ${File(permanentFull).length()} bytes")
            
            // Create CarEntity
            // ✅ FIX: Pentru Premium, subseries trebuie să fie "category/subcategory" SAU doar "subcategory"
            val subseries = if (data.isPremium) {
                val normalizedCategory = normalizePremiumCategory(data.category)
                val normalizedSubcategory = normalizePremiumSubcategory(data.subcategory)
                if (!normalizedSubcategory.isNullOrEmpty()) {
                    "$normalizedCategory/$normalizedSubcategory"
                } else {
                    normalizedCategory
                }
            } else {
                data.category
            }
            
            Log.d("LocalRepository", "✅ Computed subseries: '$subseries' (Premium: ${data.isPremium}, Category: '${data.category}', Subcategory: '${data.subcategory}')")
            
            val carEntity = CarEntity(
                id = carId,
                userId = data.userId,
                model = data.name,
                brand = data.brand,
                series = data.series,
                subseries = subseries, // ✅ Fixed: Use computed subseries
                folderPath = data.category, // Use category as folderPath for proper organization
                color = data.color,
                year = data.year ?: 0,
                barcode = barcode,
                notes = data.notes,
                isTH = data.isTH,
                isSTH = data.isSTH,
                isPremium = data.isPremium, // ✅ Fixed: Set isPremium field
                timestamp = System.currentTimeMillis(),
                lastModified = Date(),
                syncStatus = SyncStatus.PENDING_UPLOAD,
                photoUrl = permanentFull,
                frontPhotoPath = permanentFull,
                combinedPhotoPath = permanentThumbnail
            )
            
            // 🔍 DEBUG: Log CarEntity values before saving
            Log.d("LocalRepository", "=== CARENTITY BEFORE SAVE ===")
            Log.d("LocalRepository", "  - ID: ${carEntity.id}")
            Log.d("LocalRepository", "  - UserID: ${carEntity.userId}")
            Log.d("LocalRepository", "  - Model: ${carEntity.model}")
            Log.d("LocalRepository", "  - Brand: ${carEntity.brand}")
            Log.d("LocalRepository", "  - Series: ${carEntity.series}")
            Log.d("LocalRepository", "  - Subseries: ${carEntity.subseries}")
            Log.d("LocalRepository", "  - isPremium: ${carEntity.isPremium}")
            Log.d("LocalRepository", "  - isTH: ${carEntity.isTH}")
            Log.d("LocalRepository", "  - isSTH: ${carEntity.isSTH}")
            Log.d("LocalRepository", "  - photoUrl: ${carEntity.photoUrl}")
            Log.d("LocalRepository", "  - combinedPhotoPath: ${carEntity.combinedPhotoPath}")
            
            // Save car to Room Database
            carDao.insertCar(carEntity)
            Log.i("LocalRepository", "✅ Car saved to Room Database")
            
            // 🔍 DEBUG: Verify car was saved by querying it back
            val savedCar = carDao.getCarById(carId)
            if (savedCar != null) {
                Log.d("LocalRepository", "✅ Verified: Car retrieved from DB")
                Log.d("LocalRepository", "  - Retrieved Model: ${savedCar.model}")
                Log.d("LocalRepository", "  - Retrieved Series: ${savedCar.series}")
                Log.d("LocalRepository", "  - Retrieved isPremium: ${savedCar.isPremium}")
            } else {
                Log.e("LocalRepository", "❌ ERROR: Car NOT found in DB after save!")
            }
            
            // 🔍 DEBUG: Log what was saved in CarEntity
            Log.d("LocalRepository", "CarEntity photo paths:")
            Log.d("LocalRepository", "  - photoUrl: ${carEntity.photoUrl}")
            Log.d("LocalRepository", "  - frontPhotoPath: ${carEntity.frontPhotoPath}")
            Log.d("LocalRepository", "  - combinedPhotoPath: ${carEntity.combinedPhotoPath}")
            
            // Create PhotoEntity
            val photoEntity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                carId = carId,
                localPath = permanentThumbnail,
                thumbnailPath = permanentThumbnail,
                fullSizePath = permanentFull,
                cloudPath = "",
                type = PhotoType.FRONT,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                isTemporary = false,
                barcode = barcode.takeIf { it.isNotEmpty() },
                contributorUserId = data.userId
            )
            
            // Save photo to Room Database
            photoDao.insertPhoto(photoEntity)
            Log.i("LocalRepository", "✅ Photo saved to Room Database")
            
            Log.i("LocalRepository", "=== LOCAL SAVE COMPLETE ===")
            Result.success(carId)
            
        } catch (e: Exception) {
            Log.e("LocalRepository", "❌ Local save failed: ${e.message}", e)
            
            // Cleanup on error: delete car directory
            try {
                val photoDir = File(context.filesDir, "photos/${data.userId}/$carId")
                if (photoDir.exists()) {
                    photoDir.deleteRecursively()
                    Log.d("LocalRepository", "Cleaned up photo directory after error")
                }
            } catch (cleanupError: Exception) {
                Log.w("LocalRepository", "Failed to cleanup: ${cleanupError.message}")
            }
            
            Result.failure(e)
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
            Log.d("LocalRepository", "Created photo directory: ${photoDir.absolutePath}")
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
        
        Log.d("LocalRepository", "Photo copied: ${sourceFile.absolutePath} -> ${destFile.absolutePath}")
        
        return destFile.absolutePath
    }
}

private fun normalizePremiumCategory(category: String): String {
    return when (category.lowercase()) {
        "car_culture" -> "Car Culture"
        "pop_culture" -> "Pop Culture"
        "boulevard" -> "Boulevard"
        "f1" -> "F1"
        "rlc" -> "RLC"
        "large_scale" -> "1:43 Scale"
        "others_premium" -> "Others Premium"
        else -> category.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

private fun normalizePremiumSubcategory(subcategory: String?): String? {
    if (subcategory.isNullOrEmpty()) return subcategory
    return when (subcategory.lowercase()) {
        "race_day" -> "Race Day"
        "circuit_legends" -> "Circuit Legends"
        "team_transport" -> "Team Transport"
        "jay_lenos_garage" -> "Jay Leno's Garage"
        "rtr_vehicles" -> "RTR Vehicles"
        "real_riders" -> "Real Riders"
        "fast_wagons" -> "Fast Wagons"
        "speed_machine" -> "Speed Machine"
        "japan_historics" -> "Japan Historics"
        "hammer_drop" -> "Hammer Drop"
        "slide_street" -> "Slide Street"
        "terra_trek" -> "Terra Trek"
        "exotic_envy" -> "Exotic Envy"
        "cargo_containers" -> "Cargo Containers"
        "modern_classics" -> "Modern Classics"
        "fast_and_furious" -> "Fast & Furious"
        "mario_kart" -> "Mario Kart"
        "forza_motorsport" -> "Forza Motorsport"
        "gran_turismo" -> "Gran Turismo"
        "top_gun" -> "Top Gun"
        "jurassic_world" -> "Jurassic World"
        "back_to_the_future" -> "Back to the Future"
        "looney_tunes" -> "Looney Tunes"
        else -> subcategory.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
