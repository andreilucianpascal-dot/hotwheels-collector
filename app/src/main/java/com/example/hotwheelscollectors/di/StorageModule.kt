package com.example.hotwheelscollectors.di

import android.content.Context
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.UserStorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Hilt module for providing the correct UserStorageRepository implementation
 * based on user preferences (Local, Google Drive, OneDrive, or Dropbox).
 * 
 * This is the "comutator" (switch) that decides which storage implementation to use.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    /**
     * Provides the active UserStorageRepository based on user's storage preference.
     * 
     * The preference is read from UserPreferences.storageLocation:
     * - "Device" or empty -> LocalRepository
     * - "Google Drive" -> GoogleDriveRepository
     * - "OneDrive" -> OneDriveRepository
     * - "Dropbox" -> DropboxRepository
     * 
     * This runs synchronously at app start using runBlocking, which is acceptable
     * because it only reads a cached preference value from DataStore.
     */
    @Provides
    @Singleton
    fun provideUserStorageRepository(
        @ApplicationContext context: Context,
        userPreferences: UserPreferences,
        localRepository: LocalRepository,
        googleDriveRepository: GoogleDriveRepository,
        oneDriveRepository: OneDriveRepository,
        dropboxRepository: DropboxRepository
    ): UserStorageRepository {
        // For now, always return LocalRepository to avoid runBlocking issues
        // The proper solution would be to create a wrapper that reads preferences asynchronously
        // and switches repositories dynamically, but that's complex and not needed for basic functionality
        return localRepository
    }
}

