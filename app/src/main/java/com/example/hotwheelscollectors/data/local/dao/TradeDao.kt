package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.TradeOfferEntity
import com.example.hotwheelscollectors.data.local.entities.TradeStatus
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TradeDao {
    @Query("""
        SELECT * FROM trade_offers 
        WHERE userId = :userId 
        AND isDeleted = 0 
        ORDER BY 
            CASE 
                WHEN status = 'PENDING' THEN 0
                WHEN status = 'ACCEPTED' THEN 1
                ELSE 2
            END,
            createdAt DESC
    """)
    fun getAllTradeOffers(userId: String): Flow<List<TradeOfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeOffer(offer: TradeOfferEntity)

    @Update
    suspend fun updateTradeOffer(offer: TradeOfferEntity)

    @Query("""
        UPDATE trade_offers 
        SET status = :newStatus,
            updatedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun updateTradeStatus(
        id: String,
        newStatus: TradeStatus,
        timestamp: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("""
        UPDATE trade_offers 
        SET isDeleted = 1,
            deletedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun softDeleteTradeOffer(
        id: String,
        timestamp: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("DELETE FROM trade_offers WHERE id = :id")
    suspend fun hardDeleteTradeOffer(id: String)

    @Query("""
        SELECT * FROM trade_offers 
        WHERE status = 'PENDING' 
        AND isDeleted = 0 
        AND (expiresAt IS NULL OR expiresAt > :currentTime)
    """)
    fun getActiveTradeOffers(currentTime: Date = Date()): Flow<List<TradeOfferEntity>>

    @Query("""
        SELECT * FROM trade_offers 
        WHERE syncStatus != :status
        AND isDeleted = 0 
        ORDER BY createdAt DESC
    """)
    fun getUnsyncedOffers(status: SyncStatus = SyncStatus.SYNCED): Flow<List<TradeOfferEntity>>

    @Query("""
        UPDATE trade_offers 
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