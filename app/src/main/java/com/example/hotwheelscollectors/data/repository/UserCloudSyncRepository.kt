package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.dao.*
import com.example.hotwheelscollectors.data.local.entities.*
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserCloudSyncRepository handles exporting Room Database to JSON
 * and uploading it to the user's chosen cloud storage (Google Drive/OneDrive/Dropbox).
 * 
 * RESPONSIBILITIES:
 * 1. Export Room Database → JSON (all tables)
 * 2. Upload JSON to user's cloud storage
 * 3. Upload local photos to user's cloud storage (incremental)
 * 4. Track last sync timestamp for incremental syncs
 */
@Singleton
class UserCloudSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val userPreferences: UserPreferences,
    private val gson: Gson,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository
) {
    
    companion object {
        private const val TAG = "UserCloudSyncRepository"
        private const val BACKUP_FOLDER = "HotWheelsCollectors/database"
        private const val PHOTOS_FOLDER = "HotWheelsCollectors/photos"
        private const val BACKUP_FILENAME_PREFIX = "backup_"
        private const val BACKUP_FILENAME_LATEST = "backup_latest.json"
    }
    
    /**
     * Exports the entire Room Database to JSON format.
     * Includes all tables: cars, photos, users, price_history, etc.
     * 
     * @return Result containing the path to the exported JSON file, or error if failed
     */
    suspend fun exportDatabaseToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING DATABASE EXPORT ===")
            
            // Create backup directory
            val backupDir = File(context.cacheDir, "database_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Generate timestamped filename
            val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())
            val backupFile = File(backupDir, "${BACKUP_FILENAME_PREFIX}${timestamp}.json")
            
            Log.d(TAG, "Exporting database to: ${backupFile.absolutePath}")
            
            // Export all tables
            val cars = exportCars()
            val photos = exportPhotos()
            Log.d(TAG, "Exporting database: ${cars.size} cars, ${photos.size} photos")
            
            val exportData = DatabaseExport(
                version = 1,
                timestamp = System.currentTimeMillis(),
                userId = getCurrentUserId(),
                tables = DatabaseTables(
                    users = exportUsers(),
                    cars = cars,
                    photos = photos,
                    priceHistory = exportPriceHistory(),
                    tradeOffers = exportTradeOffers(),
                    wishlist = exportWishlist(),
                    searchHistory = exportSearchHistory(),
                    searchKeywords = exportSearchKeywords()
                )
            )
            
            Log.d(TAG, "Export data created: ${exportData.tables.cars.size} cars, ${exportData.tables.photos.size} photos")
            
            // Write to JSON file
            FileWriter(backupFile).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            Log.i(TAG, "✅ Database exported successfully: ${backupFile.absolutePath}")
            Log.i(TAG, "   Size: ${backupFile.length()} bytes")
            
            Result.success(backupFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database export failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Exports only changes since last sync (incremental export).
     * 
     * @param lastSyncTimestamp Timestamp of last successful sync
     * @return Result containing the path to the exported JSON file, or error if failed
     */
    suspend fun exportIncrementalDatabaseToJson(lastSyncTimestamp: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING INCREMENTAL DATABASE EXPORT ===")
            Log.d(TAG, "Last sync timestamp: $lastSyncTimestamp")
            
            // Create backup directory
            val backupDir = File(context.cacheDir, "database_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Generate timestamped filename
            val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())
            val backupFile = File(backupDir, "${BACKUP_FILENAME_PREFIX}incremental_${timestamp}.json")
            
            Log.d(TAG, "Exporting incremental changes to: ${backupFile.absolutePath}")
            
            // Export only modified records (based on lastModified or updatedAt)
            val exportData = DatabaseExport(
                version = 1,
                timestamp = System.currentTimeMillis(),
                userId = getCurrentUserId(),
                isIncremental = true,
                lastSyncTimestamp = lastSyncTimestamp,
                tables = DatabaseTables(
                    users = exportUsersIncremental(lastSyncTimestamp),
                    cars = exportCarsIncremental(lastSyncTimestamp),
                    photos = exportPhotosIncremental(lastSyncTimestamp),
                    priceHistory = exportPriceHistoryIncremental(lastSyncTimestamp),
                    tradeOffers = exportTradeOffersIncremental(lastSyncTimestamp),
                    wishlist = exportWishlistIncremental(lastSyncTimestamp),
                    searchHistory = exportSearchHistoryIncremental(lastSyncTimestamp),
                    searchKeywords = exportSearchKeywordsIncremental(lastSyncTimestamp)
                )
            )
            
            // Write to JSON file
            FileWriter(backupFile).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            Log.i(TAG, "✅ Incremental database exported successfully: ${backupFile.absolutePath}")
            Log.i(TAG, "   Size: ${backupFile.length()} bytes")
            
            Result.success(backupFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Incremental database export failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Uploads the exported JSON file to user's cloud storage.
     * 
     * @param jsonFile The JSON file to upload
     * @return Result containing the cloud path if successful, or error if failed
     */
    suspend fun uploadDatabaseJsonToCloud(jsonFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING DATABASE JSON UPLOAD ===")
            Log.d(TAG, "File: ${jsonFile.absolutePath}")
            Log.d(TAG, "Size: ${jsonFile.length()} bytes")
            
            val storageLocation = userPreferences.storageLocation.first()
            Log.d(TAG, "Storage location: $storageLocation")
            
            val cloudPath = when (storageLocation) {
                "Google Drive", "GoogleDrive" -> {
                    Log.d(TAG, "Uploading to Google Drive...")
                    uploadToGoogleDrive(jsonFile)
                }
                "OneDrive" -> {
                    Log.d(TAG, "Uploading to OneDrive...")
                    uploadToOneDrive(jsonFile)
                }
                "Dropbox" -> {
                    Log.d(TAG, "Uploading to Dropbox...")
                    uploadToDropbox(jsonFile)
                }
                else -> {
                    Log.w(TAG, "Storage location is Local - skipping cloud upload")
                    Result.success("local://${jsonFile.absolutePath}")
                }
            }
            
            if (cloudPath.isSuccess) {
                Log.i(TAG, "✅ Database JSON uploaded successfully")
                Log.i(TAG, "   Cloud path: ${cloudPath.getOrNull()}")
            } else {
                Log.e(TAG, "❌ Database JSON upload failed: ${cloudPath.exceptionOrNull()?.message}")
            }
            
            cloudPath
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database JSON upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Uploads local photos to user's cloud storage (incremental).
     * Only uploads photos that haven't been synced yet.
     * 
     * @param lastSyncTimestamp Timestamp of last successful sync
     * @return Result containing the number of photos uploaded, or error if failed
     */
    suspend fun uploadPhotosToCloud(lastSyncTimestamp: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING PHOTOS UPLOAD ===")
            Log.d(TAG, "Last sync timestamp: $lastSyncTimestamp")
            
            // Get all photos that need to be synced
            // Photos need to be synced if:
            // 1. They were never synced (lastSyncedAt == null)
            // 2. They were created/modified after the last sync timestamp
            val photos = appDatabase.photoDao().getAllPhotos().first()
            val photosToSync = photos.filter { 
                val lastSyncedTime = it.lastSyncedAt?.time ?: 0L
                lastSyncedTime == 0L || it.createdAt.time > lastSyncTimestamp
            }
            
            Log.d(TAG, "Photos to sync: ${photosToSync.size}")
            
            if (photosToSync.isEmpty()) {
                Log.d(TAG, "No photos to sync")
                return@withContext Result.success(0)
            }
            
            val storageLocation = userPreferences.storageLocation.first()
            var uploadedCount = 0
            
            for (photo in photosToSync) {
                val photoFile = File(photo.localPath)
                if (!photoFile.exists()) {
                    Log.w(TAG, "Photo file not found: ${photo.localPath}")
                    continue
                }
                
                val uploadResult = when (storageLocation) {
                    "Google Drive", "GoogleDrive" -> uploadPhotoToGoogleDrive(photoFile, photo)
                    "OneDrive" -> uploadPhotoToOneDrive(photoFile, photo)
                    "Dropbox" -> uploadPhotoToDropbox(photoFile, photo)
                    else -> {
                        Log.w(TAG, "Storage location is Local - skipping photo upload")
                        Result.success("local://${photo.localPath}")
                    }
                }
                
                if (uploadResult.isSuccess) {
                    uploadedCount++
                    // Update photo sync status
                    appDatabase.photoDao().updatePhoto(
                        photo.copy(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = Date()
                        )
                    )
                } else {
                    Log.e(TAG, "Failed to upload photo: ${photo.localPath}")
                }
            }
            
            Log.i(TAG, "✅ Photos upload completed: $uploadedCount/${photosToSync.size}")
            Result.success(uploadedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Photos upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private suspend fun getCurrentUserId(): String {
        return userPreferences.userName.first().takeIf { it.isNotEmpty() }
            ?: "unknown_user"
    }
    
    // Export methods for full export
    private suspend fun exportUsers(): List<UserEntity> {
        val userId = getCurrentUserId()
        val user = appDatabase.userDao().getById(userId)
        return if (user != null) listOf(user) else emptyList()
    }
    
    private suspend fun exportCars(): List<CarEntity> {
        // ✅ FIX: Export ALL cars regardless of userId for backup/restore purposes
        // This ensures backup includes all cars in the database
        val allCars = appDatabase.carDao().getAllCars().first()
        Log.d(TAG, "Exported ${allCars.size} cars for backup")
        return allCars
    }
    
    private suspend fun exportPhotos(): List<PhotoEntity> {
        return appDatabase.photoDao().getAllPhotos().first()
    }
    
    private suspend fun exportPriceHistory(): List<PriceHistoryEntity> {
        // PriceHistoryDao doesn't have getAllPriceHistory(), use getUnsyncedRecords with empty status
        // This will get all records (since we pass SYNCED and get all that are NOT SYNCED, which is all)
        // Actually, better approach: get all price history for all cars
        val cars = exportCars()
        val allPriceHistory = mutableListOf<PriceHistoryEntity>()
        cars.forEach { car ->
            val priceHistory = appDatabase.priceHistoryDao().getPriceHistoryForCar(car.id).first()
            allPriceHistory.addAll(priceHistory)
        }
        return allPriceHistory
    }
    
    private suspend fun exportTradeOffers(): List<TradeOfferEntity> {
        val userId = getCurrentUserId()
        return appDatabase.tradeDao().getAllTradeOffers(userId).first()
    }
    
    private suspend fun exportWishlist(): List<WishlistEntity> {
        val userId = getCurrentUserId()
        return appDatabase.wishlistDao().getAllWishlistItems(userId).first()
    }
    
    private suspend fun exportSearchHistory(): List<SearchHistoryEntity> {
        // SearchHistoryDao doesn't have getAllSearchHistory(), use getRecentSearches with large limit
        val userId = getCurrentUserId()
        return appDatabase.searchHistoryDao().getRecentSearches(userId, limit = 10000).first()
    }
    
    private suspend fun exportSearchKeywords(): List<SearchKeywordEntity> {
        return appDatabase.searchKeywordDao().getAllKeywords().first()
    }
    
    // Incremental export methods (only modified records)
    private suspend fun exportUsersIncremental(lastSyncTimestamp: Long): List<UserEntity> {
        return exportUsers().filter { 
            it.updatedAt.time > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportCarsIncremental(lastSyncTimestamp: Long): List<CarEntity> {
        return exportCars().filter { 
            it.lastModified.time > lastSyncTimestamp || 
            it.updatedAt > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportPhotosIncremental(lastSyncTimestamp: Long): List<PhotoEntity> {
        // PhotoEntity uses createdAt, not lastModified
        return exportPhotos().filter { 
            it.createdAt.time > lastSyncTimestamp ||
            (it.lastSyncedAt?.time ?: 0L) > lastSyncTimestamp
        }
    }
    
    private suspend fun exportPriceHistoryIncremental(lastSyncTimestamp: Long): List<PriceHistoryEntity> {
        return exportPriceHistory().filter { 
            it.createdAt.time > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportTradeOffersIncremental(lastSyncTimestamp: Long): List<TradeOfferEntity> {
        return exportTradeOffers().filter { 
            it.updatedAt.time > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportWishlistIncremental(lastSyncTimestamp: Long): List<WishlistEntity> {
        return exportWishlist().filter { 
            it.updatedAt.time > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportSearchHistoryIncremental(lastSyncTimestamp: Long): List<SearchHistoryEntity> {
        // SearchHistoryEntity uses timestamp, not createdAt
        return exportSearchHistory().filter { 
            it.timestamp.time > lastSyncTimestamp 
        }
    }
    
    private suspend fun exportSearchKeywordsIncremental(lastSyncTimestamp: Long): List<SearchKeywordEntity> {
        // SearchKeywords don't have timestamp, export all
        return exportSearchKeywords()
    }
    
    /**
     * Syncs the latest backup to cloud storage as backup_latest.json.
     * This method exports the database and uploads it with the latest filename.
     * Used for automatic backup updates when cars are added.
     * 
     * @return Result containing the cloud path if successful, or error if failed
     */
    suspend fun syncLatestBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING LATEST BACKUP SYNC ===")
            
            // Step 1: Check storage location
            val storageLocation = userPreferences.storageLocation.first()
            Log.d(TAG, "Storage location: $storageLocation")
            
            // Only sync if storage is cloud-based
            if (storageLocation != "Google Drive" && storageLocation != "GoogleDrive" && 
                storageLocation != "OneDrive" && storageLocation != "Dropbox") {
                Log.w(TAG, "Storage location is Local ($storageLocation) - skipping cloud backup")
                return@withContext Result.success("local://skipped")
            }
            
            // Step 2: Export database to JSON
            Log.d(TAG, "Exporting database to JSON...")
            val exportResult = exportDatabaseToJson()
            if (exportResult.isFailure) {
                Log.e(TAG, "❌ Database export failed: ${exportResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
            }
            
            val jsonFile = exportResult.getOrNull()
            if (jsonFile == null || !jsonFile.exists()) {
                Log.e(TAG, "❌ Exported JSON file is null or doesn't exist")
                return@withContext Result.failure(Exception("Exported file not found"))
            }
            
            Log.d(TAG, "✅ Database exported: ${jsonFile.absolutePath} (${jsonFile.length()} bytes)")
            
            // Step 3: Upload JSON to cloud with backup_latest.json name
            Log.d(TAG, "Uploading backup to cloud storage: $storageLocation")
            val cloudPath = when (storageLocation) {
                "Google Drive", "GoogleDrive" -> {
                    Log.d(TAG, "Uploading latest backup to Google Drive as $BACKUP_FILENAME_LATEST...")
                    uploadLatestBackupToGoogleDrive(jsonFile)
                }
                "OneDrive" -> {
                    Log.d(TAG, "Uploading latest backup to OneDrive as $BACKUP_FILENAME_LATEST...")
                    uploadLatestBackupToOneDrive(jsonFile)
                }
                "Dropbox" -> {
                    Log.d(TAG, "Uploading latest backup to Dropbox as $BACKUP_FILENAME_LATEST...")
                    uploadLatestBackupToDropbox(jsonFile)
                }
                else -> {
                    Log.w(TAG, "Storage location is Local - skipping cloud upload")
                    Result.success("local://${jsonFile.absolutePath}")
                }
            }
            
            if (cloudPath.isSuccess) {
                Log.i(TAG, "✅ Latest backup uploaded successfully to: ${cloudPath.getOrNull()}")
            } else {
                Log.e(TAG, "❌ Latest backup upload failed: ${cloudPath.exceptionOrNull()?.message}")
            }
            
            cloudPath
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Latest backup sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Cloud upload methods
    private suspend fun uploadToGoogleDrive(file: File): Result<String> {
        return try {
            // BACKUP_FOLDER is "HotWheelsCollectors/database"
            // We pass the full path, uploadFile will handle folder creation
            googleDriveRepository.uploadFile(file, BACKUP_FOLDER, file.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadLatestBackupToGoogleDrive(file: File): Result<String> {
        return try {
            // Upload with backup_latest.json name
            googleDriveRepository.uploadFile(file, BACKUP_FOLDER, BACKUP_FILENAME_LATEST)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload latest backup to Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadToOneDrive(file: File): Result<String> {
        return try {
            oneDriveRepository.uploadFile(file, BACKUP_FOLDER, file.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to OneDrive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadLatestBackupToOneDrive(file: File): Result<String> {
        return try {
            oneDriveRepository.uploadFile(file, BACKUP_FOLDER, BACKUP_FILENAME_LATEST)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload latest backup to OneDrive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadToDropbox(file: File): Result<String> {
        return try {
            dropboxRepository.uploadFile(file, BACKUP_FOLDER, file.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to Dropbox: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadLatestBackupToDropbox(file: File): Result<String> {
        return try {
            dropboxRepository.uploadFile(file, BACKUP_FOLDER, BACKUP_FILENAME_LATEST)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload latest backup to Dropbox: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadPhotoToGoogleDrive(file: File, photo: PhotoEntity): Result<String> {
        return try {
            val fileName = "${photo.id}.jpg"
            val userId = getCurrentUserId()
            val folderPath = "$PHOTOS_FOLDER/$userId/${photo.carId}"
            googleDriveRepository.uploadFile(file, folderPath, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo to Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadPhotoToOneDrive(file: File, photo: PhotoEntity): Result<String> {
        return try {
            val fileName = "${photo.id}.jpg"
            val userId = getCurrentUserId()
            val folderPath = "$PHOTOS_FOLDER/$userId/${photo.carId}"
            oneDriveRepository.uploadFile(file, folderPath, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo to OneDrive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadPhotoToDropbox(file: File, photo: PhotoEntity): Result<String> {
        return try {
            val fileName = "${photo.id}.jpg"
            val userId = getCurrentUserId()
            val folderPath = "$PHOTOS_FOLDER/$userId/${photo.carId}"
            dropboxRepository.uploadFile(file, folderPath, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo to Dropbox: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Data class representing the exported database structure.
 */
data class DatabaseExport(
    val version: Int,
    val timestamp: Long,
    val userId: String,
    val isIncremental: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val tables: DatabaseTables
)

/**
 * Data class containing all database tables.
 */
data class DatabaseTables(
    val users: List<UserEntity>,
    val cars: List<CarEntity>,
    val photos: List<PhotoEntity>,
    val priceHistory: List<PriceHistoryEntity>,
    val tradeOffers: List<TradeOfferEntity>,
    val wishlist: List<WishlistEntity>,
    val searchHistory: List<SearchHistoryEntity>,
    val searchKeywords: List<SearchKeywordEntity>
)
