// app/src/main/java/com/example/hotwheelscollectors/utils/ImageCropper.kt
package com.example.hotwheelscollectors.utils

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.*

object ImageCropper {

    fun cropCardArea(bitmap: Bitmap): Bitmap {
        try {
            // Convert to ARGB_8888 if needed
            val sourceBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }

            // Detect card boundaries using edge detection
            val cardBounds = detectCardBounds(sourceBitmap)

            if (cardBounds == null) {
                return bitmap // Return original if no card found
            }

            // Crop the detected area
            return cropBitmap(sourceBitmap, cardBounds)

        } catch (e: Exception) {
            // Return original bitmap if processing fails
            return bitmap
        }
    }

    private fun detectCardBounds(bitmap: Bitmap): RectF? {
        val width = bitmap.width
        val height = bitmap.height

        // Create a scaled down version for faster processing
        val scale = 0.5f
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // Convert to grayscale and detect edges
        val edgeMap = detectEdges(scaledBitmap)

        // Find the largest rectangular region
        val cardRegion = findLargestRectangle(edgeMap, scaledWidth, scaledHeight)

        // Scale back to original dimensions
        return if (cardRegion != null) {
            RectF(
                cardRegion.left / scale,
                cardRegion.top / scale,
                cardRegion.right / scale,
                cardRegion.bottom / scale
            )
        } else null
    }

    private fun detectEdges(bitmap: Bitmap): Array<BooleanArray> {
        val width = bitmap.width
        val height = bitmap.height
        val edgeMap = Array(height) { BooleanArray(width) }

        // Simple edge detection using gradient magnitude
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = bitmap.getPixel(x, y)
                val leftPixel = bitmap.getPixel(x - 1, y)
                val rightPixel = bitmap.getPixel(x + 1, y)
                val topPixel = bitmap.getPixel(x, y - 1)
                val bottomPixel = bitmap.getPixel(x, y + 1)

                val gx = (getGrayscale(rightPixel) - getGrayscale(leftPixel)) / 2
                val gy = (getGrayscale(bottomPixel) - getGrayscale(topPixel)) / 2

                val gradientMagnitude = sqrt((gx * gx + gy * gy).toDouble())
                edgeMap[y][x] = gradientMagnitude > 30.0
            }
        }

        return edgeMap
    }

    private fun getGrayscale(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (r * 0.299 + g * 0.587 + b * 0.114).toInt()
    }

    private fun findLargestRectangle(edgeMap: Array<BooleanArray>, width: Int, height: Int): Rect? {
        var maxArea = 0
        var bestRect: Rect? = null

        // Find the largest rectangular region with minimal edge density
        for (startY in 0 until height - 50) {
            for (startX in 0 until width - 80) {
                for (endY in startY + 50 until height) {
                    for (endX in startX + 80 until width) {
                        val area = (endX - startX) * (endY - startY)
                        if (area > maxArea) {
                            val edgeDensity = calculateEdgeDensity(edgeMap, startX, startY, endX, endY)
                            if (edgeDensity < 0.1) { // Low edge density indicates card area
                                maxArea = area
                                bestRect = Rect(startX, startY, endX, endY)
                            }
                        }
                    }
                }
            }
        }

        return bestRect
    }

    private fun calculateEdgeDensity(edgeMap: Array<BooleanArray>, startX: Int, startY: Int, endX: Int, endY: Int): Double {
        var edgeCount = 0
        var totalPixels = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                if (edgeMap[y][x]) {
                    edgeCount++
                }
                totalPixels++
            }
        }

        return if (totalPixels > 0) edgeCount.toDouble() / totalPixels else 1.0
    }

    private fun cropBitmap(bitmap: Bitmap, bounds: RectF): Bitmap {
        val left = max(0, bounds.left.toInt())
        val top = max(0, bounds.top.toInt())
        val right = min(bitmap.width, bounds.right.toInt())
        val bottom = min(bitmap.height, bounds.bottom.toInt())

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            return bitmap
        }

        // Create cropped bitmap
        val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

        // Apply perspective correction if needed
        return applyPerspectiveCorrection(croppedBitmap)
    }

    private fun applyPerspectiveCorrection(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Create a new bitmap with corrected perspective
        val correctedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(correctedBitmap)

        // Simple perspective correction using matrix transformation
        val matrix = Matrix()

        // Apply slight perspective correction
        val src = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )

        val dst = floatArrayOf(
            10f, 10f,
            (width - 10).toFloat(), 5f,
            (width - 5).toFloat(), (height - 10).toFloat(),
            5f, (height - 5).toFloat()
        )

        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        canvas.drawBitmap(bitmap, matrix, Paint())

        return correctedBitmap
    }

    // Utility function to crop image to specific aspect ratio
    fun cropToAspectRatio(bitmap: Bitmap, targetAspectRatio: Float): Bitmap {
        val currentAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        return if (abs(currentAspectRatio - targetAspectRatio) < 0.1f) {
            bitmap // Already close to target ratio
        } else {
            val newWidth: Int
            val newHeight: Int

            if (currentAspectRatio > targetAspectRatio) {
                // Current is wider, crop width
                newHeight = bitmap.height
                newWidth = (bitmap.height * targetAspectRatio).toInt()
            } else {
                // Current is taller, crop height
                newWidth = bitmap.width
                newHeight = (bitmap.width / targetAspectRatio).toInt()
            }

            val x = (bitmap.width - newWidth) / 2
            val y = (bitmap.height - newHeight) / 2

            Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
        }
    }

    // Utility function to resize bitmap
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        if (scale >= 1.0f) {
            return bitmap // No need to resize
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Utility function to add padding around bitmap
    fun addPadding(bitmap: Bitmap, padding: Int, backgroundColor: Int = Color.WHITE): Bitmap {
        val newWidth = bitmap.width + (padding * 2)
        val newHeight = bitmap.height + (padding * 2)

        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)

        // Fill background
        canvas.drawColor(backgroundColor)

        // Draw original bitmap in center
        canvas.drawBitmap(bitmap, padding.toFloat(), padding.toFloat(), null)

        return newBitmap
    }
}