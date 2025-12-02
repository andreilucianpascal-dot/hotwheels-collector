package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.data.local.entities.CarWithSearchKeywords
import com.example.hotwheelscollectors.data.local.entities.PhotoSyncStatus
import com.example.hotwheelscollectors.data.local.entities.DataSyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    // Basic CRUD operations
    @Query("SELECT * FROM cars WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCarsForUser(userId: String): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :carId")
    suspend fun getCarById(carId: String): CarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCars(cars: List<CarEntity>)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteCarById(carId: String)

    @Query("DELETE FROM cars WHERE userId = :userId")
    suspend fun deleteAllCarsForUser(userId: String)

    @Query("DELETE FROM cars")
    suspend fun deleteAll()

    @Query("DELETE FROM cars")
    suspend fun deleteAllCache()

    // Search and filter operations
    @Query("""
        SELECT * FROM cars 
        WHERE userId = :userId 
        AND (model LIKE '%' || :query || '%' 
             OR brand LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchCars(userId: String, query: String): Flow<List<CarEntity>>

    @Query("""
        SELECT * FROM cars 
        WHERE (model LIKE '%' || :query || '%' 
             OR brand LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """
    )
    fun searchCars(query: String): Flow<List<CarEntity>>

    @Query(
        """
        SELECT * FROM cars 
        WHERE userId = :userId 
        AND isPremium = :isPremium
        ORDER BY timestamp DESC
    """)
    fun getCarsByType(userId: String, isPremium: Boolean): Flow<List<CarEntity>>

    @Query("""
        SELECT * FROM cars 
        WHERE userId = :userId 
        AND year = :year
        ORDER BY timestamp DESC
    """)
    fun getCarsByYear(userId: String, year: Int): Flow<List<CarEntity>>

    @Query("""
        SELECT * FROM cars 
        WHERE userId = :userId 
        AND brand = :brand
        ORDER BY timestamp DESC
    """)
    fun getCarsByBrand(userId: String, brand: String): Flow<List<CarEntity>>

    @Query("""
        SELECT * FROM cars 
        WHERE userId = :userId 
        AND series = :series
        ORDER BY timestamp DESC
    """)
    fun getCarsBySeries(userId: String, series: String): Flow<List<CarEntity>>

    // New function to get unique brands for a series
    @Query("SELECT DISTINCT brand FROM cars WHERE series = :series AND userId = :userId ORDER BY brand ASC")
    fun getUniqueBrandsForSeries(userId: String, series: String): Flow<List<String>>

    // Advanced queries with relationships
    @Transaction
    @Query("SELECT * FROM cars WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCarsWithPhotos(userId: String): Flow<List<CarWithPhotos>>

    @Transaction
    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getCarWithPhotosById(carId: String): Flow<CarWithPhotos?>

    // Statistics
    @Query("""
        SELECT 
            COUNT(*) as totalCars,
            COUNT(CASE WHEN isPremium = 1 THEN 1 END) as premiumCars,
            COUNT(CASE WHEN isSTH = 1 THEN 1 END) as sthCars,
            COUNT(CASE WHEN isTH = 1 THEN 1 END) as thCars,
            AVG(currentValue) as avgValue,
            SUM(currentValue) as totalValue
        FROM cars 
        WHERE userId = :userId
    """)
    fun getCollectionStats(userId: String): Flow<CollectionStats>

    @Query("SELECT * FROM cars ORDER BY timestamp DESC")
    fun getAllCars(): Flow<List<CarEntity>>

    // Barcode operations
    @Query("SELECT * FROM cars WHERE barcode = :barcode AND userId = :userId")
    suspend fun getCarByBarcode(barcode: String, userId: String): CarEntity?

    @Query("SELECT COUNT(*) FROM cars WHERE barcode = :barcode AND userId = :userId")
    suspend fun getBarcodeCount(barcode: String, userId: String): Int

    // Favorite operations
    @Query("SELECT * FROM cars WHERE userId = :userId AND isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteCars(userId: String): Flow<List<CarEntity>>

    @Query("UPDATE cars SET isFavorite = :isFavorite WHERE id = :carId")
    suspend fun updateFavoriteStatus(carId: String, isFavorite: Boolean)

    // Sync operations
    @Query("SELECT * FROM cars WHERE userId = :userId AND syncStatus = :syncStatus")
    fun getCarsBySyncStatus(userId: String, syncStatus: String): Flow<List<CarEntity>>

    @Query("UPDATE cars SET syncStatus = :syncStatus WHERE id = :carId")
    suspend fun updateSyncStatus(carId: String, syncStatus: String)

    @Query("SELECT * FROM cars WHERE userId = :userId AND syncStatus != 'SYNCED'")
    fun getUnsyncedCars(userId: String): Flow<List<CarEntity>>

    // Count operations
    @Query("SELECT COUNT(*) FROM cars")
    suspend fun getCarCount(): Int

    // Incremental Sync Status Operations
    // Thumbnail sync status
    @Query("UPDATE cars SET thumbnailSyncStatus = :status, thumbnailFirebaseUrl = :url WHERE id = :carId")
    suspend fun updateThumbnailSyncStatus(carId: String, status: PhotoSyncStatus, url: String?)

    @Query("UPDATE cars SET thumbnailSyncAttempts = thumbnailSyncAttempts + 1 WHERE id = :carId")
    suspend fun incrementThumbnailSyncAttempts(carId: String)

  @Query("SELECT * FROM cars WHERE thumbnailSyncStatus IN ('PENDING', 'RETRYING', 'FAILED') AND thumbnailSyncAttempts < 3 ORDER BY syncPriority DESC, createdAt ASC")
    suspend fun getCarsWithFailedThumbnailSync(): List<CarEntity>

    // Full photo sync status
    @Query("UPDATE cars SET fullPhotoSyncStatus = :status, fullPhotoFirebaseUrl = :url WHERE id = :carId")
    suspend fun updateFullPhotoSyncStatus(carId: String, status: PhotoSyncStatus, url: String?)

    @Query("UPDATE cars SET fullPhotoSyncAttempts = fullPhotoSyncAttempts + 1 WHERE id = :carId")
    suspend fun incrementFullPhotoSyncAttempts(carId: String)

    @Query("SELECT * FROM cars WHERE fullPhotoSyncStatus IN ('PENDING', 'RETRYING', 'FAILED') AND fullPhotoSyncAttempts < 3 ORDER BY syncPriority DESC, createdAt ASC")
    suspend fun getCarsWithFailedFullPhotoSync(): List<CarEntity>

    // Barcode sync status
    @Query("UPDATE cars SET barcodeSyncStatus = :status, barcodeFirebaseUrl = :url WHERE id = :carId")
    suspend fun updateBarcodeSyncStatus(carId: String, status: PhotoSyncStatus, url: String?)

    @Query("UPDATE cars SET barcodeSyncAttempts = barcodeSyncAttempts + 1 WHERE id = :carId")
    suspend fun incrementBarcodeSyncAttempts(carId: String)

    @Query("SELECT * FROM cars WHERE barcodeSyncStatus IN ('PENDING', 'RETRYING', 'FAILED') AND barcodeSyncAttempts < 3 AND barcode != '' ORDER BY syncPriority DESC, createdAt ASC")
    suspend fun getCarsWithFailedBarcodeSync(): List<CarEntity>

    // Firestore data sync status
    @Query("UPDATE cars SET firestoreDataSyncStatus = :status WHERE id = :carId")
    suspend fun updateFirestoreDataSyncStatus(carId: String, status: DataSyncStatus)

    @Query("UPDATE cars SET firestoreDataSyncAttempts = firestoreDataSyncAttempts + 1 WHERE id = :carId")
    suspend fun incrementFirestoreDataSyncAttempts(carId: String)

    @Query("SELECT * FROM cars WHERE firestoreDataSyncStatus IN ('PENDING', 'SYNCING', 'FAILED') AND firestoreDataSyncAttempts < 3 ORDER BY syncPriority DESC, createdAt ASC")
    suspend fun getCarsWithFailedFirestoreDataSync(): List<CarEntity>

    // Overall sync status
    @Query("UPDATE cars SET syncAttempts = :attempts, lastSyncError = :error, lastSyncAttempt = :timestamp WHERE id = :carId")
    suspend fun updateSyncAttempts(carId: String, attempts: Int, error: String?, timestamp: Long)
}

data class CollectionStats(
    val totalCars: Int,
    val premiumCars: Int,
    val sthCars: Int,
    val thCars: Int,
    val avgValue: Double,
    val totalValue: Double
)