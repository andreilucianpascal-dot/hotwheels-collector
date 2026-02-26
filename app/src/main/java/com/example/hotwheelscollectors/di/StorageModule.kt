package com.example.hotwheelscollectors.di

import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.repository.DynamicStorageRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.UserStorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing the correct UserStorageRepository implementation
 * based on user preferences (Local, Google Drive, OneDrive, or Dropbox).
 * 
 * Uses DynamicStorageRepository which switches between implementations dynamically
 * based on UserPreferences.storageLocation.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    /**
     * Provides the DynamicStorageRepository which switches between storage implementations
     * based on user's storage preference.
     * 
     * The preference is read from UserPreferences.storageLocation:
     * - "Device", "Internal", "Local", or empty -> LocalRepository
     * - "Google Drive" or "GoogleDrive" -> GoogleDriveRepository
     * - "OneDrive" -> OneDriveRepository (placeholder - saves locally)
     * - "Dropbox" -> DropboxRepository (placeholder - saves locally)
     * 
     * The DynamicStorageRepository reads preferences asynchronously when saveCar() is called,
     * allowing the user to change storage preferences without restarting the app.
     */
    @Provides
    @Singleton
    fun provideUserStorageRepository(
        userPreferences: UserPreferences,
        localRepository: LocalRepository,
        googleDriveRepository: GoogleDriveRepository,
        oneDriveRepository: OneDriveRepository,
        dropboxRepository: DropboxRepository
    ): UserStorageRepository {
        return DynamicStorageRepository(
            userPreferences = userPreferences,
            localRepository = localRepository,
            googleDriveRepository = googleDriveRepository,
            oneDriveRepository = oneDriveRepository,
            dropboxRepository = dropboxRepository
        )
    }
}

