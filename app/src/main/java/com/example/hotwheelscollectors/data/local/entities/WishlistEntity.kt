package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "wishlist",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class WishlistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val brand: String? = null,
    val model: String? = null,
    val series: String? = null,
    val year: Int? = null,
    val color: String? = null,
    val isPremium: Boolean = false,
    val maxPrice: Double? = null,
    val notes: String? = null,
    val priority: Int = 0,
    val notificationsEnabled: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val lastSyncedAt: Date? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val version: Int = 1
)