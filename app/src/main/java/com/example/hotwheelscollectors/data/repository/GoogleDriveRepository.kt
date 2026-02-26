package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.UserEntity
import com.example.hotwheelscollectors.data.local.entities.PriceHistoryEntity
import com.example.hotwheelscollectors.data.local.entities.TradeOfferEntity
import com.example.hotwheelscollectors.data.local.entities.WishlistEntity
import com.example.hotwheelscollectors.data.local.entities.SearchHistoryEntity
import com.example.hotwheelscollectors.data.local.entities.SearchKeywordEntity
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.model.PersonalStorageType
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.data.repository.DatabaseExport
import com.example.hotwheelscollectors.data.repository.DatabaseTables
import androidx.room.withTransaction
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.File as JavaFile
import java.util.*
import java.util.Date
import javax.inject.Inject

/**
 * Data class to hold Drive file information (fileId + fileUrl).
 */
data class DriveFileInfo(
    val fileId: String,
    val fileUrl: String
)

/**
 * GoogleDriveRepository handles saving cars and photos to Google Drive.
 * Uses Google API Client Library instead of direct REST API calls.
 * 
 * RESPONSIBILITIES:
 * 1. Upload photos to Google Drive
 * 2. Save car metadata to Room Database (with Drive URLs and fileIds)
 * 3. Create PhotoEntity records with Drive URLs and fileIds
 * 4. Handle Google Drive API authentication
 */
class GoogleDriveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val gson: Gson,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val driveRootFolderManager: DriveRootFolderManager
) : UserStorageRepository {
    
    private val folderName = "HotWheelsCollectors"
    private val dbJsonFileName = "db.json"
    private var cachedDbJsonFileId: String? = null
    
    // JSON factory for Drive API
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    
    // HTTP transport for Drive API
    private val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    
    /**
     * Gets a Drive service instance using GoogleAccountCredential.
     * This properly handles OAuth 2.0 access tokens and refresh tokens.
     */
    private suspend fun getDriveService(account: GoogleSignInAccount): Drive = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account
            
            Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("HotWheelsCollectors")
                .build()
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to create Drive service: ${e.message}", e)
            throw ReAuthRequiredException("Failed to create Drive service: ${e.message}")
        }
    }
    
    /**
     * Saves a car with its photos to Google Drive.
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
            Log.d("GoogleDriveRepository", "=== STARTING GOOGLE DRIVE SAVE ===")
            Log.d("GoogleDriveRepository", "Car ID: $carId")
            
            // ✅ PAS 2: Check if Drive is PRIMARY storage
            val storageType = userPreferences.storageType.first()
            val isDrivePrimary = storageType == PersonalStorageType.GOOGLE_DRIVE
            Log.d("GoogleDriveRepository", "Storage mode: $storageType (Drive is primary: $isDrivePrimary)")
            
            // Validate input files exist BEFORE upload
            val thumbnailFile = JavaFile(localThumbnail)
            val fullFile = JavaFile(localFull)
            
            if (localThumbnail.isEmpty() || !thumbnailFile.exists()) {
                Log.e("GoogleDriveRepository", "❌ Thumbnail file not found: $localThumbnail")
                Log.e("GoogleDriveRepository", "   File exists: ${thumbnailFile.exists()}")
                Log.e("GoogleDriveRepository", "   File absolute path: ${thumbnailFile.absolutePath}")
                return@withContext Result.failure(
                    IllegalArgumentException("Thumbnail file not found: $localThumbnail")
                )
            }
            
            if (localFull.isEmpty() || !fullFile.exists()) {
                Log.e("GoogleDriveRepository", "❌ Full-size file not found: $localFull")
                Log.e("GoogleDriveRepository", "   File exists: ${fullFile.exists()}")
                Log.e("GoogleDriveRepository", "   File absolute path: ${fullFile.absolutePath}")
                return@withContext Result.failure(
                    IllegalArgumentException("Full-size file not found: $localFull")
                )
            }
            
            Log.d("GoogleDriveRepository", "✅ Files validated:")
            Log.d("GoogleDriveRepository", "   Thumbnail: ${thumbnailFile.absolutePath} (${thumbnailFile.length()} bytes)")
            Log.d("GoogleDriveRepository", "   Full: ${fullFile.absolutePath} (${fullFile.length()} bytes)")
            
            // Check Google Sign-In
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(
                    IllegalStateException("User not signed in to Google Drive")
                )
            
            // Get Drive service
            val driveService = getDriveService(account)
            
            // Get or create Drive folder
            val folderId = getOrCreateFolder(driveService)
            Log.d("GoogleDriveRepository", "Drive folder ID: $folderId")
            
            // Upload photos to Drive
            val thumbnailInfo = uploadPhotoToDrive(driveService, localThumbnail, folderId, "thumbnail_$carId.jpg")
            val fullInfo = uploadPhotoToDrive(driveService, localFull, folderId, "full_$carId.jpg")
            
            Log.d("GoogleDriveRepository", "Photos uploaded to Drive:")
            Log.d("GoogleDriveRepository", "  - Thumbnail: ID=${thumbnailInfo.fileId}, URL=${thumbnailInfo.fileUrl}")
            Log.d("GoogleDriveRepository", "  - Full: ID=${fullInfo.fileId}, URL=${fullInfo.fileUrl}")
            
            // ✅ FIX: Pentru Premium, subseries trebuie să fie "category/subcategory" SAU doar "category"
            // Same logic as LocalRepository
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
            
            Log.d("GoogleDriveRepository", "✅ Computed subseries: '$subseries' (Premium: ${data.isPremium}, Category: '${data.category}', Subcategory: '${data.subcategory}')")
            
            // Create CarEntity with Drive URLs
            val carEntity = CarEntity(
                id = carId,
                userId = data.userId,
                model = data.name,
                brand = data.brand,
                series = data.series,
                subseries = subseries, // ✅ Fixed: Use computed subseries for Premium cars
                folderPath = data.series,
                color = data.color,
                year = data.year ?: 0,
                barcode = barcode,
                notes = data.notes,
                isTH = data.isTH,
                isSTH = data.isSTH,
                isPremium = data.isPremium, // ✅ Ensure isPremium is set correctly
                timestamp = System.currentTimeMillis(),
                lastModified = Date(),
                syncStatus = SyncStatus.SYNCED, // Already on Drive
                photoUrl = fullInfo.fileUrl,
                frontPhotoPath = fullInfo.fileUrl,
                combinedPhotoPath = thumbnailInfo.fileUrl,
                originalBrowsePhotoUrl = data.originalBrowsePhotoUrl // ✅ Firebase URL from Browse (null for Take Photos)
            )
            
            // Create PhotoEntity with Drive URLs and fileIds
            val photoEntity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                carId = carId,
                localPath = "", // No local copy initially (can be downloaded later)
                thumbnailPath = thumbnailInfo.fileUrl,
                fullSizePath = fullInfo.fileUrl,
                cloudPath = fullInfo.fileUrl,
                type = PhotoType.FRONT,
                syncStatus = SyncStatus.SYNCED,
                isTemporary = false,
                barcode = barcode.takeIf { it.isNotEmpty() },
                contributorUserId = data.userId,
                driveFileId = fullInfo.fileId,          // ✅ Save Drive fileId for download
                driveThumbnailFileId = thumbnailInfo.fileId  // ✅ Save Drive thumbnail fileId
            )
            
            // ✅ PAS 2: If Drive is PRIMARY, write to db.json (Drive is truth)
            if (isDrivePrimary) {
                Log.d("GoogleDriveRepository", "📝 Drive is PRIMARY - writing to db.json (Drive is truth)")
                
                // Update db.json in Drive with new car
                val updateResult = updateDbJsonWithCar(carEntity, listOf(photoEntity))
                if (updateResult.isFailure) {
                    Log.e("GoogleDriveRepository", "❌ Failed to update db.json: ${updateResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(
                        updateResult.exceptionOrNull() ?: Exception("Failed to update db.json")
                    )
                }
                
                Log.i("GoogleDriveRepository", "✅ Car saved to db.json in Drive (Drive is truth)")
                
                // Populate Room as cache for UI performance
                carDao.insertCar(carEntity)
                photoDao.insertPhoto(photoEntity)
                Log.i("GoogleDriveRepository", "✅ Car cached in Room Database (for UI performance)")
                
            } else {
                // ✅ Drive is NOT primary (backup mode) - write to Room directly
                Log.d("GoogleDriveRepository", "📝 Drive is NOT primary - writing to Room (backup mode)")
                
                carDao.insertCar(carEntity)
                photoDao.insertPhoto(photoEntity)
                Log.i("GoogleDriveRepository", "✅ Car saved to Room Database with Drive URLs")
            }
            
            Log.i("GoogleDriveRepository", "=== GOOGLE DRIVE SAVE COMPLETE ===")
            Result.success(carId)
            
        } catch (e: ReAuthRequiredException) {
            Log.e("GoogleDriveRepository", "❌ Re-authentication required: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Google Drive save failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Finds or creates the HotWheelsCollectors folder in Google Drive root.
     * ✅ FIX: Uses cached folder ID to prevent duplicate folder creation.
     * Verifică dacă folderul este în root (nu are parents) pentru a preveni duplicate.
     */
    private fun getOrCreateFolder(driveService: Drive): String {
        try {
            return driveRootFolderManager.getOrCreateRootFolderId(driveService)
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            Log.e("GoogleDriveRepository", "Failed to get/create folder: HTTP ${e.statusCode}", e)
            throw Exception("Failed to get/create folder: HTTP ${e.statusCode}")
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to get/create folder: ${e.message}", e)
            throw e
        }
    }

    /**
     * Public method to test Google Drive connection and create the folder.
     * Used by Settings screen to verify connection.
     */
suspend fun testConnectionAndCreateFolder(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== TESTING GOOGLE DRIVE CONNECTION ===")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(
                    IllegalStateException("User not signed in to Google Drive")
                )
            
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            
            Log.i("GoogleDriveRepository", "✅ Connection test successful! Folder ID: $folderId")
            Result.success(folderId)
        } catch (e: ReAuthRequiredException) {
            Log.e("GoogleDriveRepository", "❌ Re-authentication required: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Connection test failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a single photo to Google Drive.
     * Returns DriveFileInfo containing fileId and fileUrl.
     * 
     * ✅ CRITICAL: This method reads the file content immediately, so the file must exist
     * when this method is called. The file is read synchronously during upload.
     */
    private fun uploadPhotoToDrive(
        driveService: Drive,
        localPath: String,
        folderId: String,
        fileName: String
    ): DriveFileInfo {
        val file = JavaFile(localPath)
        
        // ✅ CRITICAL: Verify file exists and is readable BEFORE upload
        if (!file.exists()) {
            Log.e("GoogleDriveRepository", "❌ File does not exist: $localPath")
            throw IllegalArgumentException("File does not exist: $localPath")
        }
        
        if (!file.canRead()) {
            Log.e("GoogleDriveRepository", "❌ File is not readable: $localPath")
            throw IllegalArgumentException("File is not readable: $localPath")
        }
        
        Log.d("GoogleDriveRepository", "📤 Uploading file: $fileName")
        Log.d("GoogleDriveRepository", "   Path: ${file.absolutePath}")
        Log.d("GoogleDriveRepository", "   Size: ${file.length()} bytes")
        Log.d("GoogleDriveRepository", "   Exists: ${file.exists()}")
        Log.d("GoogleDriveRepository", "   Readable: ${file.canRead()}")
        
        try {
            val fileMetadata = File().apply {
                name = fileName
                parents = Collections.singletonList(folderId)
            }
            
            // ✅ CRITICAL: FileContent reads the file immediately during this call
            // The file must exist and be readable at this point
            val mediaContent = FileContent("image/jpeg", file)
            
            Log.d("GoogleDriveRepository", "📤 Starting Drive API upload for: $fileName")
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()
            
            val fileId = uploadedFile.id
            // ✅ FIX: Generate direct download URL instead of web view URL
            // Direct download URL format: https://drive.google.com/uc?export=download&id=FILE_ID
            // This allows image libraries (like Coil) to directly load the image
            val fileUrl = "https://drive.google.com/uc?export=download&id=$fileId"
            
            Log.d("GoogleDriveRepository", "✅ Photo uploaded successfully:")
            Log.d("GoogleDriveRepository", "   File ID: $fileId")
            Log.d("GoogleDriveRepository", "   File URL (direct download): $fileUrl")
            Log.d("GoogleDriveRepository", "   File name: ${uploadedFile.name}")
            
            return DriveFileInfo(fileId = fileId, fileUrl = fileUrl)
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                Log.e("GoogleDriveRepository", "❌ Authentication failed (401)")
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            Log.e("GoogleDriveRepository", "❌ Failed to upload photo: HTTP ${e.statusCode}", e)
            Log.e("GoogleDriveRepository", "   Error details: ${e.details}")
            throw Exception("Failed to upload file to Drive (HTTP ${e.statusCode}): ${e.message}")
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to upload photo: ${e.message}", e)
            Log.e("GoogleDriveRepository", "   Exception type: ${e::class.simpleName}")
            throw e
        }
    }
    
    /**
     * Uploads a photo to Google Drive (public method for backup use).
     * Used by CarSyncRepository for personal cloud backup.
     * 
     * @return Drive file URL (for backward compatibility)
     */
    suspend fun uploadPhoto(localPath: String, barcode: String, photoType: PhotoType): String {
        return try {
            Log.d("GoogleDriveRepository", "Uploading photo for backup: $localPath")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw Exception("User not signed in to Google Drive")
            
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            
            val timestamp = System.currentTimeMillis()
            val fileName = if (barcode.isNotEmpty()) {
                "${barcode}_${photoType.name.lowercase()}_$timestamp.jpg"
            } else {
                "photo_${photoType.name.lowercase()}_$timestamp.jpg"
            }
            
            val fileInfo = uploadPhotoToDrive(driveService, localPath, folderId, fileName)
            fileInfo.fileUrl  // Return URL for backward compatibility
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Photo backup upload failed: ${e.message}", e)
            ""
        }
    }
    
    /**
     * Generic file upload method for syncing backup files (JSON, photos, etc.) to Google Drive.
     * Used by UserCloudSyncRepository for automatic cloud sync.
     * 
     * @param file The file to upload
     * @param folderPath The folder path in Google Drive (e.g., "HotWheelsCollectors/database" or "HotWheelsCollectors/photos")
     * @param fileName The name for the file in Google Drive
     * @return Result containing the Google Drive file URL if successful, or error if failed
     */
    suspend fun uploadFile(file: JavaFile, folderPath: String, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "Uploading file to Google Drive: ${file.name}")
            Log.d("GoogleDriveRepository", "Folder path: $folderPath")
            Log.d("GoogleDriveRepository", "File name: $fileName")
            
            if (!file.exists()) {
                Log.e("GoogleDriveRepository", "File does not exist: ${file.absolutePath}")
                return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            }
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            
            // Get or create parent folder (HotWheelsCollectors)
            val parentFolderId = getOrCreateFolder(driveService)
            
            // Handle nested folder paths (e.g., "HotWheelsCollectors/photos/userId/carId")
            val pathParts = folderPath.split("/").filter { it.isNotEmpty() && it != folderName }
            var currentFolderId = parentFolderId
            
            // Create/get each subfolder in the path
            for (subfolderName in pathParts) {
                currentFolderId = getOrCreateSubfolder(driveService, currentFolderId, subfolderName)
            }
            
            val subfolderId = currentFolderId
            
            // Determine MIME type based on file extension
            val mimeType = when (file.extension.lowercase()) {
                "json" -> "application/json"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }
            
            // Upload file
            val fileUrl = uploadFileToDrive(driveService, file, subfolderId, fileName, mimeType)
            
            Log.d("GoogleDriveRepository", "✅ File uploaded successfully: $fileUrl")
            Result.success(fileUrl)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ File upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets or creates a subfolder within a parent folder.
     */
    private fun getOrCreateSubfolder(driveService: Drive, parentFolderId: String, subfolderName: String): String {
        try {
            // Try to find existing subfolder
            val query = "name = '$subfolderName' and mimeType = 'application/vnd.google-apps.folder' and '$parentFolderId' in parents and trashed = false"
            val request = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
            
            val fileList: FileList = request.execute()
            val files = fileList.files
            
            if (files != null && files.isNotEmpty()) {
                return files[0].id
            }
            
            // Create subfolder if not found
            val fileMetadata = File().apply {
                name = subfolderName
                mimeType = "application/vnd.google-apps.folder"
                parents = Collections.singletonList(parentFolderId)
            }
            
            val folder = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
            
            return folder.id
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            Log.e("GoogleDriveRepository", "Failed to get/create subfolder: HTTP ${e.statusCode}", e)
            throw Exception("Failed to get/create subfolder: HTTP ${e.statusCode}")
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to get/create subfolder: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Uploads a file to Google Drive (generic method for any file type).
     */
    private fun uploadFileToDrive(
        driveService: Drive,
        file: JavaFile,
        folderId: String,
        fileName: String,
        mimeType: String
    ): String {
        try {
            val fileMetadata = File().apply {
                name = fileName
                parents = Collections.singletonList(folderId)
            }
            
            val mediaContent = FileContent(mimeType, file)
            
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            
            val fileId = uploadedFile.id
            // ✅ FIX: Generate direct download URL instead of web view URL
            val fileUrl = "https://drive.google.com/uc?export=download&id=$fileId"
            
            Log.d("GoogleDriveRepository", "✅ File uploaded: $fileName (ID: $fileId, direct download URL)")
            return fileUrl
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            Log.e("GoogleDriveRepository", "Failed to upload file: HTTP ${e.statusCode}", e)
            throw Exception("Failed to upload file to Drive (HTTP ${e.statusCode})")
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to upload file: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Finds a file in Google Drive by name in a specific folder.
     * 
     * @param driveService Drive service instance
     * @param folderId The ID of the folder to search in
     * @param fileName The name of the file to find
     * @return The file ID if found, null otherwise
     */
    private fun findFileInFolder(driveService: Drive, folderId: String, fileName: String): String? {
        return try {
            val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
            val request = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
            
            val fileList: FileList = request.execute()
            val files = fileList.files
            
            if (files != null && files.isNotEmpty()) {
                files[0].id
            } else {
                null
            }
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            Log.e("GoogleDriveRepository", "Failed to find file: HTTP ${e.statusCode}", e)
            null
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to find file: ${e.message}", e)
            null
        }
    }
    
    /**
     * Downloads a file from Google Drive by file ID.
     * 
     * @param driveService Drive service instance
     * @param fileId The Google Drive file ID
     * @param destinationFile The local file where to save the downloaded content
     */
    private fun downloadFileFromDrive(driveService: Drive, fileId: String, destinationFile: JavaFile): Result<Unit> {
        return try {
            // ✅ FIX: Check file size first to handle empty files gracefully
            val fileMetadata = driveService.files().get(fileId)
                .setFields("id,size")
                .execute()
            
            val fileSize = fileMetadata.size?.toLong() ?: 0L
            Log.d("GoogleDriveRepository", "File size: $fileSize bytes")
            
            // If file is empty (0 bytes), create empty file and return success
            if (fileSize == 0L) {
                Log.d("GoogleDriveRepository", "File is empty (0 bytes) - creating empty destination file")
                destinationFile.parentFile?.mkdirs()
                destinationFile.createNewFile()
                return Result.success(Unit)
            }
            
            // Ensure destination directory exists
            destinationFile.parentFile?.mkdirs()
            
            // Download file content
            val outputStream = FileOutputStream(destinationFile)
            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            
            Log.d("GoogleDriveRepository", "✅ File downloaded successfully: ${destinationFile.length()} bytes")
            Result.success(Unit)
            
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            } else if (e.statusCode == 416) {
                // ✅ FIX: Handle 416 "Requested range not satisfiable" for empty files
                Log.w("GoogleDriveRepository", "⚠️ 416 error (empty file or range issue) - treating as empty file")
                destinationFile.parentFile?.mkdirs()
                destinationFile.createNewFile()
                Result.success(Unit)
            } else {
                Log.e("GoogleDriveRepository", "Failed to download file: HTTP ${e.statusCode}", e)
                Result.failure(Exception("Failed to download file from Drive (HTTP ${e.statusCode})"))
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to download file: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Checks if a file exists in Google Drive.
     * 
     * @param folderPath The folder path (e.g., "HotWheelsCollectors/database")
     * @param fileName The file name to check
     * @return Result containing true if file exists, false otherwise, or error if check failed
     */
    suspend fun checkFileExists(folderPath: String, fileName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            
            // Get parent folder (HotWheelsCollectors)
            val parentFolderId = getOrCreateFolder(driveService)
            
            // Handle nested folder paths (e.g., "HotWheelsCollectors/photos/userId/carId")
            val pathParts = folderPath.split("/").filter { it.isNotEmpty() && it != folderName }
            var currentFolderId = parentFolderId
            
            // Navigate/create each subfolder in the path
            for (subfolderName in pathParts) {
                currentFolderId = getOrCreateSubfolder(driveService, currentFolderId, subfolderName)
            }
            
            val subfolderId = currentFolderId
            
            // Find file in subfolder
            val fileId = findFileInFolder(driveService, subfolderId, fileName)
            
            Result.success(fileId != null)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to check file existence: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Downloads a file from Google Drive.
     * 
     * @param folderPath The folder path (e.g., "HotWheelsCollectors/database")
     * @param fileName The file name to download
     * @param destinationFile The local file where to save the downloaded content
     * @return Result indicating success or failure
     */
    suspend fun downloadFile(folderPath: String, fileName: String, destinationFile: JavaFile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "Downloading file from Google Drive: $fileName")
            Log.d("GoogleDriveRepository", "Folder path: $folderPath")
            Log.d("GoogleDriveRepository", "Destination: ${destinationFile.absolutePath}")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            
            // Get parent folder (HotWheelsCollectors)
            val parentFolderId = getOrCreateFolder(driveService)
            
            // Handle nested folder paths (e.g., "HotWheelsCollectors/photos/userId/carId")
            val pathParts = folderPath.split("/").filter { it.isNotEmpty() && it != folderName }
            var currentFolderId = parentFolderId
            
            // Navigate/create each subfolder in the path
            for (subfolderName in pathParts) {
                currentFolderId = getOrCreateSubfolder(driveService, currentFolderId, subfolderName)
            }
            
            val subfolderId = currentFolderId
            
            // Find file in subfolder
            val fileId = findFileInFolder(driveService, subfolderId, fileName)
                ?: return@withContext Result.failure(IllegalArgumentException("File not found: $fileName"))
            
            // Download file
            val downloadResult = downloadFileFromDrive(driveService, fileId, destinationFile)
            
            if (downloadResult.isSuccess) {
                Log.d("GoogleDriveRepository", "✅ File downloaded successfully: ${destinationFile.absolutePath}")
            }
            
            downloadResult
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ File download failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Downloads a file from Google Drive by fileId.
     * Used by restore process to download photos programmatically.
     * 
     * @param fileId The Google Drive file ID
     * @param destinationFile The local file where to save the downloaded content
     * @return Result indicating success or failure
     */
    suspend fun downloadFileByFileId(fileId: String, destinationFile: JavaFile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "Downloading file from Drive by fileId: $fileId")
            Log.d("GoogleDriveRepository", "Destination: ${destinationFile.absolutePath}")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            
            // Ensure destination directory exists
            destinationFile.parentFile?.mkdirs()
            
            // Download file
            val result = downloadFileFromDrive(driveService, fileId, destinationFile)
            
            if (result.isSuccess && destinationFile.exists() && destinationFile.length() > 0) {
                Log.i("GoogleDriveRepository", "✅ File downloaded successfully: ${destinationFile.absolutePath} (${destinationFile.length()} bytes)")
            } else {
                Log.e("GoogleDriveRepository", "❌ File download failed or file is empty")
            }
            
            result
        } catch (e: ReAuthRequiredException) {
            Log.e("GoogleDriveRepository", "❌ Re-authentication required: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to download file by fileId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Migrates a car from local storage to Google Drive.
     * Uploads photos and updates CarEntity and PhotoEntity with Drive URLs.
     * 
     * @param car CarEntity with local photo paths
     * @return Result with updated CarEntity on success, or error on failure
     */
    /**
     * Clears the cached folder ID. Call this after migration is complete.
     */
    /**
     * Clears the cached folder ID. Call this after migration is complete.
     */
    fun clearFolderCache() {
        driveRootFolderManager.clearCache()
    }
    
    /**
     * Clears ALL Drive caches (folder + db.json file ID).
     * Call this after migration, restore, or sign out to prevent stale cache.
     */
    fun clearDriveCache() {
        cachedDbJsonFileId = null
        driveRootFolderManager.clearCache()
        Log.d("GoogleDriveRepository", "Drive cache cleared (folder + db.json)")
    }
    
    suspend fun migrateCarToDrive(car: CarEntity): Result<CarEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== MIGRATING CAR TO DRIVE ===")
            Log.d("GoogleDriveRepository", "Car ID: ${car.id}, Model: ${car.model}")
            
            // Check if car is already on Drive (has Drive URL)
            if (car.frontPhotoPath.contains("drive.google.com") || car.combinedPhotoPath.contains("drive.google.com")) {
                Log.d("GoogleDriveRepository", "⚠️ Car already on Drive - skipping migration")
                return@withContext Result.success(car)
            }
            
            // Check Google Sign-In
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            // Get Drive service
            val driveService = getDriveService(account)
            
            // ✅ FIX: Use cached folder ID (getOrCreateFolder now uses cache internally)
            val folderId = getOrCreateFolder(driveService)
            
            // Upload photos to Drive
            val thumbnailFile = if (car.combinedPhotoPath.isNotEmpty() && !car.combinedPhotoPath.contains("http")) {
                JavaFile(car.combinedPhotoPath)
            } else null
            
            val fullFile = if (car.frontPhotoPath.isNotEmpty() && !car.frontPhotoPath.contains("http")) {
                JavaFile(car.frontPhotoPath)
            } else null
            
            var thumbnailInfo: DriveFileInfo? = null
            var fullInfo: DriveFileInfo? = null
            
            if (thumbnailFile != null && thumbnailFile.exists()) {
                thumbnailInfo = uploadPhotoToDrive(driveService, thumbnailFile.absolutePath, folderId, "thumbnail_${car.id}.jpg")
                Log.d("GoogleDriveRepository", "✅ Thumbnail uploaded: ${thumbnailInfo.fileUrl}")
            }
            
            if (fullFile != null && fullFile.exists()) {
                fullInfo = uploadPhotoToDrive(driveService, fullFile.absolutePath, folderId, "full_${car.id}.jpg")
                Log.d("GoogleDriveRepository", "✅ Full photo uploaded: ${fullInfo.fileUrl}")
            }
            
            // ✅ FIX: Update CarEntity with Drive URLs ONLY if upload was successful
            // Ensure we have at least one photo uploaded before updating
            if (thumbnailInfo == null && fullInfo == null) {
                Log.w("GoogleDriveRepository", "⚠️ No photos uploaded - skipping car update")
                return@withContext Result.failure(IllegalStateException("No photos were uploaded to Drive"))
            }
            
            // ✅ FIX: Update ALL CarEntity fields with Drive URLs (not just photo paths)
            val updatedCar = car.copy(
                frontPhotoPath = fullInfo?.fileUrl ?: car.frontPhotoPath,
                combinedPhotoPath = thumbnailInfo?.fileUrl ?: car.combinedPhotoPath,
                photoUrl = fullInfo?.fileUrl ?: thumbnailInfo?.fileUrl ?: car.photoUrl,
                syncStatus = SyncStatus.SYNCED
            )
            
            // ✅ FIX: Use insert with REPLACE strategy to ensure ALL fields are updated correctly
            // This is more reliable than updateCar which might not update all fields
            carDao.insertCar(updatedCar)
            Log.d("GoogleDriveRepository", "✅ CarEntity updated with Drive URLs:")
            Log.d("GoogleDriveRepository", "   - frontPhotoPath: ${updatedCar.frontPhotoPath}")
            Log.d("GoogleDriveRepository", "   - combinedPhotoPath: ${updatedCar.combinedPhotoPath}")
            Log.d("GoogleDriveRepository", "   - photoUrl: ${updatedCar.photoUrl}")
            Log.d("GoogleDriveRepository", "   - Car ID: ${updatedCar.id}")
            Log.d("GoogleDriveRepository", "   - Car model: ${updatedCar.model}")
            Log.d("GoogleDriveRepository", "   - Car brand: ${updatedCar.brand}")
            
            // Update PhotoEntity if exists
            val photos = photoDao.getPhotosForCar(car.id).first()
            if (photos.isNotEmpty()) {
                photos.forEach { photo ->
                    val updatedPhoto = photo.copy(
                        thumbnailPath = thumbnailInfo?.fileUrl ?: photo.thumbnailPath,
                        fullSizePath = fullInfo?.fileUrl ?: photo.fullSizePath,
                        cloudPath = fullInfo?.fileUrl ?: thumbnailInfo?.fileUrl ?: photo.cloudPath,
                        driveFileId = fullInfo?.fileId ?: photo.driveFileId,
                        driveThumbnailFileId = thumbnailInfo?.fileId ?: photo.driveThumbnailFileId,
                        syncStatus = SyncStatus.SYNCED
                    )
                    photoDao.updatePhoto(updatedPhoto)
                    Log.d("GoogleDriveRepository", "✅ PhotoEntity ${photo.id} updated with Drive URLs")
                }
                Log.d("GoogleDriveRepository", "✅ All PhotoEntity records updated with Drive URLs")
            } else {
                Log.d("GoogleDriveRepository", "ℹ️ No PhotoEntity records found for car ${car.id}")
            }
            
            Log.i("GoogleDriveRepository", "✅ Car migration to Drive completed successfully")
            Result.success(updatedCar)
            
        } catch (e: ReAuthRequiredException) {
            Log.e("GoogleDriveRepository", "❌ Re-authentication required: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Car migration failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ============================================================================
    // ✅ PASUL 1: DB.JSON MANAGEMENT (Drive Primary Storage)
    // ============================================================================
    
    /**
     * Finds or creates the db.json file in Drive root folder.
     * Returns the file ID for use in read/write operations.
     */
    private suspend fun findOrCreateDbJson(driveService: Drive, folderId: String): String? {
        try {
            // Check cache first
            if (cachedDbJsonFileId != null) {
                Log.d("GoogleDriveRepository", "✅ Using cached db.json file ID: $cachedDbJsonFileId")
                return cachedDbJsonFileId
            }
            
            // Try to find existing db.json
            val fileId = findFileInFolder(driveService, folderId, dbJsonFileName)
            
            if (fileId != null) {
                cachedDbJsonFileId = fileId
                Log.d("GoogleDriveRepository", "✅ Found existing db.json with ID: $fileId")
                return fileId
            }
            
            // ✅ FIX: Don't create empty file - let saveDbJson create it with content
            // This prevents 416 errors from empty files
            Log.d("GoogleDriveRepository", "db.json doesn't exist yet - will be created by saveDbJson with content")
            return null
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to find/create db.json: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Loads db.json from Drive and returns DatabaseExport.
     * Returns null if file doesn't exist or is empty.
     */
    suspend fun loadDbJson(): Result<DatabaseExport?> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== LOADING DB.JSON FROM DRIVE ===")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            
            val dbJsonFileId = findOrCreateDbJson(driveService, folderId)
                ?: return@withContext Result.success(null) // File doesn't exist yet
            
            // Download db.json to temp file
            val tempFile = JavaFile(context.cacheDir, "db.json.tmp")
            val downloadResult = downloadFileFromDrive(driveService, dbJsonFileId, tempFile)
            
            if (downloadResult.isFailure) {
                Log.w("GoogleDriveRepository", "⚠️ Failed to download db.json: ${downloadResult.exceptionOrNull()?.message}")
                return@withContext Result.success(null) // Treat as empty
            }
            
            // Check if file is empty
            if (tempFile.length() == 0L) {
                Log.d("GoogleDriveRepository", "ℹ️ db.json is empty")
                tempFile.delete()
                return@withContext Result.success(null)
            }
            
            // Parse JSON
            val exportData: DatabaseExport = FileReader(tempFile).use { reader ->
                gson.fromJson(reader, DatabaseExport::class.java)
            }
            
            tempFile.delete()
            
            Log.i("GoogleDriveRepository", "✅ Loaded db.json: ${exportData.tables.cars.size} cars, ${exportData.tables.photos.size} photos")
            Result.success(exportData)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to load db.json: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Saves DatabaseExport as db.json in Drive.
     * Creates or updates the file.
     */
    suspend fun saveDbJson(exportData: DatabaseExport): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== SAVING DB.JSON TO DRIVE ===")
            Log.d("GoogleDriveRepository", "Cars: ${exportData.tables.cars.size}, Photos: ${exportData.tables.photos.size}")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            
            // Serialize to JSON
            val tempFile = JavaFile(context.cacheDir, "db.json.tmp")
            FileWriter(tempFile).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            Log.d("GoogleDriveRepository", "Serialized db.json: ${tempFile.length()} bytes")
            
            // Find or create db.json file
            var dbJsonFileId = findOrCreateDbJson(driveService, folderId)
            
            val fileMetadata = File().apply {
                name = dbJsonFileName
            }
            
            val mediaContent = FileContent("application/json", tempFile)
            
            // ✅ FIX: Create file if it doesn't exist, update if it does
            val resultFile = if (dbJsonFileId == null) {
                // File doesn't exist - create it with content
                Log.d("GoogleDriveRepository", "Creating new db.json file with content...")
                fileMetadata.parents = Collections.singletonList(folderId)
                val createdFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                dbJsonFileId = createdFile.id
                cachedDbJsonFileId = dbJsonFileId
                Log.i("GoogleDriveRepository", "✅ Created db.json with content (ID: ${createdFile.id})")
                createdFile
            } else {
                // File exists - update it
                Log.d("GoogleDriveRepository", "Updating existing db.json (ID: $dbJsonFileId)...")
                val updatedFile = driveService.files().update(dbJsonFileId, fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                Log.i("GoogleDriveRepository", "✅ Updated db.json (ID: ${updatedFile.id})")
                updatedFile
            }
            
            tempFile.delete()
            
            Log.i("GoogleDriveRepository", "✅ Saved db.json successfully (ID: ${resultFile.id})")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to save db.json: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Updates db.json with a new car.
     * Loads existing db.json, adds the car, and saves back.
     */
    suspend fun updateDbJsonWithCar(car: CarEntity, photos: List<PhotoEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== UPDATING DB.JSON WITH NEW CAR ===")
            Log.d("GoogleDriveRepository", "Car ID: ${car.id}, Model: ${car.model}")
            
            // Load existing db.json
            val loadResult = loadDbJson()
            if (loadResult.isFailure) {
                return@withContext Result.failure(loadResult.exceptionOrNull() ?: Exception("Failed to load db.json"))
            }
            
            val existingExport = loadResult.getOrNull() ?: run {
                // Create new export if db.json doesn't exist
                Log.d("GoogleDriveRepository", "db.json doesn't exist, creating new one...")
                DatabaseExport(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    userId = car.userId,
                    tables = DatabaseTables(
                        users = emptyList(),
                        cars = emptyList(),
                        photos = emptyList(),
                        priceHistory = emptyList(),
                        tradeOffers = emptyList(),
                        wishlist = emptyList(),
                        searchHistory = emptyList(),
                        searchKeywords = emptyList()
                    )
                )
            }
            
            // Update cars list (replace if exists, add if new)
            val updatedCars = existingExport.tables.cars.toMutableList()
            val existingCarIndex = updatedCars.indexOfFirst { it.id == car.id }
            if (existingCarIndex >= 0) {
                updatedCars[existingCarIndex] = car
                Log.d("GoogleDriveRepository", "Replaced existing car in db.json")
            } else {
                updatedCars.add(car)
                Log.d("GoogleDriveRepository", "Added new car to db.json")
            }
            
            // Update photos list
            val updatedPhotos = existingExport.tables.photos.toMutableList()
            photos.forEach { photo ->
                val existingPhotoIndex = updatedPhotos.indexOfFirst { it.id == photo.id }
                if (existingPhotoIndex >= 0) {
                    updatedPhotos[existingPhotoIndex] = photo
                } else {
                    updatedPhotos.add(photo)
                }
            }
            
            // Create updated export
            val updatedExport = existingExport.copy(
                timestamp = System.currentTimeMillis(),
                tables = existingExport.tables.copy(
                    cars = updatedCars,
                    photos = updatedPhotos
                )
            )
            
            // Save back to Drive
            val saveResult = saveDbJson(updatedExport)
            if (saveResult.isFailure) {
                return@withContext Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save db.json"))
            }
            
            Log.i("GoogleDriveRepository", "✅ Updated db.json with new car successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to update db.json: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Drive PRIMARY write-back for car edits.
     *
     * When Drive is PRIMARY, Room is only a cache. On app start, `StorageOrchestrator` calls
     * `syncRoomFromDrive()` which overwrites Room from Drive's `db.json`.
     *
     * If we don't update `db.json` after user edits (year/color/model/brand/notes/price etc.),
     * the next app start will restore old values and changes will look "reset".
     *
     * This function UPSERTs the given car into `db.json` (cars table only).
     */
    suspend fun upsertCarInDbJsonIfDrivePrimary(car: CarEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val storageType = userPreferences.storageType.first()
            if (storageType != PersonalStorageType.GOOGLE_DRIVE) {
                return@withContext Result.success(Unit)
            }

            Log.d("GoogleDriveRepository", "=== DRIVE PRIMARY: UPSERT CAR INTO DB.JSON ===")
            Log.d("GoogleDriveRepository", "Car ID: ${car.id}, userId: ${car.userId}, model: ${car.model}")

            val load = loadDbJson()
            if (load.isFailure) {
                return@withContext Result.failure(load.exceptionOrNull() ?: Exception("Failed to load db.json"))
            }

            val existingExport = load.getOrNull()
            val exportToSave = if (existingExport == null) {
                // db.json missing: safest fallback is exporting the full local DB and saving it
                Log.w("GoogleDriveRepository", "db.json missing - exporting local DB and saving to Drive")
                val exportResult = exportLocalDatabase()
                if (exportResult.isFailure) {
                    return@withContext Result.failure(exportResult.exceptionOrNull() ?: Exception("Failed to export local DB"))
                }
                exportResult.getOrNull()!!
            } else {
                val updatedCars = existingExport.tables.cars.toMutableList()
                val idx = updatedCars.indexOfFirst { it.id == car.id }
                if (idx >= 0) {
                    updatedCars[idx] = car
                    Log.d("GoogleDriveRepository", "Updated existing car in db.json")
                } else {
                    updatedCars.add(car)
                    Log.d("GoogleDriveRepository", "Car not found in db.json - added it")
                }

                existingExport.copy(
                    timestamp = System.currentTimeMillis(),
                    tables = existingExport.tables.copy(
                        cars = updatedCars
                    )
                )
            }

            val saved = saveDbJson(exportToSave)
            if (saved.isFailure) {
                return@withContext Result.failure(saved.exceptionOrNull() ?: Exception("Failed to save db.json"))
            }

            Log.i("GoogleDriveRepository", "✅ Drive PRIMARY: db.json updated for car=${car.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Drive PRIMARY: failed to upsert car into db.json: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clears the cached db.json file ID.
     */
    fun clearDbJsonCache() {
        cachedDbJsonFileId = null
        Log.d("GoogleDriveRepository", "✅ db.json cache cleared")
    }
    
    // ============================================================================
    // ✅ PASUL 3: SYNC ROOM FROM DRIVE (When Drive is Primary)
    // ============================================================================
    
    /**
     * Synchronizes Room Database from Drive db.json when Drive is primary storage.
     * This populates Room as a cache for UI performance.
     * 
     * Flow:
     * 1. Load db.json from Drive
     * 2. Clear existing Room data (if needed)
     * 3. Populate Room with data from db.json
     */
    suspend fun syncRoomFromDrive(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== SYNCING ROOM FROM DRIVE DB.JSON ===")
            
            // Check if Drive is primary
            val storageType = userPreferences.storageType.first()
            if (storageType != PersonalStorageType.GOOGLE_DRIVE) {
                Log.d("GoogleDriveRepository", "Drive is not primary storage ($storageType) - skipping sync")
                return@withContext Result.success(Unit)
            }
            
            Log.d("GoogleDriveRepository", "Drive is primary - loading db.json...")
            
            // Load db.json from Drive
            val loadResult = loadDbJson()
            if (loadResult.isFailure) {
                Log.e("GoogleDriveRepository", "❌ Failed to load db.json: ${loadResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(loadResult.exceptionOrNull() ?: Exception("Failed to load db.json"))
            }
            
            val exportData = loadResult.getOrNull()
            if (exportData == null) {
                Log.d("GoogleDriveRepository", "db.json doesn't exist or is empty - nothing to sync")
                return@withContext Result.success(Unit)
            }
            
            Log.d("GoogleDriveRepository", "Loaded db.json: ${exportData.tables.cars.size} cars, ${exportData.tables.photos.size} photos")
            
            // Get current user ID
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            // âœ… IMPORTANT: Treat Drive folder as per-user. Normalize ALL records to current user.
            // This prevents "0 cars" after restore/migration when older data had mismatched userId.
            val userCarsFromDrive = exportData.tables.cars.map { it.copy(userId = currentUserId) }
            val carIds = userCarsFromDrive.map { it.id }.toSet()
            val userPhotosFromDrive = exportData.tables.photos.filter { it.carId in carIds }
            val userPriceHistoryFromDrive = exportData.tables.priceHistory.filter { it.carId in carIds }
            val userWishlistFromDrive = exportData.tables.wishlist.map { it.copy(userId = currentUserId) }
            val userSearchHistoryFromDrive = exportData.tables.searchHistory.map { it.copy(userId = currentUserId) }
            // SearchKeywords are linked to cars, filter by carIds
            val userSearchKeywordsFromDrive = exportData.tables.searchKeywords.filter { it.carId in carIds }
            
            Log.d("GoogleDriveRepository", "Filtered data for current user: ${userCarsFromDrive.size} cars, ${userPhotosFromDrive.size} photos, ${userPriceHistoryFromDrive.size} price history, ${userWishlistFromDrive.size} wishlist, ${userSearchHistoryFromDrive.size} search history, ${userSearchKeywordsFromDrive.size} search keywords")
            
            // ✅ PASUL 3: Populate Room from db.json (Room becomes cache)
            // Collect Flow data BEFORE transaction (Room doesn't allow Flow in transaction)
            val existingUserCarsBeforeTransaction = carDao.getCarsForUser(currentUserId).first()
            val existingPhotosMap = mutableMapOf<String, List<PhotoEntity>>()
            existingUserCarsBeforeTransaction.forEach { car ->
                val photos = photoDao.getPhotosForCar(car.id).first()
                existingPhotosMap[car.id] = photos
            }
            
            appDatabase.withTransaction {
                // Clear existing data for current user (to avoid duplicates)
                Log.d("GoogleDriveRepository", "Clearing existing Room data for user: $currentUserId")
                
                // Delete all photos for user's cars first (to avoid foreign key constraints)
                existingUserCarsBeforeTransaction.forEach { car ->
                    existingPhotosMap[car.id]?.forEach { photo ->
                        photoDao.hardDeletePhoto(photo.id)
                    }
                }
                
                // Delete all cars for current user
                carDao.deleteAllCarsForUser(currentUserId)
                

                // âœ… Ensure UserEntity exists BEFORE inserting cars (cars.userId has FK -> users.id)
                // On fresh reinstall, Room is empty and insertCars would fail with SQLITE_CONSTRAINT_FOREIGNKEY.
                val ensureUserDao = appDatabase.userDao()
                val ensuredUser = ensureUserDao.getById(currentUserId)
                if (ensuredUser == null) {
                    ensureUserDao.insert(
                        UserEntity(
                            id = currentUserId,
                            email = "",
                            name = "",
                            createdAt = Date(),
                            lastLoginAt = Date()
                        )
                    )
                    Log.i("GoogleDriveRepository", "âœ… Ensured user exists in Room before inserting cars: $currentUserId")
                }                // Insert cars
                if (userCarsFromDrive.isNotEmpty()) {
                    carDao.insertCars(userCarsFromDrive)
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userCarsFromDrive.size} cars into Room (from db.json)")
                }
                
                // Insert photos
                if (userPhotosFromDrive.isNotEmpty()) {
                    photoDao.insertPhotos(userPhotosFromDrive)
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userPhotosFromDrive.size} photos into Room (from db.json)")
                }
                // IMPORTANT:
                // Do NOT insert arbitrary users from db.json here.
                //
                // Reason: UserEntity has a UNIQUE index on email, and cars.userId has FK -> users.id (CASCADE).
                // If we insert a user from db.json with the same email (often empty string) but a different id,
                // SQLite may REPLACE the existing ensured user row -> deletes it -> CASCADE deletes all cars.
                //
                // We already ensured the current user exists (by id) before inserting cars, which is sufficient.
                Log.d("GoogleDriveRepository", "Skipping users import from db.json (using ensured current user only)")

                // Insert price history
                if (userPriceHistoryFromDrive.isNotEmpty()) {
                    appDatabase.priceHistoryDao().insertPriceRecords(userPriceHistoryFromDrive)
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userPriceHistoryFromDrive.size} price history records into Room (from db.json)")
                }
                
                // Insert wishlist
                if (userWishlistFromDrive.isNotEmpty()) {
                    userWishlistFromDrive.forEach { item ->
                        appDatabase.wishlistDao().insertWishlistItem(item)
                    }
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userWishlistFromDrive.size} wishlist items into Room (from db.json)")
                }
                
                // Insert search history
                if (userSearchHistoryFromDrive.isNotEmpty()) {
                    userSearchHistoryFromDrive.forEach { search ->
                        appDatabase.searchHistoryDao().insertSearch(search)
                    }
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userSearchHistoryFromDrive.size} search history records into Room (from db.json)")
                }
                
                // Insert search keywords
                if (userSearchKeywordsFromDrive.isNotEmpty()) {
                    appDatabase.searchKeywordDao().insertKeywords(userSearchKeywordsFromDrive)
                    Log.i("GoogleDriveRepository", "✅ Inserted ${userSearchKeywordsFromDrive.size} search keywords into Room (from db.json)")
                }
            }
            
            Log.i("GoogleDriveRepository", "✅ Room synced from Drive db.json successfully")
            Log.i("GoogleDriveRepository", "   - Cars: ${userCarsFromDrive.size}")
            Log.i("GoogleDriveRepository", "   - Photos: ${userPhotosFromDrive.size}")
            Log.i("GoogleDriveRepository", "   - Price History: ${userPriceHistoryFromDrive.size}")
            Log.i("GoogleDriveRepository", "   - Wishlist: ${userWishlistFromDrive.size}")
            Log.i("GoogleDriveRepository", "   - Search History: ${userSearchHistoryFromDrive.size}")
            Log.i("GoogleDriveRepository", "   - Search Keywords: ${userSearchKeywordsFromDrive.size}")
            // âœ… RELINK LOCAL PHOTO PATHS (keep My Collection photos after switching to Drive)
            // If local files exist (from previous Local storage), prefer them for UI (offline + instant).
            try {
                val photosRoot = java.io.File(context.filesDir, "photos/$currentUserId")
                if (photosRoot.exists()) {
                    for (car in userCarsFromDrive) {
                        val carDir = java.io.File(photosRoot, car.id)
                        val localThumb = java.io.File(carDir, "thumbnail.jpg")
                        val localFull = java.io.File(carDir, "full.jpg")
                        if (localThumb.exists() && localFull.exists()) {
                            val updatedCar = car.copy(
                                photoUrl = localFull.absolutePath,
                                frontPhotoPath = localFull.absolutePath,
                                combinedPhotoPath = localThumb.absolutePath
                            )
                            carDao.insertCar(updatedCar)

                            val carPhotos = photoDao.getPhotosForCar(car.id).first()
                            for (p in carPhotos) {
                                val updatedPhoto = p.copy(
                                    localPath = localThumb.absolutePath,
                                    thumbnailPath = localThumb.absolutePath,
                                    fullSizePath = localFull.absolutePath
                                )
                                photoDao.updatePhoto(updatedPhoto)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleDriveRepository", "âš ï¸ Failed to relink local photo paths: ${e.message}")
            }
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to sync Room from Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ============================================================================
    // ✅ MIGRATION METHODS - REAL MIGRATION LOGIC
    // ============================================================================
    
    /**
     * Exports all local database data to DatabaseExport format.
     * Used for REAL migration from Local to Drive.
     */
    suspend fun exportLocalDatabase(): Result<DatabaseExport> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== EXPORTING LOCAL DATABASE ===")
            
            // Get current user ID
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            // Export all data
            val users = listOf(appDatabase.userDao().getById(currentUserId)).filterNotNull()
            val cars = carDao.getAllCars().first()
            val photos = photoDao.getAllPhotos().first()
            
            // Export other entities (priceHistory, wishlist, searchHistory, searchKeywords are active)
            val allPriceHistory = mutableListOf<PriceHistoryEntity>()
            cars.forEach { car ->
                val priceHistory = appDatabase.priceHistoryDao().getPriceHistoryForCar(car.id).first()
                allPriceHistory.addAll(priceHistory)
            }
            val priceHistory = allPriceHistory
            
            val wishlist = appDatabase.wishlistDao().getAllWishlistItems(currentUserId).first()
            val searchHistory = appDatabase.searchHistoryDao().getRecentSearches(currentUserId, limit = 10000).first()
            val searchKeywords = appDatabase.searchKeywordDao().getAllKeywords().first()
            
            // TradeOffers - not yet used in UI, keep as emptyList for now
            val tradeOffers = emptyList<TradeOfferEntity>()
            
            val exportData = DatabaseExport(
                version = 1,
                timestamp = System.currentTimeMillis(),
                userId = currentUserId,
                tables = DatabaseTables(
                    users = users,
                    cars = cars,
                    photos = photos,
                    priceHistory = priceHistory,
                    tradeOffers = tradeOffers,
                    wishlist = wishlist,
                    searchHistory = searchHistory,
                    searchKeywords = searchKeywords
                )
            )
            
            Log.i("GoogleDriveRepository", "✅ Local database exported: ${cars.size} cars, ${photos.size} photos, ${priceHistory.size} price history, ${wishlist.size} wishlist, ${searchHistory.size} search history, ${searchKeywords.size} search keywords")
            Result.success(exportData)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to export local database: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clears all Room data for current user.
     * Used when migrating to Drive (Room becomes cache).
     */
    suspend fun clearRoomForUser(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== CLEARING ROOM FOR USER ===")
            
            // Get current user ID
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            appDatabase.withTransaction {
                // Delete all photos for user's cars first (to avoid foreign key constraints)
                val userCars = carDao.getCarsForUser(currentUserId).first()
                userCars.forEach { car ->
                    val photos = photoDao.getPhotosForCar(car.id).first()
                    photos.forEach { photo ->
                        photoDao.hardDeletePhoto(photo.id)
                    }
                }
                
                // Delete all cars for user
                carDao.deleteAllCarsForUser(currentUserId)
                
                // Delete user (optional - can keep if needed)
                // appDatabase.userDao().deleteUser(currentUserId)
            }
            
            Log.i("GoogleDriveRepository", "✅ Room cleared for user: $currentUserId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to clear Room: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Uploads all local photos from DatabaseExport to Google Drive.
     * Used during migration from Local to Drive.
     */
    /**
     * Uploads ALL local photos to Drive and returns updated DatabaseExport with Drive URLs/IDs.
     * 
     * ✅ CRITICAL: This function MUST update the export data and return it, not just upload photos.
     * This is essential for migration Local → Drive where db.json must contain only Drive metadata.
     * 
     * @param exportData Original export data with local paths
     * @return Result containing updated DatabaseExport with Drive URLs/IDs
     */
    suspend fun uploadAllLocalPhotos(exportData: DatabaseExport): Result<DatabaseExport> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== UPLOADING ALL LOCAL PHOTOS TO DRIVE ===")
            
            // Check Google Sign-In
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            
            var uploadedCount = 0
            var failedCount = 0
            
            // ✅ Create mutable lists to update export data
            val updatedPhotos = exportData.tables.photos.toMutableList()
            val updatedCars = exportData.tables.cars.toMutableList()
            
            // Get all photos with local paths
            val localPhotos = exportData.tables.photos.filter { photo ->
                val hasLocalFull = photo.fullSizePath?.isNotEmpty() == true && 
                                  !photo.fullSizePath.contains("http") &&
                                  JavaFile(photo.fullSizePath).exists()
                val hasLocalThumb = photo.thumbnailPath?.isNotEmpty() == true && 
                                   !photo.thumbnailPath.contains("http") &&
                                   JavaFile(photo.thumbnailPath).exists()
                val hasLocalPath = photo.localPath.isNotEmpty() && 
                                  !photo.localPath.contains("http") &&
                                  JavaFile(photo.localPath).exists()
                
                hasLocalFull || hasLocalThumb || hasLocalPath
            }
            
            Log.d("GoogleDriveRepository", "Found ${localPhotos.size} local photos to upload")
            
            // Upload each photo (FULL + THUMBNAIL)
            localPhotos.forEach { photo ->
                try {
                    var fullInfo: DriveFileInfo? = null
                    var thumbnailInfo: DriveFileInfo? = null
                    
                    // Upload FULL photo
                    val fullPath = photo.fullSizePath?.takeIf { 
                        it.isNotEmpty() && !it.contains("http") && JavaFile(it).exists()
                    } ?: photo.localPath.takeIf {
                        it.isNotEmpty() && !it.contains("http") && JavaFile(it).exists()
                    }
                    
                    if (fullPath != null) {
                        fullInfo = uploadPhotoToDrive(
                            driveService,
                            fullPath,
                            folderId,
                            "full_${photo.id}.jpg"
                        )
                        Log.d("GoogleDriveRepository", "✅ Uploaded FULL photo: ${photo.id} → ${fullInfo.fileId}")
                    }
                    
                    // Upload THUMBNAIL
                    val thumbPath = photo.thumbnailPath?.takeIf {
                        it.isNotEmpty() && !it.contains("http") && JavaFile(it).exists()
                    } ?: photo.localPath.takeIf {
                        it.isNotEmpty() && !it.contains("http") && JavaFile(it).exists()
                    }
                    
                    if (thumbPath != null) {
                        thumbnailInfo = uploadPhotoToDrive(
                            driveService,
                            thumbPath,
                            folderId,
                            "thumb_${photo.id}.jpg"
                        )
                        Log.d("GoogleDriveRepository", "✅ Uploaded THUMBNAIL: ${photo.id} → ${thumbnailInfo.fileId}")
                    }
                    
                    if (fullInfo == null && thumbnailInfo == null) {
                        Log.w("GoogleDriveRepository", "⚠️ No valid local files found for photo: ${photo.id}")
                        failedCount++
                        return@forEach
                    }
                    
                    // ✅ Update PhotoEntity in export with Drive metadata
                    val photoIndex = updatedPhotos.indexOfFirst { it.id == photo.id }
                    if (photoIndex >= 0) {
                        updatedPhotos[photoIndex] = photo.copy(
                            cloudPath = fullInfo?.fileUrl ?: thumbnailInfo?.fileUrl ?: "",
                            driveFileId = fullInfo?.fileId,
                            driveThumbnailFileId = thumbnailInfo?.fileId,
                            thumbnailPath = thumbnailInfo?.fileUrl ?: "",
                            fullSizePath = fullInfo?.fileUrl ?: "",
                            localPath = "" // ✅ Clear local path (Drive is primary)
                        )
                    }
                    
                    // ✅ Update associated CarEntity with Drive URLs
                    val carIndex = updatedCars.indexOfFirst { it.id == photo.carId }
                    if (carIndex >= 0) {
                        val car = updatedCars[carIndex]
                        updatedCars[carIndex] = car.copy(
                            frontPhotoPath = fullInfo?.fileUrl ?: car.frontPhotoPath,
                            combinedPhotoPath = thumbnailInfo?.fileUrl ?: car.combinedPhotoPath,
                            photoUrl = fullInfo?.fileUrl ?: thumbnailInfo?.fileUrl ?: car.photoUrl
                        )
                    }
                    
                    uploadedCount++
                    Log.d("GoogleDriveRepository", "✅ Uploaded photo ${uploadedCount}/${localPhotos.size}: ${photo.id}")
                    
                } catch (e: Exception) {
                    failedCount++
                    Log.e("GoogleDriveRepository", "❌ Failed to upload photo ${photo.id}: ${e.message}", e)
                }
            }
            
            // ✅ Build updated export with Drive metadata
            val updatedExport = exportData.copy(
                tables = exportData.tables.copy(
                    photos = updatedPhotos,
                    cars = updatedCars
                )
            )
            
            Log.i("GoogleDriveRepository", "✅ Photo upload completed: $uploadedCount uploaded, $failedCount failed")
            Log.i("GoogleDriveRepository", "✅ Export data updated with Drive URLs/IDs")
            Result.success(updatedExport)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to upload local photos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Saves DatabaseExport data to Room.
     * Used when migrating from Drive to Local (Room becomes truth).
     */
    suspend fun saveToRoom(exportData: DatabaseExport): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== SAVING TO ROOM (ROOM BECOMES TRUTH) ===")
            
            // Get current user ID
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))
            
            // Filter data for current user
            val userCars = exportData.tables.cars.filter { it.userId == currentUserId }
            val carIds = userCars.map { it.id }.toSet()
            val userPhotos = exportData.tables.photos.filter { it.carId in carIds }
            val userUsers = exportData.tables.users.filter { it.id == currentUserId }
            
            Log.d("GoogleDriveRepository", "Saving to Room: ${userCars.size} cars, ${userPhotos.size} photos")
            
            appDatabase.withTransaction {
                // Insert users
                if (userUsers.isNotEmpty()) {
                    val userDao = appDatabase.userDao()
                    userUsers.forEach { user ->
                        userDao.insert(user)
                    }
                } else {
                    // Create user if doesn't exist
                    val userDao = appDatabase.userDao()
                    val existingUser = userDao.getById(currentUserId)
                    if (existingUser == null) {
                        val newUser = UserEntity(
                            id = currentUserId,
                            email = "",
                            name = "",
                            createdAt = Date(),
                            lastLoginAt = Date()
                        )
                        userDao.insert(newUser)
                    }
                }
                
                // Insert cars
                if (userCars.isNotEmpty()) {
                    carDao.insertCars(userCars)
                }
                
                // Insert photos
                if (userPhotos.isNotEmpty()) {
                    photoDao.insertPhotos(userPhotos)
                }
            }
            
            Log.i("GoogleDriveRepository", "✅ Data saved to Room successfully (Room is now truth)")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "❌ Failed to save to Room: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Downloads all photos from Drive to local storage.
     * Used when migrating from Drive to Local.
     */
    suspend fun downloadAllPhotosLocally(exportData: DatabaseExport): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveRepository", "=== DOWNLOADING ALL PHOTOS LOCALLY ===")

            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return@withContext Result.failure(IllegalStateException("User not authenticated"))

            // Check Google Sign-In
            GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))

            val drivePhotos = exportData.tables.photos.filter {
                it.driveFileId != null || it.driveThumbnailFileId != null || (it.cloudPath?.contains("drive.google.com") == true)
            }

            Log.d("GoogleDriveRepository", "Found ${drivePhotos.size} Drive photos to download")

            var downloadedCount = 0
            var failedCount = 0

            // âœ… LocalRepository layout
            val userRoot = JavaFile(context.filesDir, "photos/$currentUserId")
            userRoot.mkdirs()

            for (photo in drivePhotos) {
                try {
                    val carId = photo.carId
                    val carDir = JavaFile(userRoot, carId)
                    carDir.mkdirs()

                    val fullId = photo.driveFileId ?: photo.cloudPath?.let { extractDriveFileIdFromUrl(it) }
                    val thumbId = photo.driveThumbnailFileId ?: fullId

                    if (fullId.isNullOrEmpty()) {
                        Log.w("GoogleDriveRepository", "âš ï¸ Skip photo ${photo.id} - missing drive fileId")
                        failedCount++
                        continue
                    }

                    val localFull = JavaFile(carDir, "full.jpg")
                    val localThumb = JavaFile(carDir, "thumbnail.jpg")

                    // Download full only if needed
                    if (!localFull.exists() || localFull.length() == 0L) {
                        val r = downloadFileByFileId(fullId, localFull)
                        if (r.isFailure) {
                            failedCount++
                            Log.e("GoogleDriveRepository", "âŒ Failed to download full for car=$carId: ${r.exceptionOrNull()?.message}")
                            continue
                        }
                    }

                    // Download thumbnail (fallback to full)
                    if (!thumbId.isNullOrEmpty()) {
                        if (!localThumb.exists() || localThumb.length() == 0L) {
                            val r = downloadFileByFileId(thumbId, localThumb)
                            if (r.isFailure) {
                                Log.w("GoogleDriveRepository", "âš ï¸ Failed to download thumbnail for car=$carId, fallback to full")
                                try { localFull.copyTo(localThumb, overwrite = true) } catch (_: Exception) {}
                            }
                        }
                    } else {
                        if (!localThumb.exists()) {
                            try { localFull.copyTo(localThumb, overwrite = true) } catch (_: Exception) {}
                        }
                    }

                    // Update PhotoEntity in Room
                    val existingPhoto = photoDao.getPhotoById(photo.id)
                    if (existingPhoto != null) {
                        photoDao.updatePhoto(
                            existingPhoto.copy(
                                localPath = localThumb.absolutePath,
                                thumbnailPath = localThumb.absolutePath,
                                fullSizePath = localFull.absolutePath
                            )
                        )
                    }

                    // Update CarEntity in Room
                    val existingCar = carDao.getCarById(carId)
                    if (existingCar != null) {
                        carDao.insertCar(
                            existingCar.copy(
                                photoUrl = localFull.absolutePath,
                                frontPhotoPath = localFull.absolutePath,
                                combinedPhotoPath = localThumb.absolutePath
                            )
                        )
                    }

                    downloadedCount++
                } catch (e: Exception) {
                    failedCount++
                    Log.e("GoogleDriveRepository", "âŒ Error downloading photo ${photo.id}: ${e.message}", e)
                }
            }

            Log.i("GoogleDriveRepository", "âœ… Photo download completed: $downloadedCount downloaded, $failedCount failed")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "âŒ Failed to download photos locally: ${e.message}", e)
            Result.failure(e)
        }
    }
private fun extractDriveFileIdFromUrl(url: String): String? {
        return try {
            // Handle different Drive URL formats
            when {
                url.contains("/file/d/") -> {
                    val match = Regex("/file/d/([a-zA-Z0-9_-]+)").find(url)
                    match?.groupValues?.get(1)
                }
                url.contains("id=") -> {
                    val match = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(url)
                    match?.groupValues?.get(1)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Failed to extract file ID from URL: $url", e)
            null
        }
    }
}

/**
 * Normalizes Premium category names (e.g., "car_culture" -> "Car Culture").
 * Same logic as LocalRepository.
 */
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

/**
 * Normalizes Premium subcategory names (e.g., "race_day" -> "Race Day").
 * Same logic as LocalRepository.
 */
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
        "batman" -> "Batman"
        "star_wars" -> "Star Wars"
        "marvel" -> "Marvel"
        "jurassic_world" -> "Jurassic World"
        "back_to_the_future" -> "Back to the Future"
        "looney_tunes" -> "Looney Tunes"
        else -> subcategory.replace("_", " ").split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

/**
 * Exception thrown when Google Drive authentication has expired.
 */
class ReAuthRequiredException(message: String) : Exception(message)









