package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.BackupMetadataEntity
import com.example.hotwheelscollectors.data.local.entities.BackupType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupMetadataEntity>>

    @Query("SELECT * FROM backup_metadata WHERE id = :id")
    suspend fun getBackupById(id: String): BackupMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupMetadataEntity)

    @Update
    suspend fun updateBackup(backup: BackupMetadataEntity)

    @Delete
    suspend fun deleteBackup(backup: BackupMetadataEntity)

    @Query("""
        SELECT * FROM backup_metadata 
        WHERE type = :type 
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    suspend fun getLatestBackup(type: BackupType): BackupMetadataEntity?

    @Query("""
        DELETE FROM backup_metadata 
        WHERE type = :type 
        AND createdAt < :cutoffDate
    """)
    suspend fun deleteOldBackups(
        type: BackupType,
        cutoffDate: Date = Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    )
}