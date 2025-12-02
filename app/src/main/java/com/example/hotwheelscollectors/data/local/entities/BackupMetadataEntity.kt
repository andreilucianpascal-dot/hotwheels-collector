package com.example.hotwheelscollectors.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "backup_metadata",
    indices = [
        Index(value = ["type", "createdAt"])
    ]
)
data class BackupMetadataEntity(
    @PrimaryKey val id: String,
    val type: BackupType,
    val fileName: String,
    val size: Long,
    val carCount: Int,
    val photoCount: Int,
    val checksum: String,
    val cloudPath: String? = null,
    val status: BackupStatus = BackupStatus.PENDING,
    val error: String? = null,
    val createdAt: Date = Date(),
    val completedAt: Date? = null
)

enum class BackupType {
    MANUAL,
    AUTOMATIC,
    CLOUD_SYNC
}

enum class BackupStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}