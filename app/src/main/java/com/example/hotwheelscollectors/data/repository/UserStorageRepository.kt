package com.example.hotwheelscollectors.data.repository

/**
 * Common interface for all storage implementations (Local, Google Drive, OneDrive, Dropbox).
 * Each implementation handles saving car data and photos to its respective storage medium.
 */
interface UserStorageRepository {
    /**
     * Saves a car with its photos and metadata to the storage medium.
     * 
     * @param data Complete car data including all metadata and pending photos
     * @param localThumbnail Path to the optimized thumbnail photo (already processed)
     * @param localFull Path to the optimized full-size photo (already processed)
     * @param barcode Extracted barcode from the back photo
     * @return Result containing the car ID if successful, or error if failed
     */
    suspend fun saveCar(
        data: CarDataToSync,
        localThumbnail: String,
        localFull: String,
        barcode: String
    ): Result<String>
}

