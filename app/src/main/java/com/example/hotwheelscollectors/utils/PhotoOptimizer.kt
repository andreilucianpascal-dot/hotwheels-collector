package com.example.hotwheelscollectors.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class PhotoOptimizer @Inject constructor(private val context: Context) {

    companion object {
        const val THUMBNAIL_WIDTH = 400
        const val FULL_SIZE_WIDTH = 1280
        const val MAX_FILE_SIZE_KB = 200 // For thumbnails (target: 200KB) - 20% of 1MB
        const val MAX_FULL_SIZE_KB = 800 // For full-size images (target: 800KB) - 80% of 1MB
        // 🎯 TOTAL PER CAR: 200KB + 800KB = 1000KB = 1MB EXACT
    }

    suspend fun createOptimizedVersions(
        originalUri: Uri,
        outputDir: File,
        filename: String
    ): PhotoVersions {
        var originalBitmap: Bitmap? = null
        return try {
            Log.d("PhotoOptimizer", "Creating optimized versions for: $filename")
            Log.d("PhotoOptimizer", "🔍 DEBUG: Input URI: $originalUri")
            Log.d("PhotoOptimizer", "🔍 DEBUG: Output dir: ${outputDir.absolutePath}")
            Log.d("PhotoOptimizer", "🔍 DEBUG: Filename: $filename")
            
            // Ensure output directory exists
            if (!outputDir.exists()) {
                outputDir.mkdirs()
                Log.d("PhotoOptimizer", "Created output directory: ${outputDir.absolutePath}")
            }
            
            originalBitmap = loadBitmapFromUri(originalUri)
            if (originalBitmap == null) {
                Log.e("PhotoOptimizer", "Failed to load original bitmap")
                return PhotoVersions("", "", 0, 0, 0, 0)
            }
            Log.d("PhotoOptimizer", "🔍 DEBUG: Loaded bitmap - Width: ${originalBitmap.width}, Height: ${originalBitmap.height}")

            val thumbnailPath = createThumbnail(originalBitmap, outputDir, "${filename}_thumb")
            
            // Load a FRESH bitmap for full-size (originalBitmap may be recycled by createThumbnail)
            val fullSizeBitmap = loadBitmapFromUri(originalUri)
            val fullSizePath = if (fullSizeBitmap != null) {
                createFullSizeImage(fullSizeBitmap, outputDir, "${filename}_full")
            } else {
                ""
            }
            
            // Now safe to recycle original bitmap
            if (!originalBitmap.isRecycled) {
                originalBitmap.recycle()
                Log.d("PhotoOptimizer", "Original bitmap recycled after processing")
            }
            
            val thumbnailSize = File(thumbnailPath).length()
            val fullSizeSize = File(fullSizePath).length()
            
            Log.i("PhotoOptimizer", "✅ Created optimized versions:")
            Log.i("PhotoOptimizer", "   Thumbnail: ${thumbnailSize / 1024}KB")
            Log.i("PhotoOptimizer", "   Full-size: ${fullSizeSize / 1024}KB")
            Log.i("PhotoOptimizer", "   Total: ${(thumbnailSize + fullSizeSize) / 1024}KB")
            
            PhotoVersions(
                thumbnailPath = thumbnailPath,
                fullSizePath = fullSizePath,
                thumbnailWidth = THUMBNAIL_WIDTH,
                thumbnailHeight = calculateHeight(originalBitmap, THUMBNAIL_WIDTH),
                fullSizeWidth = FULL_SIZE_WIDTH,
                fullSizeHeight = calculateHeight(originalBitmap, FULL_SIZE_WIDTH)
            )
            
        } catch (e: Exception) {
            Log.e("PhotoOptimizer", "Failed to create optimized versions: ${e.message}", e)
            PhotoVersions("", "", 0, 0, 0, 0)
        } finally {
            originalBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                    Log.d("PhotoOptimizer", "Original bitmap recycled safely")
                }
            }
        }
    }

    private fun createThumbnail(
        originalBitmap: Bitmap,
        outputDir: File,
        filename: String
    ): String {
        val croppedBitmap = cropCenter(originalBitmap, 400, 300)
        try {
            val thumbnailFile = File(outputDir, "$filename.jpg")
            
            val thumbnailBytes = compressToTargetSize(croppedBitmap, MAX_FILE_SIZE_KB)
            FileOutputStream(thumbnailFile).use { it.write(thumbnailBytes) }
            
            Log.d("PhotoOptimizer", "Thumbnail created: ${thumbnailFile.length() / 1024}KB")
            return thumbnailFile.absolutePath
        } finally {
            if (!croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
                Log.d("PhotoOptimizer", "Cropped bitmap for thumbnail recycled safely")
            }
        }
    }

    private fun createFullSizeImage(
        originalBitmap: Bitmap,
        outputDir: File,
        filename: String
    ): String {
        val fullSizeFile = File(outputDir, "$filename.jpg")
        
        // Check if bitmap is recycled before using it
        if (originalBitmap.isRecycled) {
            Log.e("PhotoOptimizer", "Cannot create full-size image: bitmap is recycled")
            return ""
        }
        
        val fullSizeBytes = compressToTargetSize(originalBitmap, MAX_FULL_SIZE_KB)
        if (fullSizeBytes.isEmpty()) {
            Log.e("PhotoOptimizer", "Failed to compress full-size image")
            return ""
        }
        
        FileOutputStream(fullSizeFile).use { it.write(fullSizeBytes) }
        
        Log.d("PhotoOptimizer", "Full-size created: ${fullSizeFile.length() / 1024}KB")
        
        return fullSizeFile.absolutePath
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        var exifInputStream: InputStream? = null
        var bitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            inputStream = null
            
            if (bitmap == null) {
                Log.e("PhotoOptimizer", "Failed to decode bitmap from URI: $uri")
                return null
            }
            
            exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifInputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            Log.d("PhotoOptimizer", "EXIF orientation detected: $orientation")
            exifInputStream.close()
            exifInputStream = null
            
            rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    Log.d("PhotoOptimizer", "Rotating 90 degrees")
                    rotateBitmap(bitmap, 90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    Log.d("PhotoOptimizer", "Rotating 180 degrees")
                    rotateBitmap(bitmap, 180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    Log.d("PhotoOptimizer", "Rotating 270 degrees")
                    rotateBitmap(bitmap, 270f)
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    Log.d("PhotoOptimizer", "Flipping horizontally")
                    flipBitmap(bitmap, horizontal = true, vertical = false)
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    Log.d("PhotoOptimizer", "Flipping vertically")
                    flipBitmap(bitmap, horizontal = false, vertical = true)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    Log.d("PhotoOptimizer", "Transposing (rotate 90 + flip horizontal)")
                    val rotated = rotateBitmap(bitmap, 90f)
                    flipBitmap(rotated, horizontal = true, vertical = false)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    Log.d("PhotoOptimizer", "Transversing (rotate 270 + flip horizontal)")
                    val rotated = rotateBitmap(bitmap, 270f)
                    flipBitmap(rotated, horizontal = true, vertical = false)
                }
                ExifInterface.ORIENTATION_UNDEFINED -> {
                    Log.d("PhotoOptimizer", "EXIF orientation undefined - applying auto-correction")
                    // If EXIF is undefined, try to auto-detect and correct orientation
                    // Most phones take portrait photos in landscape mode, so rotate 90°
                    if (bitmap.width > bitmap.height) {
                        Log.d("PhotoOptimizer", "Landscape image detected, rotating 90° to make it portrait")
                        rotateBitmap(bitmap, 90f)
                    } else {
                        Log.d("PhotoOptimizer", "Portrait image detected, no rotation needed")
                        bitmap
                    }
                }
                else -> {
                    Log.d("PhotoOptimizer", "No rotation needed (orientation: $orientation)")
                    bitmap
                }
            }
            
            if (rotatedBitmap != bitmap) {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                    Log.d("PhotoOptimizer", "Original bitmap recycled after rotation")
                }
            }
            
            rotatedBitmap
        } catch (e: Exception) {
            Log.e("PhotoOptimizer", "Failed to load bitmap from URI: ${e.message}", e)
            null
        } finally {
            try {
                inputStream?.close()
                exifInputStream?.close()
            } catch (e: Exception) {
                Log.w("PhotoOptimizer", "Error closing streams: ${e.message}")
            }
            
            if (bitmap != null && rotatedBitmap != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
                Log.d("PhotoOptimizer", "Original bitmap recycled in finally block")
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        
        if (resizedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
            Log.d("PhotoOptimizer", "Original bitmap recycled after resizing")
        }
        
        return resizedBitmap
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
            Log.d("PhotoOptimizer", "Original bitmap recycled after rotation")
        }
        
        return rotatedBitmap
    }

    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        val scaleX = if (horizontal) -1f else 1f
        val scaleY = if (vertical) -1f else 1f
        matrix.setScale(scaleX, scaleY)
        
        val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        if (flippedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
            Log.d("PhotoOptimizer", "Original bitmap recycled after flip")
        }
        
        return flippedBitmap
    }

    private fun cropCenter(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        
        val (cropWidth, cropHeight) = if (bitmapAspect > targetAspect) {
            val newWidth = (bitmap.height * targetAspect).toInt()
            newWidth to bitmap.height
        } else {
            val newHeight = (bitmap.width / targetAspect).toInt()
            bitmap.width to newHeight
        }
        
        val cropX = (bitmap.width - cropWidth) / 2
        val cropY = (bitmap.height - cropHeight) / 2
        
        Log.d("PhotoOptimizer", "Crop dimensions: ${cropWidth}x${cropHeight} from ${bitmap.width}x${bitmap.height}")
        Log.d("PhotoOptimizer", "Crop position: ($cropX, $cropY)")
        
        val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
        
        if (croppedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
            Log.d("PhotoOptimizer", "Original bitmap recycled after cropping")
        }
        
        return croppedBitmap
    }

    private fun compressToTargetSize(bitmap: Bitmap, targetKB: Int): ByteArray {
        // Check if bitmap is recycled before using it
        if (bitmap.isRecycled) {
            Log.e("PhotoOptimizer", "Cannot compress recycled bitmap")
            return ByteArray(0)
        }
        
        var quality = 90
        val stream = ByteArrayOutputStream()
        var scaledBitmap: Bitmap? = null
        var workingBitmap = bitmap
        
        try {
            // First attempt - compress with quality reduction
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var currentSize = stream.toByteArray().size / 1024
            
            Log.d("PhotoOptimizer", "Initial compression: ${currentSize}KB (target: ${targetKB}KB)")
            
            // Reduce quality more aggressively
            while (currentSize > targetKB && quality > 20) {
                stream.reset()
                quality -= 15  // More aggressive quality reduction
                workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                currentSize = stream.toByteArray().size / 1024
                Log.d("PhotoOptimizer", "Quality $quality: ${currentSize}KB")
            }
            
            // If still too large, resize the bitmap iteratively
            var scaleFactor = 0.9f
            while (currentSize > targetKB && scaleFactor > 0.3f) {
                val scaledWidth = (bitmap.width * scaleFactor).toInt()
                val scaledHeight = (bitmap.height * scaleFactor).toInt()
                
                scaledBitmap?.recycle() // Clean up previous scaled bitmap
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                workingBitmap = scaledBitmap
                
                stream.reset()
                quality = 75  // Reset quality for resized image
                workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                currentSize = stream.toByteArray().size / 1024
                
                Log.d("PhotoOptimizer", "Scale ${scaleFactor}: ${currentSize}KB (${scaledWidth}x${scaledHeight})")
                scaleFactor -= 0.1f
            }
            
            val result = stream.toByteArray()
            Log.d("PhotoOptimizer", "✅ Final size: ${result.size / 1024}KB (target: ${targetKB}KB)")
            return result
        } catch (e: Exception) {
            Log.e("PhotoOptimizer", "Error compressing bitmap: ${e.message}")
            return ByteArray(0)
        } finally {
            scaledBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                    Log.d("PhotoOptimizer", "Scaled bitmap recycled safely")
                }
            }
        }
    }

    private fun calculateHeight(bitmap: Bitmap, targetWidth: Int): Int {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        return (targetWidth * aspectRatio).toInt()
    }

    fun deleteTemporaryPhoto(photoPath: String) {
        try {
            val file = File(photoPath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("PhotoOptimizer", "Temporary photo deleted: $deleted - $photoPath")
            }
        } catch (e: Exception) {
            Log.e("PhotoOptimizer", "Failed to delete temporary photo: ${e.message}", e)
        }
    }
}

data class PhotoVersions(
    val thumbnailPath: String,
    val fullSizePath: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val fullSizeWidth: Int,
    val fullSizeHeight: Int
) {
    val thumbnailSizeKB: Long
        get() = if (thumbnailPath.isNotEmpty()) File(thumbnailPath).length() / 1024 else 0
    
    val fullSizeSizeKB: Long
        get() = if (fullSizePath.isNotEmpty()) File(fullSizePath).length() / 1024 else 0
    
    val totalSizeKB: Long
        get() = thumbnailSizeKB + fullSizeSizeKB
}
