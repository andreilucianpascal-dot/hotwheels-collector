package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * GoogleDriveRepository handles saving cars and photos to Google Drive.
 * 
 * RESPONSIBILITIES:
 * 1. Upload photos to Google Drive
 * 2. Save car metadata to Room Database (with Drive URLs)
 * 3. Create PhotoEntity records with Drive URLs
 * 4. Handle Google Drive API authentication
 */
class GoogleDriveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao
) : UserStorageRepository {
    
    private val client = OkHttpClient()
    private val folderName = "HotWheelsCollectors"

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
            
            // Validate input
            if (localThumbnail.isEmpty() || !File(localThumbnail).exists()) {
                Log.e("GoogleDriveRepository", "Thumbnail file not found: $localThumbnail")
                return@withContext Result.failure(
                    IllegalArgumentException("Thumbnail file not found")
                )
            }
            
            if (localFull.isEmpty() || !File(localFull).exists()) {
                Log.e("GoogleDriveRepository", "Full-size file not found: $localFull")
                return@withContext Result.failure(
                    IllegalArgumentException("Full-size file not found")
                )
            }
            
            // Check Google Sign-In
        val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e("GoogleDriveRepository", "User not signed in to Google Drive")
                return@withContext Result.failure(
                    IllegalStateException("User not signed in to Google Drive")
                )
            }
            
        val accessToken = account.idToken
            if (accessToken == null) {
                Log.e("GoogleDriveRepository", "Missing Google ID token")
                return@withContext Result.failure(
                    IllegalStateException("Missing Google ID token for Drive access")
                )
            }
            
            // Get or create Drive folder
        val folderId = getOrCreateFolder(accessToken)
            Log.d("GoogleDriveRepository", "Drive folder ID: $folderId")
            
            // Upload photos to Drive
            val thumbnailUrl = uploadPhotoToDrive(localThumbnail, accessToken, folderId, "thumbnail_$carId.jpg")
            val fullUrl = uploadPhotoToDrive(localFull, accessToken, folderId, "full_$carId.jpg")
            
            Log.d("GoogleDriveRepository", "Photos uploaded to Drive:")
            Log.d("GoogleDriveRepository", "  - Thumbnail URL: $thumbnailUrl")
            Log.d("GoogleDriveRepository", "  - Full URL: $fullUrl")
            
            // Create CarEntity with Drive URLs
            val carEntity = CarEntity(
                id = carId,
                userId = data.userId,
                model = data.name,
                brand = data.brand,
                series = data.series,
                subseries = data.category,
                folderPath = data.series,
                color = data.color,
                year = data.year ?: 0,
                barcode = barcode,
                notes = data.notes,
                isTH = data.isTH,
                isSTH = data.isSTH,
                timestamp = System.currentTimeMillis(),
                lastModified = Date(),
                syncStatus = SyncStatus.SYNCED, // Already on Drive
                photoUrl = fullUrl,
                frontPhotoPath = fullUrl,
                combinedPhotoPath = thumbnailUrl
            )
            
            // Save car to Room Database
            carDao.insertCar(carEntity)
            Log.i("GoogleDriveRepository", "✅ Car saved to Room Database with Drive URLs")
            
            // Create PhotoEntity with Drive URLs
            val photoEntity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                carId = carId,
                localPath = "", // No local copy
                thumbnailPath = thumbnailUrl,
                fullSizePath = fullUrl,
                cloudPath = fullUrl,
                type = PhotoType.FRONT,
                syncStatus = SyncStatus.SYNCED,
                isTemporary = false,
                barcode = barcode.takeIf { it.isNotEmpty() },
                contributorUserId = data.userId
            )
            
            // Save photo to Room Database
            photoDao.insertPhoto(photoEntity)
            Log.i("GoogleDriveRepository", "✅ Photo saved to Room Database with Drive URLs")
            
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
     * Finds or creates the HotWheelsCollectors folder in Google Drive.
     */
    private fun getOrCreateFolder(accessToken: String): String {
        // Try to find existing folder
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val request = Request.Builder()
            .url(
                "https://www.googleapis.com/drive/v3/files?q=" +
                        java.net.URLEncoder.encode(query, "UTF-8") +
                        "&spaces=drive&fields=files(id,name)"
            )
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        client.newCall(request).execute().use { resp ->
            if (resp.code == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            
            val body = resp.body?.string() ?: ""
            val files = JSONObject(body).optJSONArray("files") ?: JSONArray()
            
            if (files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        }
        
        // Create folder if not found
        val meta = JSONObject()
        meta.put("name", folderName)
        meta.put("mimeType", "application/vnd.google-apps.folder")
        
        val metaBody = meta.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val folderRequest = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .post(metaBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()
        
        client.newCall(folderRequest).execute().use { resp ->
            if (resp.code == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            
            val body = resp.body?.string() ?: ""
            val json = JSONObject(body)
            return json.getString("id")
        }
    }

    /**
     * Uploads a single photo to Google Drive.
     * Returns the Drive file ID (can be used to construct view/download URLs).
     */
    private fun uploadPhotoToDrive(
        localPath: String,
        accessToken: String,
        folderId: String,
        fileName: String
    ): String {
        val file = File(localPath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $localPath")
        }
        
        val metadata = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().put(folderId))
            put("mimeType", "image/jpeg")
        }
        
        val metaBody = metadata.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(MultipartBody.Part.createFormData("metadata", null, metaBody))
            .addPart(MultipartBody.Part.createFormData("file", fileName, fileBody))
            .build()
        
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,webViewLink")
            .post(multipartBody)
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        client.newCall(request).execute().use { resp ->
            if (resp.code == 401) {
                throw ReAuthRequiredException("Google session expired, please sign in again")
            }
            
            if (!resp.isSuccessful) {
                throw Exception("Failed to upload file to Drive (HTTP ${resp.code})")
            }
            
            val body = resp.body?.string() ?: throw Exception("Empty response from Google Drive")
            val json = JSONObject(body)
            val fileId = json.getString("id")
            
            // Return view URL
            return "https://drive.google.com/file/d/$fileId/view"
        }
    }
    
    /**
     * Uploads a photo to Google Drive (public method for backup use).
     * Used by CarSyncRepository for personal cloud backup.
     */
    suspend fun uploadPhoto(localPath: String, barcode: String, photoType: PhotoType): String {
        return try {
            Log.d("GoogleDriveRepository", "Uploading photo for backup: $localPath")
            
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw Exception("User not signed in to Google Drive")
            val accessToken = account.idToken
                ?: throw Exception("Missing Google ID token")
            
            val folderId = getOrCreateFolder(accessToken)
            
            val timestamp = System.currentTimeMillis()
            val fileName = if (barcode.isNotEmpty()) {
                "${barcode}_${photoType.name.lowercase()}_$timestamp.jpg"
            } else {
                "photo_${photoType.name.lowercase()}_$timestamp.jpg"
            }
            
            uploadPhotoToDrive(localPath, accessToken, folderId, fileName)
            
        } catch (e: Exception) {
            Log.e("GoogleDriveRepository", "Photo backup upload failed: ${e.message}", e)
            ""
        }
    }
}

/**
 * Exception thrown when Google Drive authentication has expired.
 */
class ReAuthRequiredException(message: String) : Exception(message)
