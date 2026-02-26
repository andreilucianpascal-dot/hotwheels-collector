package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.model.PersonalStorageType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileReader
import java.io.FileWriter
import java.io.File as JavaFile
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class CloudUserSettings(
    @SerializedName("primaryStorage")
    val primaryStorage: String = "LOCAL",
    @SerializedName("lastMigrationAt")
    val lastMigrationAt: Long = 0L,
    @SerializedName("schemaVersion")
    val schemaVersion: Int = 1
)

@Singleton
class CloudUserSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val gson: Gson,
    private val driveRootFolderManager: DriveRootFolderManager
) {
    companion object {
        private const val TAG = "CloudUserSettings"
        private const val SETTINGS_FILE_NAME = "user_settings.json"
        private const val FOLDER_NAME = "HotWheelsCollectors"
    }
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val transport = AndroidHttp.newCompatibleTransport()
    private suspend fun getDriveService(account: GoogleSignInAccount): Drive = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = account.account
            Drive.Builder(transport, jsonFactory, credential).setApplicationName("HotWheelsCollectors").build()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create Drive service: ${e.message}", e)
            throw Exception("Failed to create Drive service: ${e.message}")
        }
    }
    private suspend fun getOrCreateFolder(driveService: Drive): String = withContext(Dispatchers.IO) {
        try {
            // âœ… SINGLE SOURCE: share the same root folder logic/lock with GoogleDriveRepository
            driveRootFolderManager.getOrCreateRootFolderId(driveService)
        } catch (e: Exception) {
            Log.e(TAG, "âŒ Failed to get/create folder: ", e)
            throw e
        }
    }
private suspend fun findSettingsFile(driveService: Drive, folderId: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "name = '$SETTINGS_FILE_NAME' and '$folderId' in parents and trashed = false"
            val request = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
            val fileList: FileList = request.execute()
            val files = fileList.files
            if (files != null && files.isNotEmpty()) {
                return@withContext files[0].id
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to find settings file: ${e.message}", e)
            null
        }
    }
    suspend fun savePrimaryStorage(storageType: PersonalStorageType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== SAVING PRIMARY STORAGE TO DRIVE ===")
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            val settings = CloudUserSettings(primaryStorage = storageType.name, lastMigrationAt = System.currentTimeMillis(), schemaVersion = 1)
            val tempFile = JavaFile(context.cacheDir, "${SETTINGS_FILE_NAME}.tmp")
            FileWriter(tempFile).use { writer ->
                gson.toJson(settings, writer)
            }
            Log.d(TAG, "Serialized settings: ${tempFile.length()} bytes")
            
            var settingsFileId = findSettingsFile(driveService, folderId)
            val fileMetadata = File().apply {
                name = SETTINGS_FILE_NAME
            }
            val mediaContent = FileContent("application/json", tempFile)
            
            val resultFile = if (settingsFileId == null) {
                Log.d(TAG, "Creating new settings file...")
                fileMetadata.parents = Collections.singletonList(folderId)
                driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            } else {
                Log.d(TAG, "Updating existing settings file (ID: $settingsFileId)...")
                driveService.files().update(settingsFileId, fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }
            
            tempFile.delete()
            Log.i(TAG, "✅ Saved primary storage to Drive: $storageType (ID: ${resultFile.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save primary storage to Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun loadPrimaryStorage(): Result<PersonalStorageType?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== LOADING PRIMARY STORAGE FROM DRIVE ===")
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext Result.success(null)
            val driveService = getDriveService(account)
            val folderId = getOrCreateFolder(driveService)
            val settingsFileId = findSettingsFile(driveService, folderId) ?: return@withContext Result.success(null)
            val tempFile = JavaFile(context.cacheDir, "${SETTINGS_FILE_NAME}.tmp")
            val outputStream = java.io.FileOutputStream(tempFile)
            driveService.files().get(settingsFileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            
            if (tempFile.length() == 0L) {
                Log.d(TAG, "Settings file is empty")
                tempFile.delete()
                return@withContext Result.success(null)
            }
            
            val settings: CloudUserSettings = FileReader(tempFile).use { reader ->
                gson.fromJson(reader, CloudUserSettings::class.java)
            }
            tempFile.delete()
            
            val storageType = try {
                PersonalStorageType.valueOf(settings.primaryStorage)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Invalid storage type in cloud settings: ${settings.primaryStorage}")
                null
            }
            
            if (storageType != null) {
                Log.i(TAG, "✅ Loaded primary storage from Drive: $storageType")
                Log.d(TAG, "   Last migration: ${settings.lastMigrationAt}")
            } else {
                Log.w(TAG, "⚠️ Could not parse storage type from cloud settings")
            }
            
            Result.success(storageType)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to load primary storage from Drive: ${e.message}")
            Result.success(null)
        }
    }
    suspend fun restorePrimaryStorageFromCloud(): Result<PersonalStorageType?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== RESTORING PRIMARY STORAGE FROM CLOUD ===")
            val loadResult = loadPrimaryStorage()
            if (loadResult.isFailure) return@withContext Result.success(null)
            val cloudStorageType = loadResult.getOrNull()
            if (cloudStorageType == null) {
                Log.d(TAG, "No cloud settings found - keeping local settings")
                return@withContext Result.success(null)
            }
            
            Log.d(TAG, "Restoring cloud storage type to local: $cloudStorageType")
            userPreferences.updateStorageType(cloudStorageType)
            
            Log.i(TAG, "✅ Primary storage restored from cloud: $cloudStorageType")
            Result.success(cloudStorageType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore primary storage from cloud: ${e.message}", e)
            Result.success(null)
        }
    }
}



