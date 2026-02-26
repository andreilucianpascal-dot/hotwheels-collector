package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.dao.*
import com.example.hotwheelscollectors.data.local.entities.*
import com.example.hotwheelscollectors.data.repository.DatabaseExport
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserCloudRestoreManager handles restoring Room Database and photos from
 * user's cloud storage (Google Drive/OneDrive/Dropbox) when logging in on a new device.
 * 
 * RESPONSIBILITIES:
 * 1. Check if backup exists in cloud storage
 * 2. Download backup JSON from cloud
 * 3. Restore Room Database from JSON
 * 4. Download photos from cloud
 * 5. Update sync timestamp
 */
@Singleton
class UserCloudRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val gson: Gson,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository
) {
    
    companion object {
        private const val TAG = "UserCloudRestoreManager"
        private const val BACKUP_FILENAME_LATEST = "backup_latest.json"
        private const val BACKUP_FOLDER = "HotWheelsCollectors/database"
        private const val PHOTOS_FOLDER = "HotWheelsCollectors/photos"
    }
    
    /**
     * Checks if a backup exists in the user's cloud storage.
     * 
     * @return Result containing true if backup exists, false otherwise, or error if check failed
     */
    suspend fun checkForBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== CHECKING FOR BACKUP IN CLOUD ===")
            
            val storageLocation = userPreferences.storageLocation.first()
            Log.d(TAG, "Storage location: $storageLocation")
            
            if (storageLocation == "Device" || storageLocation == "Internal" || storageLocation == "Local" || storageLocation.isEmpty()) {
                Log.d(TAG, "Storage location is Local - no backup to check")
                return@withContext Result.success(false)
            }
            
            val backupExists = when (storageLocation) {
                "Google Drive", "GoogleDrive" -> checkBackupInGoogleDrive()
                "OneDrive" -> checkBackupInOneDrive()
                "Dropbox" -> checkBackupInDropbox()
                else -> {
                    Log.w(TAG, "Unknown storage location: $storageLocation")
                    false
                }
            }
            
            Log.d(TAG, "Backup exists: $backupExists")
            Result.success(backupExists)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for backup: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restores Room Database and photos from cloud storage.
     * 
     * @return Result indicating success or failure
     */
    suspend fun restoreFromCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING RESTORE FROM CLOUD ===")
            
            // Step 1: Check if backup exists
            val backupCheckResult = checkForBackup()
            if (backupCheckResult.isFailure) {
                Log.e(TAG, "Failed to check for backup: ${backupCheckResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(backupCheckResult.exceptionOrNull() ?: Exception("Backup check failed"))
            }
            
            val backupExists = backupCheckResult.getOrNull() ?: false
            if (!backupExists) {
                Log.d(TAG, "No backup found in cloud - nothing to restore")
                return@withContext Result.success(Unit)
            }
            
            Log.i(TAG, "Backup found in cloud - starting restore...")
            
            // Step 2: Download backup JSON from cloud
            val downloadResult = downloadBackupJsonFromCloud()
            if (downloadResult.isFailure) {
                Log.e(TAG, "Failed to download backup JSON: ${downloadResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            val jsonFile = downloadResult.getOrNull()
            if (jsonFile == null || !jsonFile.exists()) {
                Log.e(TAG, "Downloaded JSON file is null or doesn't exist")
                return@withContext Result.failure(Exception("Downloaded file not found"))
            }
            
            Log.i(TAG, "✅ Backup JSON downloaded: ${jsonFile.absolutePath} (${jsonFile.length()} bytes)")
            
            // Step 3: Restore Room Database from JSON
            Log.d(TAG, "Restoring Room Database from JSON...")
            val restoreResult = restoreDatabaseFromJson(jsonFile)
            if (restoreResult.isFailure) {
                Log.e(TAG, "Failed to restore database: ${restoreResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(restoreResult.exceptionOrNull() ?: Exception("Restore failed"))
            }
            
            Log.i(TAG, "✅ Room Database restored successfully")
            
            // Step 4: Download photos from cloud (optional - can be done incrementally)
            Log.d(TAG, "Downloading photos from cloud...")
            val photosResult = downloadPhotosFromCloud()
            if (photosResult.isFailure) {
                Log.e(TAG, "Failed to download photos: ${photosResult.exceptionOrNull()?.message}")
                // Don't fail entire restore if photos download fails - photos can be downloaded incrementally
                Log.w(TAG, "⚠️ Photos download failed, but continuing restore...")
            } else {
                val downloadedCount = photosResult.getOrNull() ?: 0
                Log.i(TAG, "✅ Photos downloaded: $downloadedCount")
            }
            
            // Step 5: Update sync timestamp to current time (to avoid re-syncing everything)
            val currentTime = System.currentTimeMillis()
            userPreferences.updateLastCloudSync(currentTime)
            Log.i(TAG, "✅ Restore completed successfully at ${Date(currentTime)}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restores Room Database from JSON file.
     * Uses transaction to ensure atomicity and handle FOREIGN KEY constraints.
     * 
     * @param jsonFile The JSON file containing database export
     * @return Result indicating success or failure
     */
    private suspend fun restoreDatabaseFromJson(jsonFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reading JSON export file: ${jsonFile.absolutePath}")
            
            // Read JSON file
            val exportData: DatabaseExport = FileReader(jsonFile).use { reader ->
                gson.fromJson(reader, DatabaseExport::class.java)
            }
            
            Log.d(TAG, "Restoring database tables...")
            
            // ✅ FIX: Use transaction to ensure atomicity and handle FOREIGN KEY constraints
            appDatabase.withTransaction {
                // Step 1: Restore Users first (no dependencies)
                val restoredUsers = mutableListOf<UserEntity>()
                
                if (exportData.tables.users.isNotEmpty()) {
                    exportData.tables.users.forEach { user ->
                        try {
                            appDatabase.userDao().insert(user)
                            restoredUsers.add(user)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to insert user ${user.id}: ${e.message}")
                        }
                    }
                    Log.d(TAG, "✅ Restored ${restoredUsers.size}/${exportData.tables.users.size} users from backup")
                }
                
                // ✅ FIX: Get current user ID from Firebase Auth (matches CollectionViewModel)
                val currentUserId = authRepository.getCurrentUser()?.uid
                    ?: userPreferences.userName.first().takeIf { it.isNotEmpty() }
                    ?: "unknown_user"
                
                Log.d(TAG, "Current Firebase user ID: $currentUserId")
                Log.d(TAG, "Backup contains ${exportData.tables.cars.size} cars")
                
                // ✅ FIX: Create UserEntity for current user if missing (to avoid FOREIGN KEY errors)
                var currentUserExists = restoredUsers.any { it.id == currentUserId } 
                    || appDatabase.userDao().getById(currentUserId) != null
                
                if (!currentUserExists) {
                    Log.w(TAG, "⚠️ Current user not found in backup - creating UserEntity...")
                    val newUser = UserEntity(
                        id = currentUserId,
                        email = authRepository.getCurrentUser()?.email ?: "",
                        name = authRepository.getCurrentUser()?.displayName ?: currentUserId,
                        preferences = emptyMap(),
                        lastLoginAt = Date(),
                        createdAt = Date(),
                        updatedAt = Date(),
                        syncStatus = SyncStatus.SYNCED,
                        version = 1
                    )
                    
                    try {
                        appDatabase.userDao().insert(newUser)
                        restoredUsers.add(newUser)
                        currentUserExists = true
                        Log.i(TAG, "✅ Created current UserEntity: $currentUserId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to create UserEntity: ${e.message}", e)
                    }
                }
                
                // ✅ FIX: Step 2: Update all cars to use current user's ID so they appear in "My Collection"
                // This ensures restored cars are visible to the current Firebase user
                // Also check for existing cars to avoid duplicates - use OnConflictStrategy.REPLACE
                val existingCarIds = appDatabase.carDao().getAllCars().first().map { it.id }.toSet()
                Log.d(TAG, "Existing cars in DB: ${existingCarIds.size}")
                
                val carsToRestore = exportData.tables.cars.map { car ->
                    var correctedCar = car
                    
                    // Update userId to current user
                    if (car.userId != currentUserId) {
                        Log.d(TAG, "Updating car ${car.id} userId from '${car.userId}' to '$currentUserId'")
                        correctedCar = correctedCar.copy(userId = currentUserId)
                    }
                    
                    // ✅ FIX: Ensure Premium cars have correct series and isPremium flag
                    val seriesLower = correctedCar.series.lowercase()
                    if (seriesLower.contains("premium") || correctedCar.isPremium) {
                        if (!correctedCar.isPremium) {
                            Log.d(TAG, "Fixing Premium car ${correctedCar.id}: setting isPremium = true (series: '${correctedCar.series}')")
                            correctedCar = correctedCar.copy(isPremium = true)
                        }
                        if (correctedCar.series != "Premium") {
                            Log.d(TAG, "Fixing Premium car ${correctedCar.id}: setting series = 'Premium' (was: '${correctedCar.series}')")
                            correctedCar = correctedCar.copy(series = "Premium")
                        }
                    }
                    
                    correctedCar
                }
                
                // Filter out duplicates before insert (OnConflictStrategy.REPLACE should handle this, but we do it explicitly)
                val newCars = carsToRestore.filterNot { it.id in existingCarIds }
                val duplicateCars = carsToRestore.filter { it.id in existingCarIds }
                
                Log.d(TAG, "Cars to restore: ${newCars.size} new, ${duplicateCars.size} duplicates (will be replaced)")
                
                if (carsToRestore.isNotEmpty()) {
                    try {
                        // Use insertCars which has OnConflictStrategy.REPLACE - this will replace existing cars
                        appDatabase.carDao().insertCars(carsToRestore)
                        Log.d(TAG, "✅ Restored ${carsToRestore.size} cars with userId='$currentUserId'")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to insert cars: ${e.message}", e)
                        // Try inserting one by one to identify problematic cars
                        var successCount = 0
                        for (car in carsToRestore) {
                            try {
                                appDatabase.carDao().insertCar(car)
                                successCount++
                            } catch (ex: Exception) {
                                Log.w(TAG, "Failed to insert car ${car.id}: ${ex.message}")
                            }
                        }
                        Log.d(TAG, "✅ Restored $successCount/${carsToRestore.size} cars individually")
                    }
                } else {
                    Log.d(TAG, "⚠️ No cars to restore")
                }

                // ✅ FIX: Step 3: Restore Photos (depends on Cars)
                // Update contributorUserId to currentUserId so photos are linked to current user
                // Get all valid car IDs (both newly restored and existing)
                val allValidCarIds = appDatabase.carDao().getAllCars().first().map { it.id }.toSet()
                val existingPhotoIds = appDatabase.photoDao().getAllPhotos().first().map { it.id }.toSet()
                
                val photosToRestore = exportData.tables.photos
                    .filterNot { it.id in existingPhotoIds } // Skip photos that already exist
                    .map { photo ->
                        if (photo.carId in allValidCarIds) {
                            if (photo.contributorUserId != currentUserId) {
                                Log.d(TAG, "Updating photo ${photo.id} contributorUserId to '$currentUserId'")
                                photo.copy(contributorUserId = currentUserId)
                            } else {
                                photo
                            }
                        } else {
                            Log.w(TAG, "Skipping photo ${photo.id} with invalid carId: ${photo.carId}")
                            null
                        }
                    }.filterNotNull()
                
                if (photosToRestore.isNotEmpty()) {
                    appDatabase.photoDao().insertPhotos(photosToRestore)
                    Log.d(TAG, "✅ Restored ${photosToRestore.size} photos with contributorUserId='$currentUserId' (${exportData.tables.photos.size - photosToRestore.size} skipped)")
                } else {
                    Log.d(TAG, "⚠️ No new photos to restore - all ${exportData.tables.photos.size} photos already exist")
                }

                // Step 4: Restore Price History (depends on Cars)
                val validPriceHistory = exportData.tables.priceHistory.filter { price ->
                    price.carId in allValidCarIds
                }
                
                if (validPriceHistory.isNotEmpty()) {
                    appDatabase.priceHistoryDao().insertPriceRecords(validPriceHistory)
                    Log.d(TAG, "✅ Restored ${validPriceHistory.size} price history records")
                }

                // ✅ FIX: Step 5: Restore Trade Offers (update userId to currentUserId)
                val tradeOffersToRestore = exportData.tables.tradeOffers.map { offer ->
                    if (offer.userId != currentUserId) {
                        Log.d(TAG, "Updating trade offer ${offer.id} userId to '$currentUserId'")
                        offer.copy(userId = currentUserId)
                    } else {
                        offer
                    }
                }
                
                if (tradeOffersToRestore.isNotEmpty()) {
                    tradeOffersToRestore.forEach { offer ->
                        try {
                            appDatabase.tradeDao().insertTradeOffer(offer)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to insert trade offer ${offer.id}: ${e.message}")
                        }
                    }
                    Log.d(TAG, "✅ Restored ${tradeOffersToRestore.size} trade offers")
                }

                // ✅ FIX: Step 6: Restore Wishlist (update userId to currentUserId)
                val wishlistToRestore = exportData.tables.wishlist.map { item ->
                    if (item.userId != currentUserId) {
                        Log.d(TAG, "Updating wishlist item ${item.id} userId to '$currentUserId'")
                        item.copy(userId = currentUserId)
                    } else {
                        item
                    }
                }
                
                if (wishlistToRestore.isNotEmpty()) {
                    wishlistToRestore.forEach { item ->
                        try {
                            appDatabase.wishlistDao().insertWishlistItem(item)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to insert wishlist item ${item.id}: ${e.message}")
                        }
                    }
                    Log.d(TAG, "✅ Restored ${wishlistToRestore.size} wishlist items")
                }

                // ✅ FIX: Step 7: Restore Search History (update userId to currentUserId)
                val searchHistoryToRestore = exportData.tables.searchHistory.map { search ->
                    if (search.userId != currentUserId) {
                        Log.d(TAG, "Updating search history userId to '$currentUserId'")
                        search.copy(userId = currentUserId)
                    } else {
                        search
                    }
                }
                
                if (searchHistoryToRestore.isNotEmpty()) {
                    searchHistoryToRestore.forEach { search ->
                        try {
                            appDatabase.searchHistoryDao().insertSearch(search)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to insert search history: ${e.message}")
                        }
                    }
                    Log.d(TAG, "✅ Restored ${searchHistoryToRestore.size} search history records")
                }

                // Step 8: Restore Search Keywords (no dependencies)
                if (exportData.tables.searchKeywords.isNotEmpty()) {
                    try {
                        appDatabase.searchKeywordDao().insertKeywords(exportData.tables.searchKeywords)
                        Log.d(TAG, "✅ Restored ${exportData.tables.searchKeywords.size} search keywords")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to insert search keywords: ${e.message}")
                    }
                }
            }

            Log.i(TAG, "✅ Database restore completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database restore from JSON failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private suspend fun checkBackupInGoogleDrive(): Boolean {
        return try {
            val result = googleDriveRepository.checkFileExists(BACKUP_FOLDER, BACKUP_FILENAME_LATEST)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check backup in Google Drive: ${e.message}", e)
            false
        }
    }
    
    private suspend fun checkBackupInOneDrive(): Boolean {
        // TODO: Implement OneDrive API check for backup_latest.json
        return false
    }
    
    private suspend fun checkBackupInDropbox(): Boolean {
        // TODO: Implement Dropbox API check for backup_latest.json
        return false
    }
    
    private suspend fun downloadBackupJsonFromCloud(): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DOWNLOADING BACKUP JSON FROM CLOUD ===")
            
            val storageLocation = userPreferences.storageLocation.first()
            Log.d(TAG, "Storage location: $storageLocation")
            
            // Create temporary file for download
            val downloadDir = File(context.filesDir, "sync_downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val tempFile = File(downloadDir, BACKUP_FILENAME_LATEST)
            
            val downloadResult = when (storageLocation) {
                "Google Drive", "GoogleDrive" -> {
                    Log.d(TAG, "Downloading from Google Drive...")
                    googleDriveRepository.downloadFile(BACKUP_FOLDER, BACKUP_FILENAME_LATEST, tempFile)
                }
                "OneDrive" -> {
                    Log.d(TAG, "Downloading from OneDrive...")
                    downloadFromOneDrive(tempFile)
                }
                "Dropbox" -> {
                    Log.d(TAG, "Downloading from Dropbox...")
                    downloadFromDropbox(tempFile)
                }
                else -> {
                    Log.w(TAG, "Storage location is Local - cannot download")
                    Result.failure(IllegalStateException("Storage location is Local"))
                }
            }
            
            if (downloadResult.isSuccess && tempFile.exists() && tempFile.length() > 0) {
                Log.i(TAG, "✅ Backup JSON downloaded: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                Result.success(tempFile)
            } else {
                Log.e(TAG, "❌ Backup JSON download failed")
                Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Backup JSON download failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun downloadFromOneDrive(tempFile: File): Result<Unit> {
        // TODO: Implement OneDrive API download
        return Result.failure(Exception("OneDrive download not yet implemented"))
    }
    
    private suspend fun downloadFromDropbox(tempFile: File): Result<Unit> {
        // TODO: Implement Dropbox API download
        return Result.failure(Exception("Dropbox download not yet implemented"))
    }
    
    private suspend fun downloadPhotosFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== DOWNLOADING PHOTOS FROM CLOUD ===")
            
            val storageLocation = userPreferences.storageLocation.first()
            Log.d(TAG, "Storage location: $storageLocation")
            
            // Only download if storage is cloud-based
            if (storageLocation != "Google Drive" && storageLocation != "GoogleDrive" && 
                storageLocation != "OneDrive" && storageLocation != "Dropbox") {
                Log.d(TAG, "Storage location is Local ($storageLocation) - skipping photo download")
                return@withContext Result.success(0)
            }
            
            // ✅ FIX: Get all photos that have cloudPath but no localPath (or localPath doesn't exist)
            val allPhotos = appDatabase.photoDao().getAllPhotos().first()
            Log.d(TAG, "Total photos in DB: ${allPhotos.size}")
            
            // Log sample photos to debug cloudPath
            allPhotos.take(3).forEach { photo ->
                Log.d(TAG, "Sample photo: id=${photo.id}, cloudPath='${photo.cloudPath}', driveFileId='${photo.driveFileId}', localPath='${photo.localPath}'")
            }
            
            val photosToDownload = allPhotos.filter { photo ->
                // Download if:
                // 1. Has cloudPath (from backup) OR driveFileId (for programmatic download)
                // 2. localPath is empty or file doesn't exist
                val hasCloudReference = (photo.cloudPath != null && photo.cloudPath.isNotEmpty()) || photo.driveFileId != null
                val needsDownload = photo.localPath.isEmpty() || !File(photo.localPath).exists()
                val shouldDownload = hasCloudReference && needsDownload
                
                if (hasCloudReference && !needsDownload) {
                    Log.d(TAG, "Photo ${photo.id} already has local file: ${photo.localPath}")
                } else if (!hasCloudReference) {
                    Log.w(TAG, "Photo ${photo.id} has no cloudPath or driveFileId - cannot download")
                }
                
                shouldDownload
            }
            
            Log.d(TAG, "Photos to download: ${photosToDownload.size}/${allPhotos.size}")
            
            if (photosToDownload.isEmpty()) {
                Log.d(TAG, "No photos need to be downloaded")
                return@withContext Result.success(0)
            }
            
            var downloadedCount = 0
            
            // Download photos based on storage location
            when (storageLocation) {
                "Google Drive", "GoogleDrive" -> {
                    downloadedCount = downloadPhotosFromGoogleDrive(photosToDownload)
                }
                "OneDrive" -> {
                    Log.w(TAG, "OneDrive photo download not yet implemented")
                    // TODO: Implement OneDrive photo download
                }
                "Dropbox" -> {
                    Log.w(TAG, "Dropbox photo download not yet implemented")
                    // TODO: Implement Dropbox photo download
                }
            }
            
            Log.i(TAG, "✅ Photos download completed: $downloadedCount/${photosToDownload.size}")
            Result.success(downloadedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Photos download failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun downloadPhotosFromGoogleDrive(photos: List<PhotoEntity>): Int {
        var downloadedCount = 0
        
        for (photo in photos) {
            try {
                // ✅ FIX: Try driveFileId first, then fallback to cloudPath
                val driveFileId = photo.driveFileId
                val cloudPath = photo.cloudPath?.takeIf { it.isNotEmpty() }
                
                if (driveFileId == null && cloudPath == null) {
                    Log.w(TAG, "Skipping photo ${photo.id} - no driveFileId or cloudPath")
                    continue
                }
                
                // Create local directory for photos: filesDir/photos/{carId}/
                val userId = photo.contributorUserId ?: userPreferences.userName.first().takeIf { it.isNotEmpty() } ?: "unknown_user"
                val photosDir = File(context.filesDir, "$PHOTOS_FOLDER/$userId/${photo.carId}")
                photosDir.mkdirs()
                
                // Determine file name
                val fileName = "${photo.id}.jpg"
                val localFile = File(photosDir, fileName)
                
                // ✅ FIX: Download using driveFileId if available, or extract from Google Drive URL
                val downloadResult = when {
                    driveFileId != null && driveFileId.isNotEmpty() -> {
                        Log.d(TAG, "Downloading photo ${photo.id} from Drive (fileId: $driveFileId)")
                        googleDriveRepository.downloadFileByFileId(driveFileId, localFile)
                    }
                    cloudPath != null && cloudPath.contains("drive.google.com/file/d/") -> {
                        // Extract driveFileId from Google Drive URL
                        // Format: https://drive.google.com/file/d/FILE_ID/view?usp=drivesdk
                        val extractedFileId = extractDriveFileIdFromUrl(cloudPath)
                        if (extractedFileId != null) {
                            Log.d(TAG, "Downloading photo ${photo.id} from Drive (extracted fileId from URL: $extractedFileId)")
                            googleDriveRepository.downloadFileByFileId(extractedFileId, localFile)
                        } else {
                            Log.e(TAG, "Failed to extract fileId from URL: $cloudPath")
                            Result.failure(IllegalArgumentException("Invalid Google Drive URL: $cloudPath"))
                        }
                    }
                    cloudPath != null && !cloudPath.startsWith("http") -> {
                        // Try folder path approach for non-URL cloudPath
                        Log.d(TAG, "Downloading photo ${photo.id} from Drive (cloudPath: $cloudPath)")
                        val pathParts = cloudPath.split("/")
                        val extractedFileName = pathParts.lastOrNull() ?: fileName
                        val folderPath = pathParts.dropLast(1).joinToString("/")
                        googleDriveRepository.downloadFile(folderPath, extractedFileName, localFile)
                    }
                    else -> {
                        Result.failure(IllegalStateException("No valid driveFileId or cloudPath: driveFileId='$driveFileId', cloudPath='$cloudPath'"))
                    }
                }
                
                if (downloadResult.isSuccess && localFile.exists() && localFile.length() > 0) {
                    // Update PhotoEntity with localPath
                    val updatedPhoto = photo.copy(
                        localPath = localFile.absolutePath,
                        fullSizePath = localFile.absolutePath,
                        thumbnailPath = localFile.absolutePath,
                        lastSyncedAt = Date(),
                        syncStatus = SyncStatus.SYNCED
                    )
                    
                    appDatabase.photoDao().updatePhoto(updatedPhoto)
                    downloadedCount++
                    
                    // ✅ FIX: Update CarEntity.combinedPhotoPath and frontPhotoPath with localPath
                    // This ensures CarCard can display the photo correctly
                    val car = appDatabase.carDao().getCarById(photo.carId)
                    if (car != null) {
                        val updatedCar = when (photo.type) {
                            PhotoType.FRONT -> car.copy(
                                frontPhotoPath = localFile.absolutePath,
                                combinedPhotoPath = localFile.absolutePath // Use front photo as combined if only front photo exists
                            )
                            PhotoType.BACK -> car.copy(
                                combinedPhotoPath = localFile.absolutePath // Update combined if back photo is downloaded
                            )
                            PhotoType.CARD_FRONT, PhotoType.CARD_BACK, PhotoType.OTHER -> car.copy(
                                combinedPhotoPath = localFile.absolutePath // Update combined for other photo types
                            )
                            else -> car
                        }
                        appDatabase.carDao().updateCar(updatedCar)
                        Log.d(TAG, "✅ Updated CarEntity ${photo.carId} with localPath: ${localFile.absolutePath}")
                    }
                    
                    Log.d(TAG, "✅ Photo ${photo.id} downloaded: ${localFile.absolutePath} (${localFile.length()} bytes)")
                } else {
                    val error = downloadResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "⚠️ Photo ${photo.id} download failed: $error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to download photo ${photo.id}: ${e.message}", e)
                // Continue with next photo
            }
        }
        
        return downloadedCount
    }
    
    /**
     * Extracts Google Drive file ID from a Google Drive URL.
     * Supports formats like:
     * - https://drive.google.com/file/d/FILE_ID/view?usp=drivesdk
     * - https://drive.google.com/file/d/FILE_ID/view
     * - https://drive.google.com/open?id=FILE_ID
     */
    private fun extractDriveFileIdFromUrl(url: String): String? {
        return try {
            // Pattern 1: /file/d/FILE_ID/view
            val pattern1 = Regex("/file/d/([a-zA-Z0-9_-]+)")
            pattern1.find(url)?.groupValues?.get(1)
                // Pattern 2: ?id=FILE_ID
                ?: Regex("[?&]id=([a-zA-Z0-9_-]+)").find(url)?.groupValues?.get(1)
                // Pattern 3: #gid=FILE_ID (for spreadsheets)
                ?: Regex("#gid=([a-zA-Z0-9_-]+)").find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract file ID from URL: $url", e)
            null
        }
    }
}
