package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val localPath: String,
    val thumbnailPath: String? = null,
    val cloudPath: String? = null,
    val type: PhotoType,
    val order: Int = 0,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    val createdAt: Date = Date(),
    val lastSyncedAt: Date? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val deletedAt: Date? = null,
    val version: Int = 1,

    // NEW FIELDS FOR GLOBAL DATABASE SYSTEM
    val barcode: String? = null,           // Barcode for global database lookup
    val isGlobal: Boolean = false,         // Whether this photo contributes to global database  
    val photoType: PhotoType = type,       // Alias for type field (for SyncRepository compatibility)
    val collectionFolder: String? = null,  // User's collection folder path
    val contributorUserId: String? = null, // Who contributed this to global database
    val verificationCount: Int = 1,        // How many users verified this data
    val isVerified: Boolean = false,        // Whether this has been community verified

    // OPTIMIZED PHOTO SYSTEM FIELDS
    val fullSizePath: String? = null,      // Full-size optimized image path
    val thumbnailWidth: Int? = null,       // Thumbnail dimensions
    val thumbnailHeight: Int? = null,
    val fullSizeWidth: Int? = null,        // Full-size dimensions
    val fullSizeHeight: Int? = null,
    val thumbnailSizeKB: Long? = null,     // File sizes for monitoring
    val fullSizeSizeKB: Long? = null,
    val isTemporary: Boolean = false,      // For back photos that should be deleted after barcode extraction
    
    // GOOGLE DRIVE INTEGRATION
    val driveFileId: String? = null,       // Google Drive file ID for programmatic download
    val driveThumbnailFileId: String? = null, // Google Drive thumbnail file ID
)

enum class PhotoType {
    FRONT,
    BACK,
    CARD_FRONT,
    CARD_BACK,
    OTHER
}