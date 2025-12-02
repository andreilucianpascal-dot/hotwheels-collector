package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.WishlistEntity
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WishlistDao {
    @Query("""
        SELECT * FROM wishlist 
        WHERE userId = :userId 
        AND isDeleted = 0 
        ORDER BY priority DESC, createdAt DESC
    """)
    fun getAllWishlistItems(userId: String): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistEntity)

    @Update
    suspend fun updateWishlistItem(item: WishlistEntity)

    @Query("""
        UPDATE wishlist 
        SET isDeleted = 1,
            deletedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun softDeleteWishlistItem(
        id: String,
        timestamp: Date = Date(),
        syncStatus: SyncStatus = SyncStatus.PENDING_DELETE
    )

    @Query("DELETE FROM wishlist WHERE id = :id")
    suspend fun hardDeleteWishlistItem(id: String)

    @Query("""
        SELECT * FROM wishlist 
        WHERE userId = :userId 
        AND isDeleted = 0 
        AND maxPrice >= :currentPrice 
        AND (
            LOWER(name) LIKE '%' || LOWER(:name) || '%'
            OR (:brand IS NOT NULL AND LOWER(brand) = LOWER(:brand))
            OR (:model IS NOT NULL AND LOWER(model) = LOWER(:model))
            OR (:series IS NOT NULL AND LOWER(series) = LOWER(:series))
        )
    """)
    fun findMatchingWishlistItems(
        userId: String,
        name: String,
        brand: String?,
        model: String?,
        series: String?,
        currentPrice: Double
    ): Flow<List<WishlistEntity>>

    @Query("""
        SELECT * FROM wishlist 
        WHERE syncStatus != :status
        AND isDeleted = 0 
        ORDER BY updatedAt DESC
    """)
    fun getUnsyncedItems(status: SyncStatus = SyncStatus.SYNCED): Flow<List<WishlistEntity>>

    @Query("""
        UPDATE wishlist 
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