package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UserCloudSyncWorker is a WorkManager worker that performs periodic background
 * synchronization of Room Database and local photos to user's cloud storage.
 * 
 * This worker runs periodically (every 6 hours) and syncs all data to the user's
 * chosen cloud storage (Google Drive/OneDrive/Dropbox).
 */
@HiltWorker
class UserCloudSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userCloudSyncManager: UserCloudSyncManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UserCloudSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING USER CLOUD SYNC WORK ===")
            Log.d(TAG, "Run attempt: $runAttemptCount")
            
            // Check if this is a force sync
            val forceSync = inputData.getBoolean("force_sync", false)
            
            // Perform sync
            val result = userCloudSyncManager.performSync(forceSync = forceSync)
            
            if (result.isSuccess) {
                Log.i(TAG, "✅ User cloud sync completed successfully")
                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "❌ User cloud sync failed: ${error?.message}", error)
                
                // Retry if we haven't exceeded max attempts
                if (runAttemptCount < 3) {
                    Log.d(TAG, "Retrying sync (attempt $runAttemptCount/3)...")
                    Result.retry()
                } else {
                    Log.e(TAG, "Max retry attempts reached - sync failed")
                    Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ User cloud sync worker failed: ${e.message}", e)
            
            // Retry if we haven't exceeded max attempts
            if (runAttemptCount < 3) {
                Log.d(TAG, "Retrying sync after exception (attempt $runAttemptCount/3)...")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached - sync failed")
                Result.failure()
            }
        }
    }
}
