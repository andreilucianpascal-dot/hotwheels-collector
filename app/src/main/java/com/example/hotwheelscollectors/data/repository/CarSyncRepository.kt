package com.example.hotwheelscollectors.data.repository
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.PhotoSyncStatus
import com.example.hotwheelscollectors.data.local.entities.DataSyncStatus
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
     * ✅ FIX: Verifică duplicate înainte de upload, dar nu blochează - skip-ează doar dacă există deja.
     * Salvarea locală se face întotdeauna, indiferent de duplicate în Firestore.
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
            Log.d("CarSyncRepository", "  - Barcode: ${car.barcode}")
            
            // ✅ FIX: Verifică dacă barcode-ul există deja în globalBarcodes
            // Dacă există, NU salvez în globalBarcodes (pentru că e același barcode)
            // DAR salvez în globalCars (pentru că mașina poate avea descriere diferită)
            var barcodeExistsInGlobal = false
            Log.d("CarSyncRepository", "🔍 Checking barcode status:")
            Log.d("CarSyncRepository", "  - Barcode: '${car.barcode}'")
            Log.d("CarSyncRepository", "  - Barcode isEmpty: ${car.barcode.isEmpty()}")
            Log.d("CarSyncRepository", "  - Barcode isBlank: ${car.barcode.isBlank()}")
            
            if (car.barcode.isNotEmpty() && car.barcode.isNotBlank()) {
                try {
                    Log.d("CarSyncRepository", "  → Checking if barcode exists in globalBarcodes...")
                    val existingBarcode = firestoreRepository.checkBarcodeInGlobalDatabase(car.barcode)
                    if (existingBarcode != null) {
                        Log.d("CarSyncRepository", "⚠️ Barcode ${car.barcode} already exists in globalBarcodes")
                        Log.d("CarSyncRepository", "  Existing: ${existingBarcode.carName} by ${existingBarcode.brand}")
                        Log.d("CarSyncRepository", "  → Skipping globalBarcodes save, but will save to globalCars")
                        barcodeExistsInGlobal = true
                    } else {
                        Log.d("CarSyncRepository", "✅ Barcode ${car.barcode} NOT found in globalBarcodes - WILL SAVE")
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌ Error checking barcode in globalBarcodes: ${e.message}", e)
                    e.printStackTrace()
                    // Continue with save attempt if check fails - assume barcode doesn't exist
                    barcodeExistsInGlobal = false
                }
            } else {
                Log.w("CarSyncRepository", "⚠️ Car has no barcode (empty or blank) - skipping globalBarcodes save")
            }
            
            // Upload photos to Firestore Storage and get global URLs (ÎNTOTDEAUNA - pentru globalCars)
            // ✅ FIX: Gestionează erorile de upload - dacă upload-urile esentiale eșuează, nu salva în Firestore
            var fullPhotoUrl = ""
            var thumbnailUrl = ""
            var barcodeUrl: String? = null
            
            try {
                // Încearcă să uploadeze thumbnail (esential pentru Browse)
                val thumbnailPath = car.combinedPhotoPath ?: ""
                Log.d("CarSyncRepository", "🔄 Uploading thumbnail:")
                Log.d("CarSyncRepository", "  - Series: ${car.series}")
                Log.d("CarSyncRepository", "  - Car ID: $carId")
                Log.d("CarSyncRepository", "  - Thumbnail path: '$thumbnailPath'")
                Log.d("CarSyncRepository", "  - Path isEmpty: ${thumbnailPath.isEmpty()}")
                
                if (thumbnailPath.isNotEmpty()) {
                    val thumbnailFile = java.io.File(thumbnailPath)
                    Log.d("CarSyncRepository", "  - File exists: ${thumbnailFile.exists()}")
                    Log.d("CarSyncRepository", "  - File size: ${if (thumbnailFile.exists()) thumbnailFile.length() else 0} bytes")
                }
                
                thumbnailUrl = uploadPhotoToFirestore(thumbnailPath, carId, "thumbnail", car.series)
                Log.d("CarSyncRepository", "  - Thumbnail URL result: ${if (thumbnailUrl.isNotEmpty()) "✅ Success: $thumbnailUrl" else "❌ Empty"}")
            } catch (e: Exception) {
                Log.e("CarSyncRepository", "❌ Failed to upload thumbnail photo: ${e.message}", e)
                e.printStackTrace()
                // ✅ FIX: Dacă thumbnail-ul eșuează, totuși continuăm (poate fullPhotoUrl merge)
            }
            
            try {
                // Încearcă să uploadeze full photo (esential pentru detalii)
                val fullPhotoPath = car.photoUrl ?: ""
                Log.d("CarSyncRepository", "🔄 Uploading full photo:")
                Log.d("CarSyncRepository", "  - Full photo path: '$fullPhotoPath'")
                Log.d("CarSyncRepository", "  - Path isEmpty: ${fullPhotoPath.isEmpty()}")
                
                if (fullPhotoPath.isNotEmpty()) {
                    val fullPhotoFile = java.io.File(fullPhotoPath)
                    Log.d("CarSyncRepository", "  - File exists: ${fullPhotoFile.exists()}")
                    Log.d("CarSyncRepository", "  - File size: ${if (fullPhotoFile.exists()) fullPhotoFile.length() else 0} bytes")
                }
                
                fullPhotoUrl = uploadPhotoToFirestore(fullPhotoPath, carId, "full", car.series)
                Log.d("CarSyncRepository", "  - Full photo URL result: ${if (fullPhotoUrl.isNotEmpty()) "✅ Success: $fullPhotoUrl" else "❌ Empty"}")
            } catch (e: Exception) {
                Log.e("CarSyncRepository", "❌ Failed to upload full photo: ${e.message}", e)
                e.printStackTrace()
                // ✅ FIX: Dacă full photo eșuează, totuși continuăm
            }
            
            // Barcode photo este opțional
            if (car.barcode.isNotEmpty()) {
                try {
                    // Note: barcode photo path ar trebui să fie setat în CarEntity dacă există
                    val barcodePhotoPath = "" // TODO: Adaugă câmp pentru barcode photo path dacă e necesar
                    barcodeUrl = uploadPhotoToFirestore(barcodePhotoPath, carId, "barcode", car.series)
                } catch (e: Exception) {
                    Log.w("CarSyncRepository", "⚠️ Failed to upload barcode photo (optional): ${e.message}")
                    // Barcode photo eșec nu este critic
                }
            }
            
            Log.d("CarSyncRepository", "Firestore Storage URLs:")
            Log.d("CarSyncRepository", "  - Full: $fullPhotoUrl")
            Log.d("CarSyncRepository", "  - Thumbnail: $thumbnailUrl")
            Log.d("CarSyncRepository", "  - Barcode: $barcodeUrl")
            
            // ✅ FIX: Verifică dacă cel puțin un URL esential este disponibil
            // Dacă nici thumbnail, nici full nu au reușit, loghează avertisment dar continuă
            if (thumbnailUrl.isEmpty() && fullPhotoUrl.isEmpty()) {
                Log.w("CarSyncRepository", "⚠️ WARNING: Both thumbnail and full photo uploads failed!")
                Log.w("CarSyncRepository", "  → Car will be saved to Firestore WITHOUT photo URLs")
                Log.w("CarSyncRepository", "  → User should check Firebase Storage Rules to fix 403 Permission Denied errors")
            }
            
            // Sync to Firestore globalCars collection
            // ✅ FIX: Salvez ÎNTOTDEAUNA în globalCars (carId este unic UUID per utilizator)
            // Mașina poate avea același barcode dar descriere diferită (model, brand, year, color)
            // ✅ FIX: Salvez chiar dacă photo URLs sunt goale (pentru că datele textuale sunt utile)
            try {
                // ✅ FIX: Set category correctly based on car type
                // For Premium: category = series ("Premium")
                // For Silver Series: category = series ("Silver Series")
                // For TH: category = "Treasure Hunt" (from subseries "TH")
                // For STH: category = "Super Treasure Hunt" (from subseries "STH")
                // For others: category = series
                val category = when {
                    car.isSTH -> "Super Treasure Hunt"
                    car.isTH -> "Treasure Hunt"
                    car.series.equals("Silver Series", ignoreCase = true) -> "Silver Series"
                    else -> car.series
                }
                
                Log.d("CarSyncRepository", "🔄 Saving to globalCars:")
                Log.d("CarSyncRepository", "  - Category: $category")
                Log.d("CarSyncRepository", "  - Series: ${car.series}")
                Log.d("CarSyncRepository", "  - Thumbnail URL: ${if (thumbnailUrl.isNotEmpty()) thumbnailUrl else "EMPTY"}")
                Log.d("CarSyncRepository", "  - Full Photo URL: ${if (fullPhotoUrl.isNotEmpty()) fullPhotoUrl else "EMPTY"}")
                
                val result = firestoreRepository.saveAllCarsToGlobalDatabase(
                    localCarId = carId,
                    carName = car.model,
                    brand = car.brand,
                    series = car.series,
                    year = car.year,
                    color = car.color.takeIf { it.isNotEmpty() },
                    frontPhotoUrl = thumbnailUrl.ifEmpty { null }, // Null dacă e gol
                    backPhotoUrl = fullPhotoUrl.ifEmpty { null }, // Null dacă e gol
                    croppedBarcodeUrl = barcodeUrl?.takeIf { it.isNotEmpty() },
                    category = category,
                    subcategory = car.subseries, // Rally, Car Culture, TH, STH, etc.
                    barcode = car.barcode.takeIf { it.isNotEmpty() },
                    isTH = car.isTH,
                    isSTH = car.isSTH
                )
                
                if (result.isSuccess) {
                    Log.i("CarSyncRepository", "✅ Saved to globalCars collection")
                    Log.i("CarSyncRepository", "  → frontPhotoUrl: ${thumbnailUrl.ifEmpty { "EMPTY" }}")
                } else {
                    Log.w("CarSyncRepository", "Failed to save to globalCars: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("CarSyncRepository", "Firestore globalCars save failed: ${e.message}", e)
                // Nu returnăm eroare - doar logăm
            }
            
            // Sync to Firestore globalBarcodes collection
            // ✅ FIX: Salvez în globalBarcodes DOAR dacă barcode-ul NU există deja
            // (Barcode-ul este comun pentru același model - nu are sens să-l salvez de două ori)
            // ✅ FIX: Pentru Silver Series și Mainline, setăm category corect
            // ✅ CRITICAL: Salvez barcode-ul CHIAR DACĂ thumbnail-ul eșuează (barcode-ul este esențial pentru căutare)
            Log.d("CarSyncRepository", "🔍 Final barcode save check:")
            Log.d("CarSyncRepository", "  - Barcode: '${car.barcode}'")
            Log.d("CarSyncRepository", "  - Barcode isNotEmpty: ${car.barcode.isNotEmpty()}")
            Log.d("CarSyncRepository", "  - Barcode isNotBlank: ${car.barcode.isNotBlank()}")
            Log.d("CarSyncRepository", "  - barcodeExistsInGlobal: $barcodeExistsInGlobal")
            
            if (car.barcode.isNotEmpty() && car.barcode.isNotBlank() && !barcodeExistsInGlobal) {
                try {
                    // ✅ FIX: Set category correctly based on car type
                    // For Premium: category = series ("Premium")
                    // For Silver Series: category = series ("Silver Series")
                    // For TH: category = "Treasure Hunt"
                    // For STH: category = "Super Treasure Hunt"
                    // For others: category = series
                    val category = when {
                        car.isSTH -> "Super Treasure Hunt"
                        car.isTH -> "Treasure Hunt"
                        car.series.equals("Silver Series", ignoreCase = true) -> "Silver Series"
                        else -> car.series
                    }
                    
                    Log.d("CarSyncRepository", "🔄 Attempting to save barcode to globalBarcodes:")
                    Log.d("CarSyncRepository", "  - Barcode: ${car.barcode}")
                    Log.d("CarSyncRepository", "  - Category: $category")
                    Log.d("CarSyncRepository", "  - Series: ${car.series}")
                    
                    val result = firestoreRepository.saveToGlobalDatabase(
                        barcode = car.barcode,
                        carName = car.model,
                        brand = car.brand,
                        series = car.series,
                        year = car.year,
                        color = car.color.takeIf { it.isNotEmpty() },
                        frontPhotoUrl = thumbnailUrl.ifEmpty { null }, // Thumbnail for Browse
                        backPhotoUrl = fullPhotoUrl.ifEmpty { null }, // Full photo for detailed view
                        croppedBarcodeUrl = barcodeUrl?.takeIf { it.isNotEmpty() },
                        category = category,
                        subcategory = car.subseries
                    )
                    
                    if (result.isSuccess) {
                        Log.i("CarSyncRepository", "✅✅✅ Successfully saved to globalBarcodes collection ✅✅✅")
                        Log.i("CarSyncRepository", "  → Barcode: ${car.barcode}")
                        Log.i("CarSyncRepository", "  → Category: $category")
                        Log.i("CarSyncRepository", "  → frontPhotoUrl: ${if (thumbnailUrl.isNotEmpty()) thumbnailUrl else "EMPTY"}")
                        Log.i("CarSyncRepository", "  → backPhotoUrl: ${if (fullPhotoUrl.isNotEmpty()) fullPhotoUrl else "EMPTY"}")
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("CarSyncRepository", "❌❌❌ FAILED to save to globalBarcodes: ${error?.message}")
                        error?.printStackTrace()
                        // ✅ CRITICAL: Log error but don't throw - we want to continue with globalCars save
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌❌❌ EXCEPTION saving to globalBarcodes: ${e.message}", e)
                    e.printStackTrace()
                    // ✅ CRITICAL: Log error but don't throw - we want to continue with globalCars save
                }
            } else if (car.barcode.isNotEmpty() && car.barcode.isNotBlank() && barcodeExistsInGlobal) {
                Log.d("CarSyncRepository", "⚠️ Skipped globalBarcodes save (barcode already exists)")
            } else {
                if (car.barcode.isEmpty() || car.barcode.isBlank()) {
                    Log.w("CarSyncRepository", "⚠️ Skipped globalBarcodes save (barcode is empty or blank)")
                } else {
                    Log.w("CarSyncRepository", "⚠️ Skipped globalBarcodes save (unknown reason)")
                }
            }
            
            Log.i("CarSyncRepository", "=== FIRESTORE SYNC COMPLETE ===")
            
        } catch (e: Exception) {
            Log.e("CarSyncRepository", "❌ Firestore sync failed: ${e.message}", e)
            // ✅ FIX: Nu mai aruncăm excepție - doar logăm eroarea
            // Salvarea locală s-a făcut deja cu succes, sync-ul Firestore este opțional
        }
    }

    /**
     * Uploads a photo to Firestore Storage and returns the download URL.
     * 
     * @param localPhotoPath Path to the local photo file
     * @param carId Car ID for organizing photos in storage
     * @param photoType Type of photo: "thumbnail", "full", or "barcode"
     * @param carSeries Car series to determine storage folder
     * @return Firestore Storage download URL, or empty string if upload fails
     */
    private suspend fun uploadPhotoToFirestore(
        localPhotoPath: String, 
        carId: String, 
        photoType: String,
        carSeries: String
    ): String {
        // ✅ FIX: Returnează string gol doar dacă path-ul este gol sau fișierul nu există
        // Dacă upload-ul eșuează (excepție), aruncă excepția mai departe
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
            // ✅ FIX: Dacă nu putem decoda bitmap-ul, returnează gol (nu e o eroare critică)
                return ""
            }

            // Determine storage path based on car series
            // ✅ FIX: Handle case-insensitive matching for Silver Series
            val seriesLower = carSeries.lowercase().trim()
            val storagePath = when {
                seriesLower.contains("premium") -> "premium/$carId/$photoType"
                seriesLower.contains("silver") && seriesLower.contains("series") -> "silver_series/$carId/$photoType"
                seriesLower.contains("treasure") && seriesLower.contains("hunt") && seriesLower.contains("super") -> "super_treasure_hunt/$carId/$photoType"
                seriesLower.contains("treasure") && seriesLower.contains("hunt") -> "treasure_hunt/$carId/$photoType"
                seriesLower.contains("other") -> "others/$carId/$photoType"
                else -> "mainline/$carId/$photoType" // Default to mainline for "Mainline" series
            }
            
            Log.d("CarSyncRepository", "Storage path determined:")
            Log.d("CarSyncRepository", "  - Input series: '$carSeries'")
            Log.d("CarSyncRepository", "  - Normalized: '$seriesLower'")
            Log.d("CarSyncRepository", "  - Storage path: '$storagePath'")
            
            Log.d("CarSyncRepository", "Using storage path: $storagePath for series: $carSeries")
        
        // ✅ FIX: savePhoto() acum aruncă excepția dacă upload-ul eșuează
        // Astfel, știm clar când upload-ul nu a reușit
            val firestoreUrl = storageRepository.savePhoto(bitmap, storagePath)
            
        // ✅ FIX: Verifică dacă URL-ul este gol (nu ar trebui să ajungă aici dacă savePhoto aruncă excepție)
        if (firestoreUrl.isEmpty()) {
            Log.w("CarSyncRepository", "⚠️ Upload succeeded but URL is empty for $photoType")
        } else {
            Log.i("CarSyncRepository", "✅ $photoType photo uploaded to Firestore Storage")
            Log.d("CarSyncRepository", "  - Firestore URL: $firestoreUrl")
        }
        
        return firestoreUrl
    }

    /**
     * Incremental sync with priorities and retry logic.
     * 
     * PRIORITIES:
     * 1. Thumbnail (PRIORITATE 1 - apare în Browse)
     * 2. Firestore Data (PRIORITATE 2 - apare în Browse)
     * 3. Full Photo (PRIORITATE 3 - LAZY, doar pentru "Add to My Collection")
     * 4. Barcode (PRIORITATE 4 - OPTIMIZAT, skip dacă există deja)
     * 
     * @param carId The ID of the car to sync
     */
    suspend fun syncCarIncremental(carId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d("CarSyncRepository", "=== STARTING INCREMENTAL SYNC ===")
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
            Log.d("CarSyncRepository", "  - Barcode: ${car.barcode}")
            
            // ✅ STEP 1: Thumbnail upload (PRIORITATE 1 - apare în Browse)
            if (car.thumbnailSyncStatus != PhotoSyncStatus.SYNCED) {
                try {
                    Log.d("CarSyncRepository", "STEP 1: Uploading thumbnail...")
                    carDao.updateThumbnailSyncStatus(carId, PhotoSyncStatus.UPLOADING, null)
                    
                    val thumbnailUrl = uploadPhotoToFirestore(
                        car.combinedPhotoPath ?: "", 
                        carId, 
                        "thumbnail", 
                        car.series
                    )
                    
                    if (thumbnailUrl.isNotEmpty()) {
                        carDao.updateThumbnailSyncStatus(carId, PhotoSyncStatus.SYNCED, thumbnailUrl)
                        Log.i("CarSyncRepository", "✅ STEP 1: Thumbnail uploaded successfully")
                    } else {
                        throw Exception("Thumbnail upload returned empty URL")
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌ STEP 1: Thumbnail upload failed: ${e.message}", e)
                    val attempts = car.thumbnailSyncAttempts + 1
                    carDao.incrementThumbnailSyncAttempts(carId)
                    
                    if (attempts < 3) {
                        carDao.updateThumbnailSyncStatus(carId, PhotoSyncStatus.RETRYING, null)
                        Log.w("CarSyncRepository", "⚠️ STEP 1: Will retry thumbnail upload (attempt $attempts/3)")
                    } else {
                        carDao.updateThumbnailSyncStatus(carId, PhotoSyncStatus.FAILED, null)
                        Log.e("CarSyncRepository", "❌ STEP 1: Thumbnail upload failed after 3 attempts")
                    }
                }
            } else {
                Log.d("CarSyncRepository", "STEP 1: Thumbnail already synced")
            }
            
            // ✅ STEP 2: Firestore Data save (PRIORITATE 2 - apare în Browse)
            // Read car again to get updated thumbnail URL from STEP 1
            var updatedCar = carDao.getCarById(carId) ?: car
            if (updatedCar.firestoreDataSyncStatus != DataSyncStatus.SYNCED) {
                try {
                    Log.d("CarSyncRepository", "STEP 2: Saving Firestore data...")
                    carDao.updateFirestoreDataSyncStatus(carId, DataSyncStatus.SYNCING)
                    
                    // Get thumbnail URL (from STEP 1 or existing)
                    val thumbnailUrl = updatedCar.thumbnailFirebaseUrl ?: ""
                    
                    // ✅ FIX: Set category correctly based on car type
                    // For Premium: category = series ("Premium")
                    // For Silver Series: category = series ("Silver Series")
                    // For TH: category = "Treasure Hunt"
                    // For STH: category = "Super Treasure Hunt"
                    // For others: category = series
                    val category = when {
                        updatedCar.isSTH -> "Super Treasure Hunt"
                        updatedCar.isTH -> "Treasure Hunt"
                        updatedCar.series.equals("Silver Series", ignoreCase = true) -> "Silver Series"
                        else -> updatedCar.series
                    }
                    
                    // Save to globalCars (always save, even if thumbnail is empty)
                    val result = firestoreRepository.saveAllCarsToGlobalDatabase(
                        localCarId = carId,
                        carName = updatedCar.model,
                        brand = updatedCar.brand,
                        series = updatedCar.series,
                        year = updatedCar.year,
                        color = updatedCar.color.takeIf { it.isNotEmpty() },
                        frontPhotoUrl = thumbnailUrl.ifEmpty { null },
                        backPhotoUrl = null, // Will be set in STEP 3
                        croppedBarcodeUrl = null, // Will be set in STEP 4
                        category = category,
                        subcategory = updatedCar.subseries,
                        barcode = updatedCar.barcode.takeIf { it.isNotEmpty() },
                        isTH = updatedCar.isTH,
                        isSTH = updatedCar.isSTH
                    )
                    
                    if (result.isSuccess) {
                        carDao.updateFirestoreDataSyncStatus(carId, DataSyncStatus.SYNCED)
                        Log.i("CarSyncRepository", "✅ STEP 2: Firestore data saved successfully")
                        Log.i("CarSyncRepository", "  → Car now appears in Browse (with thumbnail)")
                    } else {
                        throw Exception(result.exceptionOrNull()?.message ?: "Firestore save failed")
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌ STEP 2: Firestore data save failed: ${e.message}", e)
                    val attempts = updatedCar.firestoreDataSyncAttempts + 1
                    carDao.incrementFirestoreDataSyncAttempts(carId)
                    
                    if (attempts < 3) {
                        carDao.updateFirestoreDataSyncStatus(carId, DataSyncStatus.FAILED)
                        Log.w("CarSyncRepository", "⚠️ STEP 2: Will retry Firestore data save (attempt $attempts/3)")
                    } else {
                        carDao.updateFirestoreDataSyncStatus(carId, DataSyncStatus.FAILED)
                        Log.e("CarSyncRepository", "❌ STEP 2: Firestore data save failed after 3 attempts")
                    }
                }
            } else {
                Log.d("CarSyncRepository", "STEP 2: Firestore data already synced")
            }
            
            // ✅ STEP 3: Full Photo upload (PRIORITATE 3 - LAZY, doar pentru "Add to My Collection")
            // Read car again to get updated URLs from previous steps
            updatedCar = carDao.getCarById(carId) ?: updatedCar
            if (updatedCar.fullPhotoSyncStatus != PhotoSyncStatus.SYNCED) {
                try {
                    Log.d("CarSyncRepository", "STEP 3: Uploading full photo (LAZY)...")
                    carDao.updateFullPhotoSyncStatus(carId, PhotoSyncStatus.UPLOADING, null)
                    
                    val fullPhotoUrl = uploadPhotoToFirestore(
                        updatedCar.photoUrl ?: "", 
                        carId, 
                        "full", 
                        updatedCar.series
                    )
                    
                    if (fullPhotoUrl.isNotEmpty()) {
                        carDao.updateFullPhotoSyncStatus(carId, PhotoSyncStatus.SYNCED, fullPhotoUrl)
                        
                        // ✅ FIX: Set category correctly based on car type
                        // For Premium: category = series ("Premium")
                        // For Silver Series: category = series ("Silver Series")
                        // For TH: category = "Treasure Hunt"
                        // For STH: category = "Super Treasure Hunt"
                        // For others: category = series
                        val category = when {
                            updatedCar.isSTH -> "Super Treasure Hunt"
                            updatedCar.isTH -> "Treasure Hunt"
                            updatedCar.series.equals("Silver Series", ignoreCase = true) -> "Silver Series"
                            else -> updatedCar.series
                        }
                        
                        // Update Firestore with full photo URL
                        // Note: This is a simplified update - in production, you might want a dedicated update function
                        firestoreRepository.saveAllCarsToGlobalDatabase(
                            localCarId = carId,
                            carName = updatedCar.model,
                            brand = updatedCar.brand,
                            series = updatedCar.series,
                            year = updatedCar.year,
                            color = updatedCar.color.takeIf { it.isNotEmpty() },
                            frontPhotoUrl = updatedCar.thumbnailFirebaseUrl ?: "",
                            backPhotoUrl = fullPhotoUrl, // ✅ Full photo URL
                            croppedBarcodeUrl = updatedCar.barcodeFirebaseUrl,
                            category = category,
                            subcategory = updatedCar.subseries,
                            barcode = updatedCar.barcode.takeIf { it.isNotEmpty() },
                            isTH = updatedCar.isTH,
                            isSTH = updatedCar.isSTH
                        )
                        
                        Log.i("CarSyncRepository", "✅ STEP 3: Full photo uploaded successfully")
                        Log.i("CarSyncRepository", "  → Full photo now available for 'Add to My Collection'")
                    } else {
                        throw Exception("Full photo upload returned empty URL")
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌ STEP 3: Full photo upload failed: ${e.message}", e)
                    val attempts = updatedCar.fullPhotoSyncAttempts + 1
                    carDao.incrementFullPhotoSyncAttempts(carId)
                    
                    if (attempts < 3) {
                        carDao.updateFullPhotoSyncStatus(carId, PhotoSyncStatus.RETRYING, null)
                        Log.w("CarSyncRepository", "⚠️ STEP 3: Will retry full photo upload (attempt $attempts/3)")
                        Log.w("CarSyncRepository", "  → Thumbnail remains functional (Browse still works)")
                    } else {
                        carDao.updateFullPhotoSyncStatus(carId, PhotoSyncStatus.FAILED, null)
                        Log.e("CarSyncRepository", "❌ STEP 3: Full photo upload failed after 3 attempts")
                        Log.w("CarSyncRepository", "  → Thumbnail remains functional (Browse still works)")
                    }
                }
            } else {
                Log.d("CarSyncRepository", "STEP 3: Full photo already synced")
            }
            
            // ✅ STEP 4: Barcode upload (PRIORITATE 4 - OPTIMIZAT, skip dacă există deja)
            // Read car again to get updated URLs from previous steps
            updatedCar = carDao.getCarById(carId) ?: updatedCar
            if (updatedCar.barcode.isNotEmpty() && updatedCar.barcodeSyncStatus != PhotoSyncStatus.SYNCED) {
                try {
                    // ✅ Verifică dacă barcode-ul există deja în globalBarcodes
                    val existingBarcode = firestoreRepository.checkBarcodeInGlobalDatabase(updatedCar.barcode)
                    
                    if (existingBarcode != null) {
                        // ✅ 90% din cazuri: Skip upload, folosește URL existent
                        carDao.updateBarcodeSyncStatus(
                            carId, 
                            PhotoSyncStatus.SYNCED, 
                            existingBarcode.croppedBarcodeUrl
                        )
                        Log.d("CarSyncRepository", "✅ STEP 4: Barcode already exists - skipped upload")
                        Log.d("CarSyncRepository", "  → Using existing barcode URL from globalBarcodes")
                    } else {
                        // ✅ 10% din cazuri: New barcode - save to globalBarcodes
                        Log.d("CarSyncRepository", "STEP 4: Saving new barcode to globalBarcodes...")
                        carDao.updateBarcodeSyncStatus(carId, PhotoSyncStatus.UPLOADING, null)
                        
                        // ✅ FIX: Salvăm barcode-ul în globalBarcodes chiar dacă nu avem photo path
                        // Back photo este șters după extragerea barcode-ului, deci nu avem photo path
                        // Dar salvăm barcode-ul text și datele mașinii pentru căutare și identificare
                        val result = firestoreRepository.saveToGlobalDatabase(
                            barcode = updatedCar.barcode,
                            carName = updatedCar.model,
                            brand = updatedCar.brand,
                            series = updatedCar.series,
                            year = updatedCar.year,
                            color = updatedCar.color.takeIf { it.isNotEmpty() },
                            frontPhotoUrl = updatedCar.thumbnailFirebaseUrl ?: "",
                            backPhotoUrl = updatedCar.fullPhotoFirebaseUrl ?: "",
                            croppedBarcodeUrl = "", // No barcode photo (back photo was deleted after extraction)
                            category = updatedCar.series,
                            subcategory = updatedCar.subseries
                        )
                        
                        if (result.isSuccess) {
                            carDao.updateBarcodeSyncStatus(carId, PhotoSyncStatus.SYNCED, null)
                            Log.i("CarSyncRepository", "✅ STEP 4: Barcode saved to globalBarcodes (new barcode, no photo)")
                            Log.i("CarSyncRepository", "  → Barcode text: ${updatedCar.barcode}")
                        } else {
                            throw Exception(result.exceptionOrNull()?.message ?: "Failed to save barcode to globalBarcodes")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CarSyncRepository", "❌ STEP 4: Barcode upload failed: ${e.message}", e)
                    val attempts = updatedCar.barcodeSyncAttempts + 1
                    carDao.incrementBarcodeSyncAttempts(carId)
                    
                    if (attempts < 3) {
                        carDao.updateBarcodeSyncStatus(carId, PhotoSyncStatus.RETRYING, null)
                        Log.w("CarSyncRepository", "⚠️ STEP 4: Will retry barcode upload (attempt $attempts/3)")
                    } else {
                        carDao.updateBarcodeSyncStatus(carId, PhotoSyncStatus.FAILED, null)
                        Log.e("CarSyncRepository", "❌ STEP 4: Barcode upload failed after 3 attempts")
                    }
                }
            } else {
                if (updatedCar.barcode.isEmpty()) {
                    Log.d("CarSyncRepository", "STEP 4: No barcode to sync")
                } else {
                    Log.d("CarSyncRepository", "STEP 4: Barcode already synced")
                }
            }
            
            Log.i("CarSyncRepository", "=== INCREMENTAL SYNC COMPLETE ===")
            
        } catch (e: Exception) {
            Log.e("CarSyncRepository", "❌ Incremental sync failed: ${e.message}", e)
            // Don't throw - just log the error
        }
    }
}
