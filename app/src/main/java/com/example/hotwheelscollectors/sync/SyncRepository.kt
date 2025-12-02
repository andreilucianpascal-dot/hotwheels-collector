// SyncRepository.kt
package com.example.hotwheelscollectors.sync

import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.sync.ConflictResolver
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val firestoreRepository: FirestoreRepository,
    private val conflictResolver: ConflictResolver,
    private val userPreferences: UserPreferences
) {
    suspend fun sync(): Result<Unit> = try {
        // Get local changes
        val localChanges = carDao.getUnsyncedCars(firestoreRepository.userId).first()
        val photoChanges = photoDao.getUnsyncedPhotos(SyncStatus.SYNCED).first()

        // Get remote changes
        val lastSync = userPreferences.lastSync.first()
        val remoteChanges = firestoreRepository.getCarsSinceTimestamp(Date(lastSync))

        // Resolve conflicts
        val (resolvedLocal, resolvedRemote) = conflictResolver.resolve(
            localChanges = localChanges,
            remoteChanges = remoteChanges
        )

        // Upload local changes
        resolvedLocal.forEach { car ->
            firestoreRepository.updateCar(car)
            carDao.updateSyncStatus(car.id, SyncStatus.SYNCED.name)
        }

        // Upload photo changes (including per-user folders for own photos)
        photoChanges.forEach { photo ->
            when (photo.syncStatus) {
                SyncStatus.PENDING_UPLOAD -> uploadPhoto(photo)
                SyncStatus.PENDING_DELETE -> deletePhoto(photo)
                else -> { /* Already synced */ }
            }
        }

        // Apply remote changes
        resolvedRemote.forEach { car ->
            carDao.insertCar(car.copy(syncStatus = SyncStatus.SYNCED))
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadPhoto(photo: PhotoEntity) {
        try {
            // If the photo is for a globally-identified car (barcode assigned and global), store in global database folder
            // Otherwise, store user-collected photos in unique user folders for their own collections

            val isGlobal = photo.barcode != null && photo.isGlobal
            val userId = firestoreRepository.userId

            val cloudPath = if (isGlobal) {
                // Save under the global structure: Barcodes/<barcode>/front_photo.jpg OR back_photo.jpg, etc.
                firestoreRepository.uploadPhotoToGlobal(photo.localPath, photo.barcode!!, photo.photoType)
            } else {
                // Save under the per-user collection: UserCollections/<userId>/<folder>/<car_id>/user_photos/photo_x.jpg
                firestoreRepository.uploadPhotoToUserCollection(
                    photo.localPath,
                    userId = userId,
                    carId = photo.carId, // or photo.id if used as unique per photo
                    folder = photo.collectionFolder,
                    photoType = photo.photoType
                )
            }

            // Update photo entity with cloud path
            photoDao.updatePhoto(photo.copy(
                cloudPath = cloudPath,
                syncStatus = SyncStatus.SYNCED
            ))
        } catch (e: Exception) {
            // Handle upload failure
        }
    }

    private suspend fun deletePhoto(photo: PhotoEntity) {
        try {
            // Delete from cloud storage
            photo.cloudPath?.let { path ->
                firestoreRepository.deletePhoto(path)
            }

            // Hard delete local photo
            photoDao.hardDeletePhoto(photo.id)
        } catch (e: Exception) {
            // Handle delete failure
        }
    }
}