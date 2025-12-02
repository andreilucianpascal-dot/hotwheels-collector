package com.example.hotwheelscollectors.data.repository

/**
 * Data class to hold all necessary information for syncing a new car.
 * This simplifies the function signature of the repository method.
 */
data class CarDataToSync(
    val userId: String,
    val name: String,
    val brand: String,
    val series: String,
    val category: String, // subcategory for Mainline (Rally, Convertibles, etc.)
    val subcategory: String? = null, // subcategory for Premium only (Back to the Future, etc.)
    val color: String,
    val year: Int?,
    var barcode: String,
    val notes: String,
    val isTH: Boolean,
    val isSTH: Boolean,
    val isPremium: Boolean, // Added missing isPremium field
    val screenType: String,
    val pendingPhotos: List<PhotoData>,
    val preOptimizedThumbnailPath: String,
    val preOptimizedFullPath: String
)

/**
 * Result of the car synchronization operation.
 */
data class SyncResult(
    val isSuccess: Boolean,
    val message: String,
    val finalCarId: String? = null
)

