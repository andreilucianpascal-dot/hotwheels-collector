package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
    CONFLICT
}