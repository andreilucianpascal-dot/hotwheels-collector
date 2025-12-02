// ImageUtils.kt
package com.example.hotwheelscollectors.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtils @Inject constructor(
    private val context: Context
) {
    suspend fun getImageSize(uri: Uri): Pair<Int, Int> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            Pair(options.outWidth, options.outHeight)
        } ?: Pair(0, 0)
    }

    suspend fun rotateImageIfNeeded(file: File): File = withContext(Dispatchers.IO) {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        if (orientation == ExifInterface.ORIENTATION_NORMAL) return@withContext file

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        val output = File.createTempFile(
            "rotated_${System.currentTimeMillis()}",
            ".jpg"
        )

        FileOutputStream(output).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        bitmap.recycle()
        rotatedBitmap.recycle()

        output
    }

    suspend fun createThumbnail(
        uri: Uri,
        maxSize: Int = 300
    ): File = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("Could not decode image")

        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (targetWidth, targetHeight) = if (bitmap.width > bitmap.height) {
            Pair(maxSize, (maxSize / ratio).toInt())
        } else {
            Pair((maxSize * ratio).toInt(), maxSize)
        }

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            targetWidth,
            targetHeight,
            true
        )

        val output = File.createTempFile(
            "thumb_${System.currentTimeMillis()}",
            ".jpg"
        )

        FileOutputStream(output).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        bitmap.recycle()
        scaledBitmap.recycle()

        output
    }
}