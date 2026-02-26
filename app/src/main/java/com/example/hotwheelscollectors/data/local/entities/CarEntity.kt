package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "cars",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["model"]),
        Index(value = ["brand"]),
        Index(value = ["year"]),
        Index(value = ["isPremium"]),
        Index(value = ["timestamp"]),
        Index(value = ["barcode"])
    ]
)
@TypeConverters(com.example.hotwheelscollectors.data.local.Converters::class)
data class CarEntity(
    @PrimaryKey
    val id: String = "",

    val userId: String = "",

    val model: String = "",

    val brand: String = "",

    val year: Int = 0,

    val photoUrl: String = "",

    val folderPath: String = "",

    val isPremium: Boolean = false,

    val timestamp: Long = System.currentTimeMillis(),

    val barcode: String = "",

    val frontPhotoPath: String = "",

    val backPhotoPath: String = "",

    val combinedPhotoPath: String = "",

    val searchKeywords: List<String> = emptyList(),

    val series: String = "",

    val subseries: String = "",

    val color: String = "",

    val number: String = "",

    val isSTH: Boolean = false,

    val isTH: Boolean = false,

    val isFirstEdition: Boolean = false,

    val condition: String = "",

    val purchasePrice: Double = 0.0,

    // ISO 4217 currency code for purchasePrice (e.g., RON, EUR, GBP)
    val purchaseCurrency: String = "",

    val currentValue: Double = 0.0,

    val notes: String = "",

    val location: String = "",

    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,

    val lastModified: Date = Date(),

    val isFavorite: Boolean = false,

    val tags: List<String> = emptyList(),

    val customFields: Map<String, String> = emptyMap(),

    // Conflict resolution properties
    val isDeleted: Boolean = false,
    val version: Long = 1L,
    val updatedAt: Long = System.currentTimeMillis(),

    // Incremental Sync Status (Hybrid Strategy)
    // Overall sync status
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val lastSyncAttempt: Long? = null,

    // Thumbnail sync status (PRIORITATE 1 - apare în Browse)
    val thumbnailSyncStatus: PhotoSyncStatus = PhotoSyncStatus.PENDING,
    val thumbnailFirebaseUrl: String? = null,
    val thumbnailSyncAttempts: Int = 0,

    // Full photo sync status (PRIORITATE 3 - LAZY, doar pentru "Add to My Collection")
    val fullPhotoSyncStatus: PhotoSyncStatus = PhotoSyncStatus.PENDING,
    val fullPhotoFirebaseUrl: String? = null,
    val fullPhotoSyncAttempts: Int = 0,

    // Barcode sync status (PRIORITATE 4 - OPTIMIZAT, skip dacă există deja)
    val barcodeSyncStatus: PhotoSyncStatus = PhotoSyncStatus.PENDING,
    val barcodeFirebaseUrl: String? = null,
    val barcodeSyncAttempts: Int = 0,

    // Firestore data sync status (PRIORITATE 2 - apare în Browse)
    val firestoreDataSyncStatus: DataSyncStatus = DataSyncStatus.PENDING,
    val firestoreDataSyncAttempts: Int = 0,

    // Sync priority (100 = highest, 0 = lowest)
    val syncPriority: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    
    // ✅ Browse duplicate prevention: Firebase URL of original photo from Browse
    // Used to prevent adding the same car from Browse multiple times
    // null for cars added via Take Photos (not from Browse)
    val originalBrowsePhotoUrl: String? = null
)