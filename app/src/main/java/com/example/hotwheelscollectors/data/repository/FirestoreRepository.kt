package com.example.hotwheelscollectors.data.repository

import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.UserDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

data class GlobalCarData(
    val barcode: String,
    val carName: String,
    val brand: String,
    val series: String,
    val year: Int,
    val color: String,
    val frontPhotoUrl: String,
    val backPhotoUrl: String,
    val croppedBarcodeUrl: String,
    val contributorUserId: String,
    val verificationCount: Int,
    val category: String,
    val subcategory: String,
    val createdAt: Date
)

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val userDao: UserDao
) {

    val userId: String
        get() {
            val user = getCurrentUser()
            if (user == null) {
                android.util.Log.e("FirestoreRepository", "User not authenticated")
                throw IllegalStateException("User must be authenticated to access Firestore")
            }
            return user.uid
        }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun addCar(car: HotWheelsCar): Result<Unit> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val carEntity = CarEntity(
                id = car.id,
                userId = currentUser.uid,
                model = car.model,
                brand = car.brand,
                year = car.year,
                photoUrl = car.photoUrl ?: "",
                folderPath = car.folderPath ?: "",
                isPremium = car.isPremium,
                timestamp = car.timestamp ?: System.currentTimeMillis(),
                barcode = car.barcode ?: "",
                frontPhotoPath = car.frontPhotoPath ?: "",
                backPhotoPath = car.backPhotoPath ?: "",
                combinedPhotoPath = car.combinedPhotoPath ?: "",
                searchKeywords = car.searchKeywords ?: emptyList()
            )

            carDao.insertCar(carEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCarEntity(car: CarEntity): Result<Unit> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            carDao.insertCar(car)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCarsForUser(userId: String): Flow<List<CarEntity>> {
        return carDao.getCarsForUser(userId)
    }

    suspend fun getCarById(carId: String): CarEntity? {
        return carDao.getCarById(carId)
    }

    suspend fun updateCar(car: CarEntity): Result<Unit> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            carDao.updateCar(car)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCarsByBrand(brand: String): List<HotWheelsCar> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return emptyList()
            }

            val carEntities = carDao.getCarsByBrand(currentUser.uid, brand).first()
            carEntities.map { it.toHotWheelsCar() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCarsBySeries(seriesId: String): List<HotWheelsCar> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return emptyList()
            }

            val carEntities = carDao.getCarsBySeries(currentUser.uid, seriesId).first()
            carEntities.map { it.toHotWheelsCar() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCarsByCategory(categoryId: String): List<HotWheelsCar> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return emptyList()
            }

            // Get all cars for the user and filter by category
            val allCars = carDao.getCarsForUser(currentUser.uid).first()
            val categoryCars = allCars.filter { car ->
                when (categoryId) {
                    "supercars" -> car.brand.lowercase() in listOf("ferrari", "lamborghini", "maserati", "pagani", "bugatti", "mclaren", "koenigsegg", "aston_martin", "rimac", "lucid_air", "ford_gt", "mazda_787b", "automobili_pininfarina", "bentley", "toyota", "corvette")
                    "rally" -> car.brand.lowercase() in listOf("subaru", "mitsubishi", "lancia", "peugeot", "citroen", "toyota", "ford", "audi", "volkswagen", "mazda", "bmw", "volvo", "datsun")
                    "american_muscle" -> car.brand.lowercase() in listOf("ford", "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac", "oldsmobile", "plymouth", "lincoln", "mercury", "camaro", "chevy", "corvette")
                    "suv_trucks" -> car.brand.lowercase() in listOf("hummer", "jeep", "ram", "gmc", "land_rover", "toyota", "honda", "nissan", "ford", "chevrolet", "dodge", "bmw", "mercedes", "mercedes_benz", "audi", "volkswagen", "porsche")
                    "vans" -> car.brand.lowercase() in listOf("ford", "chevrolet", "dodge", "chrysler", "toyota", "honda", "nissan", "volkswagen", "mercedes", "mercedes_benz")
                    "motorcycle" -> car.brand.lowercase() in listOf("honda", "yamaha", "kawasaki", "suzuki", "bmw", "ducati", "harley_davidson", "indian", "triumph")
                    "convertible" -> car.brand.lowercase() in listOf("ford", "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac", "oldsmobile", "plymouth", "lincoln", "mercury", "toyota", "honda", "nissan", "mazda", "subaru", "mitsubishi", "suzuki", "daihatsu", "lexus", "infiniti", "acura", "datsun", "bmw", "mercedes", "mercedes_benz", "audi", "volkswagen", "porsche", "opel", "ferrari", "lamborghini", "maserati", "pagani", "bugatti", "fiat", "alfa_romeo", "lancia", "abarth", "peugeot", "renault", "citroen", "jaguar", "land_rover", "mini", "bentley", "aston_martin", "lotus", "mclaren", "volvo", "koenigsegg", "corvette")
                    "hot_roads" -> true // All brands - no specific filtering
                    else -> false
                }
            }
            categoryCars.map { it.toHotWheelsCar() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteCar(carId: String): Result<Unit> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            carDao.deleteCarById(carId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchCars(userId: String, query: String): Flow<List<CarEntity>> {
        return carDao.searchCars(userId, query)
    }

    // Stubs used by sync/offline layers; replace with real cloud calls when available
    suspend fun getCarsSinceTimestamp(_since: Date): List<CarEntity> {
        // Default to returning local data; adjust to fetch from server in the future
        return getCarsForUser(userId).first()
    }

    suspend fun uploadPhoto(localPath: String): String {
        return uploadPhotoToGlobal(localPath, "", PhotoType.OTHER)
    }

    /**
     * Production-ready Firebase Storage photo upload function
     * Uploads photo to Firebase Storage and returns the download URL
     */
    suspend fun uploadPhotoToGlobal(localPath: String, barcode: String, photoType: PhotoType): String {
        return try {
            android.util.Log.d("FirestoreRepository", "Starting Firebase Storage upload: $localPath")
            android.util.Log.d("FirestoreRepository", "Barcode: '$barcode', PhotoType: $photoType")
            
            // Check authentication
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("FirestoreRepository", "❌ No authenticated user for Firebase Storage upload")
                return ""
            }
            android.util.Log.d("FirestoreRepository", "✅ User authenticated: ${currentUser.uid}")
            
            // Check if Firebase Storage is available
            try {
                val testRef = storage.reference.child("test")
                android.util.Log.d("FirestoreRepository", "✅ Firebase Storage reference created successfully")
            } catch (e: Exception) {
                android.util.Log.e("FirestoreRepository", "❌ Firebase Storage not available: ${e.message}", e)
                return ""
            }
            
            // Create unique filename with barcode and timestamp
            val timestamp = System.currentTimeMillis()
            val fileExtension = localPath.substringAfterLast(".", "jpg")
            val fileName = if (barcode.isNotEmpty()) {
                "global/cars/${barcode}_${photoType.name.lowercase()}_${timestamp}.$fileExtension"
            } else {
                "global/cars/photo_${photoType.name.lowercase()}_${timestamp}.$fileExtension"
            }
            
            android.util.Log.d("FirestoreRepository", "Uploading to Firebase Storage path: $fileName")
            
            // Create file reference
            val file = File(localPath)
            if (!file.exists()) {
                android.util.Log.e("FirestoreRepository", "❌ Local file does not exist: $localPath")
                return ""
            }
            
            val fileSize = file.length()
            android.util.Log.d("FirestoreRepository", "✅ File exists, size: $fileSize bytes")
            
            val photoRef = storage.reference.child(fileName)
            android.util.Log.d("FirestoreRepository", "✅ Storage reference created: $fileName")
            
            val uploadTask = photoRef.putFile(Uri.fromFile(file))
            android.util.Log.d("FirestoreRepository", "✅ Upload task started")
            
            // Wait for upload to complete and get download URL
            val result = uploadTask.await()
            android.util.Log.d("FirestoreRepository", "✅ Upload task completed")
            
            val downloadUrl = photoRef.downloadUrl.await()
            android.util.Log.i("FirestoreRepository", "✅ Photo uploaded successfully to Firebase Storage: $downloadUrl")
            downloadUrl.toString()
            
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "❌ Firebase Storage upload failed: ${e.message}", e)
            android.util.Log.e("FirestoreRepository", "❌ Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("FirestoreRepository", "❌ Stack trace: ${e.stackTrace.joinToString("\n")}")
            ""
        }
    }

    suspend fun deletePhoto(_path: String) {
        // Implement Firebase Storage delete here
    }

    /**
     * Debug function to check authentication status
     */
    fun checkAuthenticationStatus(): String {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                "❌ No authenticated user"
            } else {
                val tokenTask = currentUser.getIdToken(false)
                if (tokenTask.isSuccessful) {
                    val token = tokenTask.result?.token
                    "✅ User authenticated: ${currentUser.uid}, Token valid: ${token != null}"
                } else {
                    "❌ User authenticated but token invalid: ${currentUser.uid}"
                }
            }
        } catch (e: Exception) {
            "❌ Authentication check failed: ${e.message}"
        }
    }

    suspend fun getMainlineCars(): List<CarEntity> {
        return getCarsForUser(userId).first().filter { !it.isPremium }
    }

    suspend fun getPremiumCars(): List<CarEntity> {
        return getCarsForUser(userId).first().filter { it.isPremium }
    }

    /**
     * Get all cars from global database (both barcoded and non-barcoded)
     */
    suspend fun getGlobalCars(): List<GlobalCarData> {
        return try {
            // Get cars from both collections
            val barcodedCars = firestore.collection("globalBarcodes").get().await()
            val allCars = firestore.collection("globalCars").get().await()
            
            val barcodedData = barcodedCars.documents.mapNotNull { document ->
                try {
                    GlobalCarData(
                        barcode = document.getString("barcode") ?: "",
                        carName = document.getString("carName") ?: "",
                        brand = document.getString("brand") ?: "",
                        series = document.getString("series") ?: "",
                        year = document.getLong("year")?.toInt() ?: 0,
                        color = document.getString("color") ?: "",
                        frontPhotoUrl = document.getString("frontPhotoUrl") ?: "",
                        backPhotoUrl = document.getString("backPhotoUrl") ?: "",
                        croppedBarcodeUrl = document.getString("croppedBarcodeUrl") ?: "",
                        contributorUserId = document.getString("contributorUserId") ?: "",
                        verificationCount = document.getLong("verificationCount")?.toInt() ?: 1,
                        category = document.getString("category") ?: "",
                        subcategory = document.getString("subcategory") ?: "",
                        createdAt = document.getTimestamp("createdAt")?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val allCarsData = allCars.documents.mapNotNull { document ->
                try {
                    GlobalCarData(
                        barcode = document.getString("barcode") ?: "",
                        carName = document.getString("carName") ?: "",
                        brand = document.getString("brand") ?: "",
                        series = document.getString("series") ?: "",
                        year = document.getLong("year")?.toInt() ?: 0,
                        color = document.getString("color") ?: "",
                        frontPhotoUrl = document.getString("frontPhotoUrl") ?: "",
                        backPhotoUrl = document.getString("backPhotoUrl") ?: "",
                        croppedBarcodeUrl = document.getString("croppedBarcodeUrl") ?: "",
                        contributorUserId = document.getString("contributorUserId") ?: "",
                        verificationCount = document.getLong("verificationCount")?.toInt() ?: 1,
                        category = document.getString("category") ?: "",
                        subcategory = document.getString("subcategory") ?: "",
                        createdAt = document.getTimestamp("createdAt")?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val barcodesCovered = allCarsData.mapNotNull { it.barcode.takeIf { barcode -> barcode.isNotEmpty() } }.toMutableSet()
            val uniqueBarcodedData = barcodedData.filter { it.barcode.isNotEmpty() && !barcodesCovered.contains(it.barcode) }

            val combined = (allCarsData + uniqueBarcodedData).sortedByDescending { it.createdAt }
            combined
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get mainline cars from global database
     */
    suspend fun getGlobalMainlineCars(): List<GlobalCarData> {
        return getGlobalCars().filter { 
            it.category.lowercase() == "mainline" || 
            (!it.category.lowercase().contains("premium") && 
             !it.category.lowercase().contains("treasure") &&
             !it.category.lowercase().contains("other"))
        }
    }

    /**
     * Get premium cars from global database
     */
    suspend fun getGlobalPremiumCars(): List<GlobalCarData> {
        return getGlobalCars().filter { 
            it.category.lowercase().contains("premium")
        }
    }

    /**
     * Get treasure hunt cars from global database
     */
    suspend fun getGlobalTreasureHuntCars(): List<GlobalCarData> {
        return getGlobalCars().filter { 
            it.category.lowercase().contains("treasure") && 
            !it.category.lowercase().contains("super")
        }
    }

    /**
     * Get super treasure hunt cars from global database
     */
    suspend fun getGlobalSuperTreasureHuntCars(): List<GlobalCarData> {
        return getGlobalCars().filter { 
            it.category.lowercase().contains("super") && 
            it.category.lowercase().contains("treasure")
        }
    }

    /**
     * Get other cars from global database
     */
    suspend fun getGlobalOtherCars(): List<GlobalCarData> {
        return getGlobalCars().filter { 
            it.category.lowercase().contains("other") ||
            it.series.lowercase() == "others"
        }
    }


    /**
     * Upload photo to user's personal collection
     * Path: UserCollections/{userId}/{folder}/{carId}/photo_type.jpg
     */
    suspend fun uploadPhotoToUserCollection(
        localPath: String,
        userId: String,
        carId: String,
        folder: String?,
        photoType: PhotoType,
    ): String {
        return try {
            val folderPath = folder ?: "general"
            val fileName = "${photoType.name.lowercase()}.jpg"
            val userPath = "UserCollections/$userId/$folderPath/$carId/$fileName"

            // Upload to Firebase Storage (real implementation)
            val storageRef = storage.reference.child(userPath)
            val file = java.io.File(localPath)
            val uploadTask = storageRef.putFile(android.net.Uri.fromFile(file))

            // Wait for upload to complete and get download URL
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }.await().toString()

        } catch (e: Exception) {
            // Fallback: return local path if cloud upload fails
            localPath
        }
    }

    /**
     * Check if barcode exists in global database
     */
    suspend fun checkBarcodeInGlobalDatabase(barcode: String): GlobalBarcodeResult? {
        return try {
            val docRef = firestore.collection("globalBarcodes").document(barcode)
            val document = docRef.get().await()

            if (document.exists()) {
                val data = document.data
                GlobalBarcodeResult(
                    barcode = barcode,
                    carName = data?.get("carName") as? String ?: "",
                    brand = data?.get("brand") as? String ?: "",
                    series = data?.get("series") as? String ?: "",
                    year = (data?.get("year") as? Long)?.toInt() ?: 0,
                    color = data?.get("color") as? String,
                    frontPhotoUrl = data?.get("frontPhotoUrl") as? String,
                    backPhotoUrl = data?.get("backPhotoUrl") as? String,
                    croppedBarcodeUrl = data?.get("croppedBarcodeUrl") as? String,
                    contributorUserId = data?.get("contributorUserId") as? String ?: "",
                    verificationCount = (data?.get("verificationCount") as? Long)?.toInt() ?: 1,
                    lastVerified = (data?.get("lastVerified") as? com.google.firebase.Timestamp)?.toDate()
                        ?: Date(),
                    category = data?.get("category") as? String ?: "",
                    subcategory = data?.get("subcategory") as? String
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save car data to global barcode database (for barcoded cars)
     */
    suspend fun saveToGlobalDatabase(
        barcode: String,
        carName: String,
        brand: String,
        series: String,
        year: Int,
        color: String?,
        frontPhotoUrl: String?,
        backPhotoUrl: String?,
        croppedBarcodeUrl: String?,
        category: String,
        subcategory: String?,
    ): Result<Unit> {
        return try {
            val globalData = mapOf(
                "barcode" to barcode,
                "carName" to carName,
                "brand" to brand,
                "series" to series,
                "year" to year,
                "color" to (color ?: ""),
                "frontPhotoUrl" to (frontPhotoUrl ?: ""),
                "backPhotoUrl" to (backPhotoUrl ?: ""),
                "croppedBarcodeUrl" to (croppedBarcodeUrl ?: ""),
                "contributorUserId" to userId,
                "verificationCount" to 1,
                "lastVerified" to com.google.firebase.Timestamp.now(),
                "category" to category,
                "subcategory" to (subcategory ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("globalBarcodes")
                .document(barcode)
                .set(globalData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save ALL cars to global database (both barcoded and non-barcoded)
     */
    suspend fun saveAllCarsToGlobalDatabase(
        localCarId: String,
        carName: String,
        brand: String,
        series: String,
        year: Int,
        color: String?,
        frontPhotoUrl: String?,
        backPhotoUrl: String?,
        croppedBarcodeUrl: String?,
        category: String, // e.g. "Premium"
        subcategory: String?,
        barcode: String? = null,
        isTH: Boolean = false,
        isSTH: Boolean = false,
    ): Result<Unit> {
        return try {
            val documentId = localCarId

            val globalData = mapOf(
                "carId" to localCarId,
                "barcode" to (barcode ?: ""),
                "carName" to carName,
                "brand" to brand,
                "series" to series,
                "year" to year,
                "color" to (color ?: ""),
                "frontPhotoUrl" to (frontPhotoUrl ?: ""),
                "backPhotoUrl" to (backPhotoUrl ?: ""),
                "croppedBarcodeUrl" to (croppedBarcodeUrl ?: ""),
                "contributorUserId" to userId,
                "verificationCount" to 1,
                "lastVerified" to com.google.firebase.Timestamp.now(),
                "category" to category,
                "subcategory" to (subcategory ?: ""),
                "isTH" to isTH,
                "isSTH" to isSTH,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("globalCars")
                .document(documentId)
                .set(globalData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class for global barcode lookup results
 */
data class GlobalBarcodeResult(
    val barcode: String,
    val carName: String,
    val brand: String,
    val series: String,
    val year: Int,
    val color: String?,
    val frontPhotoUrl: String?,
    val backPhotoUrl: String?,
    val croppedBarcodeUrl: String? = null,
    val contributorUserId: String,
    val verificationCount: Int,
    val lastVerified: Date,
    val category: String,
    val subcategory: String?,
)

// Extension function to convert CarEntity to HotWheelsCar
private fun CarEntity.toHotWheelsCar(): HotWheelsCar {
    return HotWheelsCar(
        id = id,
        name = model, // Use model as name since HotWheelsCar has name field
        model = model,
        brand = brand,
        series = series,
        subseries = subseries,
        year = year,
        number = number,
        color = color,
        tampos = "", // Not available in CarEntity
        barcode = barcode,
        baseType = "", // Not available in CarEntity
        wheelType = "", // Not available in CarEntity
        photoUrl = photoUrl,
        folderPath = folderPath,
        isPremium = isPremium,
        isSTH = isSTH,
        isTH = isTH,
        isFirstEdition = isFirstEdition,
        timestamp = timestamp,
        frontPhotoPath = frontPhotoPath,
        backPhotoPath = backPhotoPath,
        combinedPhotoPath = combinedPhotoPath,
        notes = notes,
        purchaseDate = "", // Not available in CarEntity
        purchasePrice = purchasePrice,
        purchaseLocation = location,
        condition = condition,
        packageCondition = "", // Not available in CarEntity
        estimatedValue = currentValue,
        lastPriceCheck = "", // Not available in CarEntity
        searchKeywords = searchKeywords
    )
}