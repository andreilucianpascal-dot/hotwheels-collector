package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "trade_offers",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["offeredCarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("offeredCarId"),
        Index("status")
    ]
)
data class TradeOfferEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val offeredCarId: String,
    val requestedCarDetails: String,
    val status: TradeStatus,
    val notes: String? = null,
    val contactInfo: String? = null,
    val expiresAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val lastSyncedAt: Date? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val version: Int = 1
)

enum class TradeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    COMPLETED,
    CANCELLED
}