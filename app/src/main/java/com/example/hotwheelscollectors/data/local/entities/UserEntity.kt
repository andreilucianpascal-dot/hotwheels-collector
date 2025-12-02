package com.example.hotwheelscollectors.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import com.example.hotwheelscollectors.data.local.entities.SyncStatus

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val preferences: Map<String, String> = emptyMap(),
    val lastLoginAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val lastSyncedAt: Date? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val version: Int = 1,
)