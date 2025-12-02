package com.example.hotwheelscollectors.data.local.entities

/**
 * Status for Firestore data sync operations.
 * Used for incremental sync with retry logic.
 */
enum class DataSyncStatus {
    PENDING,        // Data not yet in Firestore
    SYNCING,        // Sync in progress
    SYNCED,         // Data is in Firestore
    FAILED,         // Sync failed (will retry)
    PARTIAL         // Only thumbnail synced (car appears in Browse, but without full photo)
}

