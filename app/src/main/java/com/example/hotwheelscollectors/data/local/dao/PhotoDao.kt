package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE carId = :carId AND isDeleted = 0 ORDER BY `order`")
    fun getPhotosForCar(carId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("""
        UPDATE photos 
        SET isDeleted = 1,
            deletedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun softDeletePhoto(
        id: String,
        timestamp: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun hardDeletePhoto(id: String)

    @Query(
        """
        SELECT * FROM photos 
        WHERE syncStatus != :status
        AND isDeleted = 0 
        ORDER BY createdAt DESC
    """
    )
    fun getUnsyncedPhotos(status: SyncStatus = SyncStatus.SYNCED): Flow<List<PhotoEntity>>

    @Query("""
        UPDATE photos 
        SET syncStatus = :newStatus,
            lastSyncedAt = :timestamp 
        WHERE id IN (:ids)
    """)
    suspend fun updateSyncStatus(
        ids: List<String>,
        newStatus: SyncStatus,
        timestamp: Date = Date()
    )

    @Query("SELECT COUNT(*) FROM photos WHERE carId = :carId AND isDeleted = 0")
    fun getPhotoCount(carId: String): Flow<Int>

    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("DELETE FROM photos")
    suspend fun deleteAll()
}