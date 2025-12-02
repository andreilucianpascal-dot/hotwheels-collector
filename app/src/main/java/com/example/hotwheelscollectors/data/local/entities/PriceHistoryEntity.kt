package com.example.hotwheelscollectors.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("carId"),
        Index("date")
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val price: Double,
    val source: String,
    val condition: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val date: Date = Date(),
    val createdAt: Date = Date(),
    val lastSyncedAt: Date? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val version: Int = 1
)