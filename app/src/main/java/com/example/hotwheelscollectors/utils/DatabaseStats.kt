package com.example.hotwheelscollectors.utils

/**
 * Data class representing database statistics for cleanup operations
 */
data class DatabaseStats(
    val totalCars: Int = 0,
    val totalPhotos: Int = 0,
    val totalKeywords: Int = 0,
    val mainlineCars: Int = 0,
    val othersCars: Int = 0,
    val premiumCars: Int = 0,
    val duplicateCars: Int = 0,
    val genericBrandCars: Int = 0,
    val orphanedPhotos: Int = 0,
    val databaseSize: Long = 0,
    val lastCleanupDate: String = ""
)
