// ImageManager.kt
package com.example.hotwheelscollectors.image

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    private val context: Context,
    private val imageCompressor: ImageCompressor,
    private val imageCache: ImageCache,
    private val imageStorage: ImageStorage
) {
    suspend fun saveImage(uri: Uri, type: ImageType): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create temp file
            val tempFile = createTempFile(type)

            // Copy original to temp file
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Compress image
            val compressedFile = imageCompressor.compress(tempFile, type)

            // Store in local storage
            val storedPath = imageStorage.store(compressedFile, type)

            // Cache the image
            imageCache.cacheImage(storedPath.toUri(), type)

            // Cleanup temp files
            tempFile.delete()
            compressedFile.delete()

            Result.success(storedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadImage(path: String): Flow<Uri> {
        val uri = path.toUri()
        return imageCache.getCachedImage(uri) ?: imageStorage.load(path)
    }

    suspend fun deleteImage(path: String) {
        imageStorage.delete(path)
        imageCache.removeFromCache(path.toUri())
    }

    private fun createTempFile(type: ImageType): File {
        return File.createTempFile(
            "temp_${System.currentTimeMillis()}",
            type.extension,
            context.cacheDir
        )
    }
}

enum class ImageType(val extension: String) {
    CAR_FRONT(".jpg"),
    CAR_BACK(".jpg"),
    CARD_FRONT(".jpg"),
    CARD_BACK(".jpg"),
    THUMBNAIL(".jpg"),
    PROFILE(".jpg")
}