package com.example.hotwheelscollectors.data.repository

import android.util.Log
import com.example.hotwheelscollectors.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DynamicStorageRepository - Wrapper that switches between different storage implementations
 * based on user preferences (Local, Google Drive, OneDrive, Dropbox).
 * 
 * This repository observes UserPreferences.storageLocation and delegates to the correct
 * repository implementation dynamically.
 */
@Singleton
class DynamicStorageRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val localRepository: LocalRepository,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository
) : UserStorageRepository {

    /**
     * Gets the active repository based on current storage location preference.
     * 
     * @return The repository that should be used for saving cars
     */
    private suspend fun getActiveRepository(): UserStorageRepository {
        val storageLocation = userPreferences.storageLocation.first()
        Log.d("DynamicStorageRepository", "📦 Reading storage location from UserPreferences: '$storageLocation'")
        
        // Normalize the storage location value (handle various formats and case variations)
        val normalizedLocation = when {
            storageLocation.equals("Google Drive", ignoreCase = true) || 
            storageLocation.equals("GoogleDrive", ignoreCase = true) ||
            storageLocation.equals("google drive", ignoreCase = true) -> {
                Log.d("DynamicStorageRepository", "✅ Normalized to: 'Google Drive'")
                "Google Drive"
            }
            storageLocation.equals("OneDrive", ignoreCase = true) -> "OneDrive"
            storageLocation.equals("Dropbox", ignoreCase = true) -> "Dropbox"
            storageLocation.equals("Device", ignoreCase = true) || 
            storageLocation.equals("Internal", ignoreCase = true) ||
            storageLocation.equals("Internal Storage (On Device)", ignoreCase = true) ||
            storageLocation.equals("Local", ignoreCase = true) ||
            storageLocation.isEmpty() -> {
                Log.d("DynamicStorageRepository", "✅ Normalized to: 'Device'")
                "Device"
            }
            else -> {
                Log.w("DynamicStorageRepository", "⚠️ Unknown storage location: '$storageLocation', defaulting to 'Device'")
                "Device"
            }
        }
        
        return when (normalizedLocation) {
            "Google Drive" -> {
                Log.d("DynamicStorageRepository", "✅ Using GoogleDriveRepository")
                googleDriveRepository
            }
            "OneDrive" -> {
                Log.d("DynamicStorageRepository", "Using OneDriveRepository (placeholder)")
                oneDriveRepository
            }
            "Dropbox" -> {
                Log.d("DynamicStorageRepository", "Using DropboxRepository (placeholder)")
                dropboxRepository
            }
            else -> {
                Log.d("DynamicStorageRepository", "✅ Using LocalRepository")
                localRepository
            }
        }
    }

    /**
     * Saves a car to the active storage repository based on user preferences.
     */
    override suspend fun saveCar(
        data: CarDataToSync,
        localThumbnail: String,
        localFull: String,
        barcode: String
    ): Result<String> {
        val activeRepository = getActiveRepository()
        val repositoryName = activeRepository::class.simpleName
        Log.d("DynamicStorageRepository", "💾 Saving car via $repositoryName")
        Log.d("DynamicStorageRepository", "   Car: ${data.name} (${data.brand})")
        Log.d("DynamicStorageRepository", "   Thumbnail: $localThumbnail")
        Log.d("DynamicStorageRepository", "   Full: $localFull")
        
        val result = activeRepository.saveCar(data, localThumbnail, localFull, barcode)
        
        if (result.isSuccess) {
            Log.d("DynamicStorageRepository", "✅ Car saved successfully via $repositoryName")
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            Log.e("DynamicStorageRepository", "❌ Failed to save car via $repositoryName: $error")
        }
        
        return result
    }
}

