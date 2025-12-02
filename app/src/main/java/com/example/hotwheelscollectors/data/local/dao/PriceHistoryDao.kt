package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.PriceHistoryEntity
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PriceHistoryDao {
    @Query("""
        SELECT * FROM price_history 
        WHERE carId = :carId 
        AND isDeleted = 0 
        ORDER BY date DESC
    """)
    fun getPriceHistoryForCar(carId: String): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecord(record: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecords(records: List<PriceHistoryEntity>)

    @Update
    suspend fun updatePriceRecord(record: PriceHistoryEntity)

    @Query("""
        UPDATE price_history 
        SET isDeleted = 1,
            deletedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun softDeletePriceRecord(
        id: String,
        timestamp: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.PENDING_DELETE,
    )

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun hardDeletePriceRecord(id: String)

    @Query("""
        SELECT AVG(price) FROM price_history 
        WHERE carId = :carId 
        AND isDeleted = 0 
        AND date >= :startDate
    """)
    fun getAveragePriceForPeriod(carId: String, startDate: Date): Flow<Double?>

    @Query("""
        SELECT * FROM price_history 
        WHERE syncStatus != :status
        AND isDeleted = 0 
        ORDER BY createdAt DESC
    """)
    fun getUnsyncedRecords(status: SyncStatus = SyncStatus.SYNCED): Flow<List<PriceHistoryEntity>>

    @Query("""
        UPDATE price_history 
        SET syncStatus = :newStatus,
            lastSyncedAt = :timestamp 
        WHERE id IN (:ids)
    """)
    suspend fun updateSyncStatus(
        ids: List<String>,
        newStatus: SyncStatus,
        timestamp: Date = Date()
    )
}