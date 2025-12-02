package com.example.hotwheelscollectors.data.repository
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.hotwheelscollectors.data.local.dao.CarDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CarSyncRepository is responsible ONLY for syncing car data to Firebase Firestore.
 * 
 * RESPONSIBILITIES:
 * 1. Read car data from Room Database
 * 2. Sync to Firebase Firestore (globalCars and globalBarcodes collections)
 * 
 * This repository NO LONGER handles:
 * - Photo processing (done by PhotoProcessingRepository)
 * - Local saving (done by LocalRepository or GoogleDriveRepository)
 * - Decision making (done by AddCarUseCase)
 */
@Singleton
class CarSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val firestoreRepository: FirestoreRepository,
    private val storageRepository: StorageRepository
) {
    
    /**
     * Syncs a car to Firebase Firestore.
     * Reads the car from Room Database and uploads to Firestore.
     * 
     * @param carId The ID of the car to sync
     */
    suspend fun syncCarToFirestore(carId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d("CarSyncRepository", "=== STARTING FIRESTORE SYNC ===")
            Log.d("CarSyncRepository", "Car ID: $carId")
            
            // Read car from Room Database
            val car = carDao.getCarById(carId)
            if (car == null) {
                Log.e("CarSyncRepository", "Car not found in local database: $carId")
                return@withContext
            }
            
            Log.d("CarSyncRepository", "Car found in local DB:")
            Log.d("CarSyncRepository", "  - Model: ${car.model}")
            Log.d("CarSyncRepository", "  - Brand: ${car.brand}")
            Log.d("CarSyncRepository", "  - Series: ${car.series}")
            Log.d("CarSyncRepository", "  - Subseries: ${car.subseries}")
            
            // Upload photos to Firestore Storage and get global URLs
            val fullPhotoUrl = uploadPhotoToFirestore(car.photoUrl ?: "", carId, "full")
            val thumbnailUrl = uploadPhotoToFirestore(car.combinedPhotoPath ?: "", carId, "thumbnail")
            val barcodeUrl = if (car.barcode.isNotEmpty()) {
                // Try to find barcode photo - this might need adjustment based on your barcode storage logic
                uploadPhotoToFirestore("", carId, "barcode")
            } else null
            
            Log.d("CarSyncRepository", "Firestore Storage URLs:")
            Log.d("CarSyncRepository", "  - Full: $fullPhotoUrl")
            Log.d("CarSyncRepository", "  - Thumbnail: $thumbnailUrl")
            Log.d("CarSyncRepository", "  - Barcode: $barcodeUrl")
            
            // Sync to Firestore globalCars collection
            try {
                val result = firestoreRepository.saveAllCarsToGlobalDatabase(
                    carId = carId,
                    carName = car.model,
                    brand = car.brand,
                    series = car.series,
                    year = car.year,
                    color = car.color.takeIf { it.isNotEmpty() },
                    frontPhotoUrl = thumbnailUrl, // Thumbnail for Browse Mainline
                    backPhotoUrl = fullPhotoUrl, // Full photo for detailed view
                    croppedBarcodeUrl = barcodeUrl,
                    category = car.series, // "Mainline", "Premium", "Others"
                    subcategory = car.subseries, // Rally, Car Culture, TH, STH, etc.
                    barcode = car.barcode.takeIf { it.isNotEmpty() },
                    isTH = car.isTH,
                    isSTH = car.isSTH
                )
                
                if (result.isSuccess) {
                    Log.i("CarSyncRepository", "✅ Saved to globalCars collection")
                } else {
                    Log.w("CarSyncRepository", "Failed to save to globalCars: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("CarSyncRepository", "Firestore globalCars save failed: ${e.message}", e)
            }
            
            // Sync to Firestore globalBarcodes collection (if barcode exists)
            if (car.barcode.isNotEmpty()) {
                try {
                    val result = firestoreRepository.saveToGlobalDatabase(
                        barcode = car.barcode,
                        carName = car.model,
                        brand = car.brand,
                        series = car.series,
                        year = car.year,
                        color = car.color.takeIf { it.isNotEmpty() },
                        frontPhotoUrl = thumbnailUrl, // Thumbnail for Browse Mainline
                        backPhotoUrl = fullPhotoUrl, // Full photo for detailed view
                        croppedBarcodeUrl = barcodeUrl,
                        category = car.series,
                        subcategory = car.subseries
                    )
                    
                    if (result.isSuccess) {
                        Log.i("CarSyncRepository", "✅ Saved to globalBarcodes collection")
                    } else {
                        Log.w("CarSyncRepository", "Failed to save to globalBarcodes: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "Firestore globalBarcodes save failed: ${e.message}", e)
                }
            }
            
            Log.i("CarSyncRepository", "=== FIRESTORE SYNC COMPLETE ===")
            
        } catch (e: Exception) {
            Log.e("CarSyncRepository", "❌ Firestore sync failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Uploads a photo to Firestore Storage and returns the download URL.
     * 
     * @param localPhotoPath Path to the local photo file
     * @param carId Car ID for organizing photos in storage
     * @param photoType Type of photo: "thumbnail", "full", or "barcode"
     * @return Firestore Storage download URL, or empty string if upload fails
     */
    private suspend fun uploadPhotoToFirestore(
        localPhotoPath: String, 
        carId: String, 
        photoType: String
    ): String {
        return try {
            if (localPhotoPath.isEmpty()) {
                Log.w("CarSyncRepository", "Empty photo path for $photoType, skipping upload")
                return ""
            }

            val photoFile = File(localPhotoPath)
            if (!photoFile.exists()) {
                Log.w("CarSyncRepository", "Photo file does not exist: $localPhotoPath")
                return ""
            }

            Log.d("CarSyncRepository", "Uploading $photoType photo to Firestore Storage...")
            Log.d("CarSyncRepository", "  - Local path: $localPhotoPath")
            Log.d("CarSyncRepository", "  - Car ID: $carId")

            // Convert file to bitmap
            val bitmap = BitmapFactory.decodeFile(localPhotoPath)
            if (bitmap == null) {
                Log.e("CarSyncRepository", "Failed to decode bitmap from: $localPhotoPath")
                return ""
            }

            // Upload to Firestore Storage
            val storagePath = "mainline/$carId/$photoType"
            val firestoreUrl = storageRepository.savePhoto(bitmap, storagePath)
            
            Log.i("CarSyncRepository", "✅ $photoType photo uploaded to Firestore Storage")
            Log.d("CarSyncRepository", "  - Firestore URL: $firestoreUrl")
            
            firestoreUrl
            
        } catch (e: Exception) {
            Log.e("CarSyncRepository", "Failed to upload $photoType photo to Firestore Storage: ${e.message}", e)
            ""
        }
    }
}
