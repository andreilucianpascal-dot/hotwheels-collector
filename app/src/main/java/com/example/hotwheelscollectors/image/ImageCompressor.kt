// ImageCompressor.kt
package com.example.hotwheelscollectors.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class ImageCompressor @Inject constructor() {
    suspend fun compress(input: File, type: ImageType): File = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(input.path)
        val (targetWidth, targetHeight) = calculateTargetSize(
            width = bitmap.width,
            height = bitmap.height,
            type = type
        )

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            targetWidth,
            targetHeight,
            true
        )

        val output = File.createTempFile(
            "compressed_${System.currentTimeMillis()}",
            type.extension
        )

        FileOutputStream(output).use { out ->
            scaledBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                getQualityForType(type),
                out
            )
        }

        bitmap.recycle()
        scaledBitmap.recycle()

        output
    }

    private fun calculateTargetSize(
        width: Int,
        height: Int,
        type: ImageType
    ): Pair<Int, Int> {
        return when (type) {
            ImageType.THUMBNAIL -> {
                // Thumbnail pentru mașini: 400x300px fix
                Pair(400, 300)
            }
            ImageType.PROFILE -> {
                // Profile pentru utilizatori: păstrăm 500px
                val maxSize = 500
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    val targetWidth = maxSize
                    val targetHeight = (targetWidth / ratio).roundToInt()
                    Pair(targetWidth, targetHeight)
                } else {
                    val targetHeight = maxSize
                    val targetWidth = (targetHeight * ratio).roundToInt()
                    Pair(targetWidth, targetHeight)
                }
            }
            else -> {
                // Full size pentru mașini: 1280px max
                val maxSize = 1280
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    val targetWidth = maxSize
                    val targetHeight = (targetWidth / ratio).roundToInt()
                    Pair(targetWidth, targetHeight)
                } else {
                    val targetHeight = maxSize
                    val targetWidth = (targetHeight * ratio).roundToInt()
                    Pair(targetWidth, targetHeight)
                }
            }
        }
    }

    private fun getQualityForType(type: ImageType): Int = when (type) {
        ImageType.THUMBNAIL -> 80
        ImageType.PROFILE -> 85
        else -> 90
    }
}