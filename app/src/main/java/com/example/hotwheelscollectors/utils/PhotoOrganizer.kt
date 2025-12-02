// app/src/main/java/com/example/hotwheelscollectors/utils/PhotoOrganizer.kt

package com.example.hotwheelscollectors.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

class PhotoOrganizer(private val context: Context) {
    fun processCarPhotos(
        frontPhoto: Bitmap,
        backPhoto: Bitmap,
        model: String,
        brand: String,
        isPremium: Boolean
    ): CarPhotos {
        // Create directory structure
        val baseDir = context.getExternalFilesDir("car_photos")
        val carDir = File(baseDir, "${brand}_${model}_${System.currentTimeMillis()}")
        carDir.mkdirs()

        // Crop card areas
        val croppedFront = ImageCropper.cropCardArea(frontPhoto)
        val croppedBack = ImageCropper.cropCardArea(backPhoto)

        // Extract barcode region
        val barcodeCrop = extractBarcodeRegion(croppedBack)

        // Create combined view
        val combined = createCombinedView(croppedFront, croppedBack)

        // Save all photos
        val frontPath = saveBitmap(croppedFront, File(carDir, "front.jpg"))
        val backPath = saveBitmap(croppedBack, File(carDir, "back.jpg"))
        val barcodePath = saveBitmap(barcodeCrop, File(carDir, "barcode.jpg"))
        val combinedPath = saveBitmap(combined, File(carDir, "combined.jpg"))

        return CarPhotos(
            frontPath = frontPath,
            backPath = backPath,
            barcodePath = barcodePath,
            combinedPath = combinedPath
        )
    }

    private fun extractBarcodeRegion(backPhoto: Bitmap): Bitmap {
        // Barcode is usually in the bottom third of the card
        val barcodeRect = Rect(
            0,
            (backPhoto.height * 0.67).toInt(),
            backPhoto.width,
            backPhoto.height
        )

        return Bitmap.createBitmap(
            backPhoto,
            barcodeRect.left,
            barcodeRect.top,
            barcodeRect.width(),
            barcodeRect.height()
        )
    }

    private fun createCombinedView(front: Bitmap, back: Bitmap): Bitmap {
        val width = front.width + back.width + PADDING
        val height = maxOf(front.height, back.height)

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint()

            // Draw front photo
            canvas.drawBitmap(front, 0f, 0f, paint)

            // Draw back photo
            canvas.drawBitmap(
                back,
                (front.width + PADDING).toFloat(),
                0f,
                paint
            )
        }
    }

    private fun saveBitmap(bitmap: Bitmap, file: File): String {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    companion object {
        private const val PADDING = 16 // pixels
    }
}

data class CarPhotos(
    val frontPath: String,
    val backPath: String,
    val barcodePath: String,
    val combinedPath: String
)