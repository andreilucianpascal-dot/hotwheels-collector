package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.utils.PhotoOptimizer
import com.example.hotwheelscollectors.utils.PhotoVersions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for photo processing operations:
 * - Image optimization and compression
 * - Barcode extraction using ML Kit
 * - Image orientation correction
 * - File management
 */
@Singleton
class PhotoProcessingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoOptimizer: PhotoOptimizer
) {

    /**
     * Process a photo file and return PhotoData
     */
    suspend fun processPhotoFile(uri: Uri): PhotoData? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Step 1: Correct orientation based on EXIF data
                    val correctedBitmap = correctImageOrientation(bitmap, uri)

                    // Step 2: Create photo file
                    val photoFile = createPhotoFile()
                    FileOutputStream(photoFile).use { out ->
                        correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    PhotoData(
                        originalUri = uri,
                        savedPath = photoFile.absolutePath,
                        width = correctedBitmap.width,
                        height = correctedBitmap.height,
                        size = photoFile.length()
                    )
                } else null
            } catch (e: Exception) {
                android.util.Log.e("PhotoProcessingRepository", "Failed to process photo: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Extract barcode from image using ML Kit
     */
    suspend fun extractBarcodeFromImage(imagePath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
                val scanner = BarcodeScanning.getClient()

                val result = scanner.process(image).await()
                result.firstOrNull()?.rawValue ?: ""
            } catch (e: Exception) {
                android.util.Log.e("PhotoProcessingRepository", "Barcode extraction failed: ${e.message}", e)
                ""
            }
        }
    }

    /**
     * Create optimized photo versions (thumbnail + full size)
     */
    suspend fun createOptimizedVersions(
        frontPhotoPath: String,
        targetDir: File,
        filenamePrefix: String
    ): PhotoVersions {
        return withContext(Dispatchers.IO) {
            try {
                val frontPhotoUri = Uri.fromFile(File(frontPhotoPath))
                photoOptimizer.createOptimizedVersions(frontPhotoUri, targetDir, filenamePrefix)
            } catch (e: Exception) {
                android.util.Log.e("PhotoProcessingRepository", "Failed to create optimized versions: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Delete temporary photo file
     */
    suspend fun deleteTemporaryPhoto(photoPath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(photoPath)
                if (file.exists()) {
                    file.delete()
                    android.util.Log.d("PhotoProcessingRepository", "Deleted temporary photo: $photoPath")
                } else {
                    // no-op else branch to satisfy expression contexts in some Kotlin versions
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoProcessingRepository", "Failed to delete temporary photo: ${e.message}", e)
            }
        }
    }

    /**
     * Correct image orientation based on EXIF data
     */
    private fun correctImageOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = android.media.ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val rotatedBitmap = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
            rotatedBitmap
        } catch (e: Exception) {
            android.util.Log.e("PhotoProcessingRepository", "Failed to correct orientation: ${e.message}", e)
            bitmap
        }
    }

    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Create photo file
     */
    private fun createPhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(null), "HotWheelsPhotos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "IMG_${timeStamp}.jpg")
    }

    /**
     * Get file from URI
     */
    fun getFileFromUri(uri: Uri): File {
        return File(uri.path ?: "")
    }
}

/**
 * Data class for photo processing results
 */
data class PhotoData(
    val originalUri: Uri,
    val savedPath: String,
    val width: Int,
    val height: Int,
    val size: Long
)
