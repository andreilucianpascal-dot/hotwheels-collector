// OfflineManager.kt
package com.example.hotwheelscollectors.offline

import android.content.Context
import androidx.work.*
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: CacheManager,
    private val networkMonitor: NetworkMonitor,
    private val syncStrategy: SyncStrategy,
    private val carDao: CarDao,
    private val firestoreRepository: FirestoreRepository,
    private val userPreferences: UserPreferences
) {
    private val _offlineState = MutableStateFlow<OfflineState>(OfflineState.Unknown)
    val offlineState = _offlineState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        observeNetworkState()
        setupPeriodicSync()
    }

    fun initializeOfflineMode() {
        observeNetworkState()
        setupPeriodicSync()
    }

    private fun observeNetworkState() {
        networkMonitor.isOnline.onEach { isOnline ->
            _offlineState.value = if (isOnline) {
                scope.launch { syncPendingChanges() }
                OfflineState.Online
            } else {
                OfflineState.Offline
            }
        }.launchIn(scope)
    }

    private suspend fun syncPendingChanges() {
        try {
            val pendingChanges = carDao.getUnsyncedCars(firestoreRepository.userId).first()
            if (pendingChanges.isNotEmpty()) {
                syncStrategy.sync(pendingChanges)
            }
        } catch (e: Exception) {
            _offlineState.value = OfflineState.Error(e.message ?: "Sync failed")
        }
    }

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "offline_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    suspend fun prefetchData() {
        cacheManager.prefetchCriticalData()
    }

    suspend fun clearCache() {
        cacheManager.clearCache()
    }

    sealed class OfflineState {
        object Unknown : OfflineState()
        object Online : OfflineState()
        object Offline : OfflineState()
        data class Error(val message: String) : OfflineState()
    }
}