package com.example.hotwheelscollectors.offline

import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStrategy @Inject constructor(
    private val carDao: CarDao,
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun sync(pendingChanges: List<CarEntity>) {
        pendingChanges.forEach { car ->
            when (car.syncStatus) {
                SyncStatus.PENDING_UPLOAD -> handleUpload(car)
                SyncStatus.PENDING_DELETE -> handleDelete(car)
                SyncStatus.CONFLICT -> resolveConflict(car)
                else -> { /* Already synced */ }
            }
        }
    }

    private suspend fun handleUpload(car: CarEntity) {
        try {
            firestoreRepository.updateCar(car)
            carDao.updateSyncStatus(
                car.id,  // Changed from list to single ID
                SyncStatus.SYNCED.name  // Changed from enum to string
            )
        } catch (e: Exception) {
            // Handle upload failure
        }
    }

    private suspend fun handleDelete(car: CarEntity) {
        try {
            firestoreRepository.deleteCar(car.id)
            // Use regular delete instead of hardDeleteCar
            carDao.deleteCarById(car.id)
        } catch (e: Exception) {
            // Handle delete failure
        }
    }

    private suspend fun resolveConflict(car: CarEntity) {
        try {
            val remoteCar = firestoreRepository.getCarById(car.id)
            if (remoteCar != null) {
                // Use lastModified instead of version
                if (car.lastModified.after(remoteCar.lastModified)) {
                    handleUpload(car)
                } else {
                    carDao.insertCar(remoteCar)
                }
            } else {
                handleUpload(car)
            }
        } catch (e: Exception) {
            // Handle conflict resolution failure
        }
    }
}