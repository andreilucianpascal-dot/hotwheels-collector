package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserCloudSyncManager orchestrates periodic synchronization of Room Database
 * and local photos to the user's chosen cloud storage (Google Drive/OneDrive/Dropbox).
 * 
 * RESPONSIBILITIES:
 * 1. Orchestrate periodic sync to user's cloud storage
 * 2. Schedule background sync using WorkManager
 * 3. Handle conflict resolution (Last-Write-Wins)
 * 4. Track last sync timestamp for incremental syncs
 */
@Singleton
class UserCloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCloudSyncRepository: UserCloudSyncRepository,
    private val userPreferences: UserPreferences,
) {
    companion object {
        private const val TAG = "UserCloudSyncManager"
        private const val SYNC_INTERVAL_HOURS = 6L // Sync every 6 hours
        private const val SYNC_WORK_NAME = "user_cloud_sync"
    }
    
    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()
    
    /**
     * Initializes periodic background sync.
     * Should be called when the app starts or user logs in.
     */
    fun initializeBackgroundSync() {
        Log.d(TAG, "Initializing background cloud sync...")
        schedulePeriodicSync()
    }
    
    /**
     * Performs a cloud sync operation.
     * 
     * @param forceSync If true, sync even if interval hasn't passed
     * @return Result indicating success or failure
     */
    suspend fun performSync(forceSync: Boolean = false): Result<Unit> {
        return try {
            Log.d(TAG, "=== STARTING CLOUD SYNC ===")
            _syncState.value = CloudSyncState.Syncing
            
            // Get last cloud sync timestamp
            val lastCloudSync = userPreferences.lastCloudSync.first()
            
            // Check if sync is needed
            if (!forceSync && !shouldSync(lastCloudSync)) {
                Log.d(TAG, "Sync skipped - interval not reached yet")
                _syncState.value = CloudSyncState.Idle
                return Result.success(Unit)
            }
            
            Log.d(TAG, "Last cloud sync: ${if (lastCloudSync > 0) java.util.Date(lastCloudSync) else "Never"}")
            
            // Step 1: Export Room Database to JSON (incremental if possible)
            val exportResult = if (lastCloudSync > 0) {
                Log.d(TAG, "Exporting incremental database changes...")
                userCloudSyncRepository.exportIncrementalDatabaseToJson(lastCloudSync)
            } else {
                Log.d(TAG, "Exporting full database (first sync)...")
                userCloudSyncRepository.exportDatabaseToJson()
            }
            
            if (exportResult.isFailure) {
                Log.e(TAG, "Database export failed: ${exportResult.exceptionOrNull()?.message}")
                _syncState.value = CloudSyncState.Error(exportResult.exceptionOrNull()?.message ?: "Export failed")
                return Result.failure(exportResult.exceptionOrNull() ?: Exception("Export failed"))
            }
            
            val jsonFile = exportResult.getOrNull()
            if (jsonFile == null || !jsonFile.exists()) {
                Log.e(TAG, "Exported JSON file is null or doesn't exist")
                _syncState.value = CloudSyncState.Error("Exported file not found")
                return Result.failure(Exception("Exported file not found"))
            }
            
            Log.i(TAG, "✅ Database exported: ${jsonFile.absolutePath} (${jsonFile.length()} bytes)")
            
            // Step 2: Upload JSON to cloud
            Log.d(TAG, "Uploading database JSON to cloud...")
            val uploadResult = userCloudSyncRepository.uploadDatabaseJsonToCloud(jsonFile)
            
            if (uploadResult.isFailure) {
                Log.e(TAG, "Database JSON upload failed: ${uploadResult.exceptionOrNull()?.message}")
                _syncState.value = CloudSyncState.Error(uploadResult.exceptionOrNull()?.message ?: "Upload failed")
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
            
            Log.i(TAG, "✅ Database JSON uploaded: ${uploadResult.getOrNull()}")
            
            // Step 3: Upload local photos (incremental)
            Log.d(TAG, "Uploading photos to cloud...")
            val photosResult = userCloudSyncRepository.uploadPhotosToCloud(lastCloudSync)
            
            if (photosResult.isFailure) {
                Log.e(TAG, "Photos upload failed: ${photosResult.exceptionOrNull()?.message}")
                // Don't fail entire sync if photos upload fails - photos can be retried later
                Log.w(TAG, "⚠️ Photos upload failed, but continuing sync...")
            } else {
                val uploadedCount = photosResult.getOrNull() ?: 0
                Log.i(TAG, "✅ Photos uploaded: $uploadedCount")
            }
            
            // Step 4: Update last sync timestamp
            val currentTime = System.currentTimeMillis()
            userPreferences.updateLastCloudSync(currentTime)
            Log.i(TAG, "✅ Cloud sync completed successfully at ${java.util.Date(currentTime)}")
            
            _syncState.value = CloudSyncState.Success
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cloud sync failed: ${e.message}", e)
            _syncState.value = CloudSyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }
    
    /**
     * Schedules periodic background sync using WorkManager.
     */
    private fun schedulePeriodicSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(false) // Can sync when not charging
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<UserCloudSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(SYNC_WORK_NAME)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already set
                    syncRequest
                )
            
            Log.d(TAG, "✅ Periodic cloud sync scheduled (every $SYNC_INTERVAL_HOURS hours)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic sync: ${e.message}", e)
        }
    }
    
    /**
     * Checks if sync should be performed based on last sync timestamp.
     * 
     * @param lastSync Timestamp of last sync (0 if never synced)
     * @return true if sync is needed, false otherwise
     */
    private fun shouldSync(lastSync: Long): Boolean {
        if (lastSync == 0L) {
            // Never synced - always sync
            return true
        }
        
        val currentTime = System.currentTimeMillis()
        val syncIntervalMs = SYNC_INTERVAL_HOURS * 60 * 60 * 1000
        val timeSinceLastSync = currentTime - lastSync
        
        return timeSinceLastSync >= syncIntervalMs
    }
    
    /**
     * Cancels the periodic sync schedule.
     */
    fun cancelPeriodicSync() {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(SYNC_WORK_NAME)
            Log.d(TAG, "Periodic cloud sync cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel periodic sync: ${e.message}", e)
        }
    }
    
    /**
     * State of cloud sync operation.
     */
    sealed class CloudSyncState {
        object Idle : CloudSyncState()
        object Syncing : CloudSyncState()
        object Success : CloudSyncState()
        data class Error(val message: String) : CloudSyncState()
    }
}
