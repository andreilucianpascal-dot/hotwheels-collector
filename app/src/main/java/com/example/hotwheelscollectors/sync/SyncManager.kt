// SyncManager.kt
package com.example.hotwheelscollectors.sync

import android.content.Context
import androidx.work.*
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: SyncRepository,
    private val userPreferences: UserPreferences,
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    init {
        schedulePeriodicSync()
    }

    fun initializeBackgroundSync() {
        schedulePeriodicSync()
    }

    suspend fun performSync(forceSync: Boolean = false) {
        try {
            _syncState.value = SyncState.Syncing

            // Get last sync timestamp
            val lastSync = userPreferences.lastSync.first()

            if (!forceSync && !shouldSync(lastSync)) {
                _syncState.value = SyncState.Idle
                return
            }

            // Perform sync
            val result = syncRepository.sync()

            if (result.isSuccess) {
                userPreferences.updateLastSync(System.currentTimeMillis())
                _syncState.value = SyncState.Success
            } else {
                _syncState.value = SyncState.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync_cars",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    private fun shouldSync(lastSync: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val syncInterval = 1 * 60 * 60 * 1000 // 1 hour in milliseconds
        return currentTime - lastSync >= syncInterval
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Error(val message: String) : SyncState()
    }
}