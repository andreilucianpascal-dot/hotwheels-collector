package com.example.hotwheelscollectors.data.local.entities

/**
 * Status for individual photo sync operations (thumbnail, full photo, barcode).
 * Used for incremental sync with retry logic.
 */
enum class PhotoSyncStatus {
    PENDING,        // Not yet uploaded
    UPLOADING,      // Upload in progress
    SYNCED,         // Upload successful
    FAILED,         // Upload failed (will retry)
    RETRYING        // Retry in progress
}

