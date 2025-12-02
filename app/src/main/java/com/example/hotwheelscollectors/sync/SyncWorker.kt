// SyncWorker.kt
package com.example.hotwheelscollectors.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.CarSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val carSyncRepository: CarSyncRepository,
    private val carDao: CarDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if this is a single car sync or full sync
            val carId = inputData.getString("car_id")
            
            if (carId != null && carId.isNotEmpty()) {
                // Sync single car (incremental)
                Log.d("SyncWorker", "Syncing single car (incremental): $carId")
                carSyncRepository.syncCarIncremental(carId)
                Log.d("SyncWorker", "Single car incremental sync completed")
            } else {
                // Full sync: Retry all failed uploads
                Log.d("SyncWorker", "Performing full sync - retrying failed uploads")
                
                // Retry failed thumbnail uploads
                val failedThumbnails = carDao.getCarsWithFailedThumbnailSync()
                Log.d("SyncWorker", "Found ${failedThumbnails.size} cars with failed thumbnail sync")
                failedThumbnails.forEach { car ->
                    Log.d("SyncWorker", "Retrying thumbnail sync for car: ${car.id}")
                    carSyncRepository.syncCarIncremental(car.id)
                }
                
                // Retry failed full photo uploads
                val failedFullPhotos = carDao.getCarsWithFailedFullPhotoSync()
                Log.d("SyncWorker", "Found ${failedFullPhotos.size} cars with failed full photo sync")
                failedFullPhotos.forEach { car ->
                    Log.d("SyncWorker", "Retrying full photo sync for car: ${car.id}")
                    carSyncRepository.syncCarIncremental(car.id)
                }
                
                // Retry failed barcode uploads
                val failedBarcodes = carDao.getCarsWithFailedBarcodeSync()
                Log.d("SyncWorker", "Found ${failedBarcodes.size} cars with failed barcode sync")
                failedBarcodes.forEach { car ->
                    Log.d("SyncWorker", "Retrying barcode sync for car: ${car.id}")
                    carSyncRepository.syncCarIncremental(car.id)
                }
                
                // Retry failed Firestore data syncs
                val failedFirestoreData = carDao.getCarsWithFailedFirestoreDataSync()
                Log.d("SyncWorker", "Found ${failedFirestoreData.size} cars with failed Firestore data sync")
                failedFirestoreData.forEach { car ->
                    Log.d("SyncWorker", "Retrying Firestore data sync for car: ${car.id}")
                    carSyncRepository.syncCarIncremental(car.id)
                }
                
                // Also perform regular full sync
                syncManager.performSync()
                
                Log.d("SyncWorker", "Full sync completed successfully")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}