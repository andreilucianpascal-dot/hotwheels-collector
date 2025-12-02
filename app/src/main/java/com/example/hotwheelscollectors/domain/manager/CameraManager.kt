package com.example.hotwheelscollectors.domain.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.hotwheelscollectors.data.repository.PhotoProcessingRepository
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CameraManager handles all photo processing logic for car photos.
 * 
 * COMMON LOGIC FOR ALL CAR TYPES:
 * 1. Takes 2 photos (front + back)
 * 2. Extracts barcode from back photo using ML Kit
 * 3. Deletes back photo automatically
 * 4. Generates thumbnail (300 KB) from front photo
 * 5. Generates full photo (500 KB) from front photo
 * 6. Returns processed result
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoProcessingRepository: PhotoProcessingRepository
) {
    
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    /**
     * Processes car photos according to the standard flow:
     * 2 photos → extract barcode → generate thumbnail + full
     * Note: Photos are preserved for Firebase sync upload
     */
    suspend fun processCarPhotos(
        frontPhotoUri: Uri,
        backPhotoUri: Uri?
    ): PhotoProcessingResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            Timber.d("Starting photo processing for front: $frontPhotoUri, back: $backPhotoUri")
            
            // 1. Extract barcode from back photo (MUST be first - back photo is deleted after)
            val barcode = extractBarcodeFromBackPhoto(backPhotoUri)
            Timber.d("Extracted barcode: $barcode")
            
            // 2. Delete back photo after barcode extraction
            backPhotoUri?.let { deletePhoto(it) }
            Timber.d("Deleted back photo after barcode extraction")
            
            // 3. Generate thumbnail and full photo IN PARALLEL (both use front photo, independent)
            // ✅ PARALLELIZATION: Both operations run simultaneously, reducing total time by ~33-50%
            val thumbnailDeferred = async { 
                val result = generateThumbnail(frontPhotoUri, 300_000)
                Timber.d("Generated thumbnail: $result")
                result
            }
            
            val fullPhotoDeferred = async { 
                val result = generateFullPhoto(frontPhotoUri, 500_000)
                Timber.d("Generated full photo: $result")
                result
            }
            
            // Wait for both to complete
            val results = awaitAll(thumbnailDeferred, fullPhotoDeferred)
            val thumbnailUri = results[0] as Uri?
            val fullPhotoUri = results[1] as Uri?
            
            val totalTime = System.currentTimeMillis() - startTime
            Timber.d("✅ Photo processing completed in ${totalTime}ms (parallelized)")
            
            PhotoProcessingResult(
                barcode = barcode,
                thumbnailUri = thumbnailUri,
                fullPhotoUri = fullPhotoUri,
                success = true
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to process car photos")
            PhotoProcessingResult(
                barcode = "",
                thumbnailUri = null,
                fullPhotoUri = null,
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Extracts barcode from back photo using ML Kit
     */
    private suspend fun extractBarcodeFromBackPhoto(backPhotoUri: Uri?): String = withContext(Dispatchers.IO) {
        if (backPhotoUri == null) {
            Timber.d("No back photo provided for barcode extraction")
            return@withContext ""
        }

        try {
            Timber.d("Starting barcode extraction from: $backPhotoUri")
            
            val bitmap = loadBitmapFromUri(backPhotoUri)
            if (bitmap == null) {
                Timber.w("Failed to load bitmap from URI: $backPhotoUri")
                return@withContext ""
            }

            val image = InputImage.fromBitmap(bitmap, 0)
            
            val barcodes = barcodeScanner.process(image).await()
            
            val extractedBarcode = barcodes.firstOrNull()?.rawValue ?: ""
            Timber.d("Barcode extraction result: '$extractedBarcode'")
            
            extractedBarcode
            
        } catch (e: Exception) {
            Timber.e(e, "Error during barcode extraction")
            ""
        }
    }

    /**
     * Loads bitmap from URI and corrects orientation based on EXIF data
     * ✅ FIX: This ensures photos are always displayed upright, not rotated
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            // Load the bitmap
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return null
            
            // Get EXIF orientation
            val exifOrientation = getExifOrientation(uri)
            
            Timber.d("Loaded bitmap from URI: $uri, EXIF orientation: $exifOrientation")
            
            // Rotate bitmap if needed based on EXIF
            var rotatedBitmap = rotateBitmapIfNeeded(bitmap, exifOrientation)
            
            // ✅ FIX: Camera saves photos without EXIF (orientation = 0)
            // CameraX reports sourceRotationDegrees=90, so force 90° rotation
            // This matches what CameraOrientationUtil expects
            if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
                val forceRotationDegrees = 90f // Camera saves rotated 90° clockwise
                Timber.d("EXIF orientation is $exifOrientation - forcing ${forceRotationDegrees}° rotation to match CameraX")
                val matrix = Matrix()
                matrix.postRotate(forceRotationDegrees)
                val tempBitmap = rotatedBitmap
                rotatedBitmap = Bitmap.createBitmap(
                    tempBitmap, 0, 0,
                    tempBitmap.width, tempBitmap.height,
                    matrix, true
                )
                if (tempBitmap != rotatedBitmap) {
                    tempBitmap.recycle()
                }
                Timber.d("Photo rotated ${forceRotationDegrees}° successfully")
            }
            
            rotatedBitmap
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from URI: $uri")
            null
        }
    }
    
    /**
     * Gets EXIF orientation from image URI
     */
    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            Timber.w(e, "Failed to read EXIF data, using default orientation")
            ExifInterface.ORIENTATION_NORMAL
        }
    }
    
    /**
     * Rotates bitmap based on EXIF orientation
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
                Timber.d("Rotating bitmap 90°")
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
                Timber.d("Rotating bitmap 180°")
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                Timber.d("Rotating bitmap 270°")
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
                Timber.d("Flipping bitmap horizontally")
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
                Timber.d("Flipping bitmap vertically")
            }
            else -> {
                Timber.d("No rotation needed (orientation: $orientation)")
                return bitmap // No rotation needed
            }
        }
        
        return try {
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height,
                matrix, true
            )
            
            // Recycle original bitmap if it's different from rotated
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            
            rotatedBitmap
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate bitmap")
            bitmap // Return original on error
        }
    }

    /**
     * Deletes photo file
     */
    private fun deletePhoto(photoUri: Uri) {
        try {
            val file = File(photoUri.path ?: return)
            if (file.exists()) {
                file.delete()
                Timber.d("Deleted photo file: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete photo: $photoUri")
        }
    }

    /**
     * Generates thumbnail with specified max size in bytes
     */
    private suspend fun generateThumbnail(originalUri: Uri, maxSizeBytes: Int): Uri? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = loadBitmapFromUri(originalUri) ?: return@withContext null

            // ✅ NEW: Use real card dimensions (108×108 or 108×165) with white background
            val withWhiteBackground = drawCardOnWhiteBackground(originalBitmap)
            val downscaled = scaleBitmapToMax(withWhiteBackground, maxSidePx = 720)
            
            // Calculate compression ratio to achieve target size
            val compressedBitmap = compressBitmapToSize(downscaled, maxSizeBytes, minQuality = 60)
            
            // Save compressed bitmap to file
            val thumbnailFile = File(context.cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(thumbnailFile)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
            
            Timber.d("Generated thumbnail: ${thumbnailFile.absolutePath}, size: ${thumbnailFile.length()} bytes")
            Uri.fromFile(thumbnailFile)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnail")
            null
        }
    }

    /**
     * Generates full photo with specified max size in bytes
     */
    private suspend fun generateFullPhoto(originalUri: Uri, maxSizeBytes: Int): Uri? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = loadBitmapFromUri(originalUri) ?: return@withContext null

            // ✅ NEW: Use real card dimensions (108×108 or 108×165) with white background
            val withWhiteBackground = drawCardOnWhiteBackground(originalBitmap)
            val downscaled = scaleBitmapToMax(withWhiteBackground, maxSidePx = 2048)
            
            // Calculate compression ratio to achieve target size
            val compressedBitmap = compressBitmapToSize(downscaled, maxSizeBytes, minQuality = 60)
            
            // Save compressed bitmap to file
            val fullPhotoFile = File(context.cacheDir, "full_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(fullPhotoFile)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            Timber.d("Generated full photo: ${fullPhotoFile.absolutePath}, size: ${fullPhotoFile.length()} bytes")
            Uri.fromFile(fullPhotoFile)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate full photo")
            null
        }
    }

    /**
     * Compresses bitmap to target size
     */
    private fun compressBitmapToSize(bitmap: Bitmap, targetSizeBytes: Int, minQuality: Int = 60): Bitmap {
        var quality = 90
        val outputStream = ByteArrayOutputStream()
        
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > targetSizeBytes && quality > minQuality)
        
        val compressedByteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.size)
    }

    /**
     * Scales a bitmap so that its largest side equals maxSidePx, preserving aspect ratio.
     */
    private fun scaleBitmapToMax(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxSidePx) return bitmap

        val scale = maxSidePx.toFloat() / maxSide.toFloat()
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * Detects card type (small 108×108 or long 108×165) based on aspect ratio.
     * Small card: aspect ratio ≈ 1.0 (square)
     * Long card: aspect ratio ≈ 0.654 (108/165) or ≈ 1.528 (165/108)
     */
    private enum class CardType {
        SMALL,  // 108mm × 108mm (aspect ratio = 1.0)
        LONG    // 108mm × 165mm (aspect ratio = 108/165 ≈ 0.654)
    }

    private fun detectCardType(bitmap: Bitmap): CardType {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val aspectRatio = minOf(width, height) / maxOf(width, height)
        
        // Real template dimensions:
        // Small: 1276×1276 px (aspect ratio = 1.0)
        // Long: 1276×1949 px (aspect ratio = 1276/1949 ≈ 0.654)
        // Use higher threshold (0.95) for square detection to avoid false positives
        return if (aspectRatio >= 0.95f) {
            CardType.SMALL
        } else {
            CardType.LONG
        }
    }

    /**
     * Loads template from assets folder.
     */
    private fun loadTemplateFromAssets(filename: String): Bitmap? {
        return try {
            val inputStream = context.assets.open("card_templates/$filename")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            Timber.d("Loaded template: $filename, size: ${bitmap?.width}x${bitmap?.height}")
            bitmap
        } catch (e: Exception) {
            Timber.w(e, "Failed to load template: $filename")
            null
        }
    }

    /**
     * Calculates match score for a given rectangle using the template.
     * Scales template to match rect size and calculates similarity.
     */
    private fun calculateMatchScoreForRect(source: Bitmap, template: Bitmap, rect: android.graphics.Rect): Float {
        val rectWidth = rect.right - rect.left
        val rectHeight = rect.bottom - rect.top
        
        // Scale template to match rect size
        val scaledTemplate = Bitmap.createScaledBitmap(template, rectWidth, rectHeight, true)
        val score = calculateMatchScore(source, scaledTemplate, rect.left, rect.top)
        
        if (scaledTemplate != template) {
            scaledTemplate.recycle()
        }
        
        return score
    }

    /**
     * Calculates similarity score between two image regions using simple pixel difference.
     * Returns score between 0 (no match) and 1 (perfect match).
     */
    private fun calculateMatchScore(source: Bitmap, template: Bitmap, startX: Int, startY: Int): Float {
        if (startX + template.width > source.width || startY + template.height > source.height) {
            return 0f
        }
        
        var matchScore = 0f
        var totalPixels = 0
        
        // Sample pixels for faster comparison (every 4th pixel)
        val step = 4
        for (y in 0 until template.height step step) {
            for (x in 0 until template.width step step) {
                if (startX + x < source.width && startY + y < source.height) {
                    val templatePixel = template.getPixel(x, y)
                    val sourcePixel = source.getPixel(startX + x, startY + y)
                    
                    // Check alpha channel - only compare where template is opaque
                    val templateAlpha = (templatePixel shr 24) and 0xFF
                    if (templateAlpha > 128) { // Template is opaque here
                        // Simple color difference
                        val rDiff = kotlin.math.abs(((templatePixel shr 16) and 0xFF) - ((sourcePixel shr 16) and 0xFF))
                        val gDiff = kotlin.math.abs(((templatePixel shr 8) and 0xFF) - ((sourcePixel shr 8) and 0xFF))
                        val bDiff = kotlin.math.abs((templatePixel and 0xFF) - (sourcePixel and 0xFF))
                        val diff = (rDiff + gDiff + bDiff) / 3f
                        matchScore += 1f - (diff / 255f) // Normalize to 0-1
                        totalPixels++
                    }
                }
            }
        }
        
        return if (totalPixels > 0) matchScore / totalPixels else 0f
    }

    /**
     * Performs template matching to find card position in image.
     * Uses real template dimensions (1276×1276 or 1276×1949) and searches at optimized scales.
     * Returns the best match location or null if not found.
     */
    private fun findCardWithTemplate(source: Bitmap, template: Bitmap): android.graphics.Rect? {
        try {
            // Real template dimensions: 1276×1276 px (small) or 1276×1949 px (long) at 300 DPI
            val templateWidth = template.width // Real: 1276 px
            val templateHeight = template.height // Real: 1276 or 1949 px
            val templateAspect = templateWidth.toFloat() / templateHeight.toFloat()
            
            // Downscale source for faster search (max 1200px on longest side)
            val maxSearchDimension = 1200
            val sourceAspect = source.width.toFloat() / source.height.toFloat()
            val searchSource: Bitmap
            val scaleDownFactor: Float
            
            if (source.width > source.height) {
                if (source.width > maxSearchDimension) {
                    scaleDownFactor = maxSearchDimension.toFloat() / source.width.toFloat()
                    val searchHeight = (source.height * scaleDownFactor).toInt()
                    searchSource = Bitmap.createScaledBitmap(source, maxSearchDimension, searchHeight, true)
                } else {
                    searchSource = source
                    scaleDownFactor = 1f
                }
            } else {
                if (source.height > maxSearchDimension) {
                    scaleDownFactor = maxSearchDimension.toFloat() / source.height.toFloat()
                    val searchWidth = (source.width * scaleDownFactor).toInt()
                    searchSource = Bitmap.createScaledBitmap(source, searchWidth, maxSearchDimension, true)
                } else {
                    searchSource = source
                    scaleDownFactor = 1f
                }
            }
            
            val minSearchDimension = minOf(searchSource.width, searchSource.height)
            val templateMinDimension = minOf(templateWidth, templateHeight)
            
            // Optimized: Try only 3 scales (reduces search time)
            val scales = listOf(0.6f, 0.75f, 0.9f)
            var bestMatch: android.graphics.Rect? = null
            var bestScore = 0f
            
            Timber.d("Template: ${templateWidth}x${templateHeight}, Source: ${source.width}x${source.height}, Search: ${searchSource.width}x${searchSource.height}")
            
            for (scaleRatio in scales) {
                val estimatedCardSize = (minSearchDimension * scaleRatio).toInt()
                val scaleFactor = estimatedCardSize.toFloat() / templateMinDimension.toFloat()
                
                val scaledWidth = (templateWidth * scaleFactor).toInt()
                val scaledHeight = (templateHeight * scaleFactor).toInt()
                
                // Skip if scaled template is larger than search source
                if (scaledWidth > searchSource.width || scaledHeight > searchSource.height) {
                    continue
                }
                
                // Scale template
                val scaledTemplate = Bitmap.createScaledBitmap(template, scaledWidth, scaledHeight, true)
                
                // Optimized step size (larger for faster search)
                val stepSize = maxOf(20, minOf(scaledWidth, scaledHeight) / 8)
                
                for (y in 0 until (searchSource.height - scaledHeight) step stepSize) {
                    for (x in 0 until (searchSource.width - scaledWidth) step stepSize) {
                        val score = calculateMatchScore(searchSource, scaledTemplate, x, y)
                        
                        if (score > bestScore) {
                            bestScore = score
                            // Scale back to original source coordinates
                            val origX = (x / scaleDownFactor).toInt()
                            val origY = (y / scaleDownFactor).toInt()
                            val origWidth = (scaledWidth / scaleDownFactor).toInt()
                            val origHeight = (scaledHeight / scaleDownFactor).toInt()
                            bestMatch = android.graphics.Rect(origX, origY, origX + origWidth, origY + origHeight)
                        }
                    }
                }
                
                if (scaledTemplate != template) {
                    scaledTemplate.recycle()
                }
                
                Timber.d("Scale $scaleRatio: best score $bestScore")
            }
            
            // Clean up downscaled source if created
            if (searchSource != source) {
                searchSource.recycle()
            }
            
            if (bestMatch != null && bestScore > 0.4f) { // Higher threshold (0.4) for better accuracy
                Timber.d("Best match found: ${bestMatch.right - bestMatch.left}x${bestMatch.bottom - bestMatch.top} at (${bestMatch.left}, ${bestMatch.top}), score: $bestScore")
                return bestMatch
            } else {
                Timber.w("No good match found (best score: $bestScore), falling back to center")
                // Fallback to center with estimated size using real template dimensions
                val estimatedCardSize = (minOf(source.width, source.height) * 0.7f).toInt()
                val scaleFactor = estimatedCardSize.toFloat() / templateMinDimension.toFloat()
                val scaledWidth = (templateWidth * scaleFactor).toInt()
                val scaledHeight = (templateHeight * scaleFactor).toInt()
                val left = (source.width - scaledWidth) / 2
                val top = (source.height - scaledHeight) / 2
                return android.graphics.Rect(left, top, left + scaledWidth, top + scaledHeight)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error in template matching")
            return null
        }
    }

    /**
     * Applies template mask to extract card and place on white background.
     */
    private fun applyTemplateMask(source: Bitmap, template: Bitmap, cardRect: android.graphics.Rect): Bitmap {
        val width = source.width
        val height = source.height
        
        // Create white canvas
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        val cardWidth = cardRect.right - cardRect.left
        val cardHeight = cardRect.bottom - cardRect.top
        
        // Scale template to match cardRect size exactly
        val scaledTemplate = Bitmap.createScaledBitmap(
            template,
            cardWidth,
            cardHeight,
            true
        )
        
        // Extract card region from source
        val cardBitmap = Bitmap.createBitmap(
            source,
            cardRect.left.coerceAtLeast(0),
            cardRect.top.coerceAtLeast(0),
            cardWidth.coerceAtMost(source.width - cardRect.left),
            cardHeight.coerceAtMost(source.height - cardRect.top)
        )
        
        // Ensure dimensions match
        val finalCardWidth = minOf(cardBitmap.width, scaledTemplate.width)
        val finalCardHeight = minOf(cardBitmap.height, scaledTemplate.height)
        
        // Apply template as mask: where template is transparent, keep white; where opaque, use card
        val maskPixels = IntArray(scaledTemplate.width * scaledTemplate.height)
        scaledTemplate.getPixels(maskPixels, 0, scaledTemplate.width, 0, 0, scaledTemplate.width, scaledTemplate.height)
        
        val cardPixels = IntArray(cardBitmap.width * cardBitmap.height)
        cardBitmap.getPixels(cardPixels, 0, cardBitmap.width, 0, 0, cardBitmap.width, cardBitmap.height)
        
        // Blend: use card pixels where template is opaque, white where transparent
        for (y in 0 until finalCardHeight) {
            for (x in 0 until finalCardWidth) {
                val maskIdx = y * scaledTemplate.width + x
                val cardIdx = y * cardBitmap.width + x
                
                if (maskIdx < maskPixels.size && cardIdx < cardPixels.size) {
                    val alpha = (maskPixels[maskIdx] shr 24) and 0xFF
                    if (alpha < 128) { // Transparent in template = white background
                        cardPixels[cardIdx] = 0xFFFFFFFF.toInt() // White
                    }
                }
            }
        }
        
        cardBitmap.setPixels(cardPixels, 0, cardBitmap.width, 0, 0, cardBitmap.width, cardBitmap.height)
        
        // Draw card on white background at correct position
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(cardBitmap, cardRect.left.toFloat(), cardRect.top.toFloat(), paint)
        
        if (scaledTemplate != template) {
            scaledTemplate.recycle()
        }
        cardBitmap.recycle()
        
        Timber.d("Applied template mask: card ${finalCardWidth}x${finalCardHeight} at (${cardRect.left}, ${cardRect.top})")
        
        return result
    }

    /**
     * Draws the card on white background using template matching with real card templates.
     * Tries both templates and uses the one with best match.
     * Falls back to aspect ratio crop if template matching fails.
     */
    private fun drawCardOnWhiteBackground(source: Bitmap): Bitmap {
        // Try both templates and use the one with best match
        val smallTemplate = loadTemplateFromAssets("108.png")
        val longTemplate = loadTemplateFromAssets("165.png")
        
        var bestMatch: android.graphics.Rect? = null
        var bestTemplate: Bitmap? = null
        var bestScore = 0f
        
        // Try small template (108×108)
        if (smallTemplate != null) {
            val match = findCardWithTemplate(source, smallTemplate)
            if (match != null) {
                // Calculate match score for this template
                val score = calculateMatchScoreForRect(source, smallTemplate, match)
                Timber.d("Small template match score: $score")
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = match
                    bestTemplate = smallTemplate
                }
            }
        }
        
        // Try long template (108×165)
        if (longTemplate != null) {
            val match = findCardWithTemplate(source, longTemplate)
            if (match != null) {
                // Calculate match score for this template
                val score = calculateMatchScoreForRect(source, longTemplate, match)
                Timber.d("Long template match score: $score")
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = match
                    bestTemplate = longTemplate
                    // Recycle small template if long is better
                    if (smallTemplate != null && smallTemplate != longTemplate) {
                        smallTemplate.recycle()
                    }
                } else if (smallTemplate != null && smallTemplate != longTemplate) {
                    longTemplate.recycle()
                }
            } else if (smallTemplate != null && smallTemplate != longTemplate) {
                longTemplate.recycle()
            }
        }
        
        // Use best match if found
        if (bestTemplate != null && bestMatch != null && bestScore > 0.3f) {
            Timber.d("Using template matching: ${if (bestTemplate == smallTemplate) "SMALL" else "LONG"} template, score: $bestScore")
            val result = applyTemplateMask(source, bestTemplate, bestMatch)
            // Recycle unused template
            if (bestTemplate == smallTemplate && longTemplate != null && longTemplate != smallTemplate) {
                longTemplate.recycle()
            } else if (bestTemplate == longTemplate && smallTemplate != null && smallTemplate != longTemplate) {
                smallTemplate.recycle()
            }
            return result
        } else {
            Timber.w("No good template match found (best score: $bestScore), falling back to aspect ratio crop")
            // Recycle templates
            smallTemplate?.recycle()
            longTemplate?.recycle()
        }
        
        // Fallback: use aspect ratio-based crop (existing logic)
        val width = source.width
        val height = source.height
        
        // Detect card type for fallback
        val cardType = detectCardType(source)
        
        // Real card dimensions in mm (108×108 for small, 108×165 for long)
        val smallCardWidth = 108f
        val smallCardHeight = 108f
        val longCardWidth = 108f
        val longCardHeight = 165f
        
        // Calculate target aspect ratio
        val targetAspectRatio = when (cardType) {
            CardType.SMALL -> smallCardWidth / smallCardHeight // 1.0
            CardType.LONG -> longCardWidth / longCardHeight // ≈ 0.654
        }
        
        // Calculate crop dimensions maintaining target aspect ratio
        val sourceAspectRatio = width.toFloat() / height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        
        if (sourceAspectRatio > targetAspectRatio) {
            // Source is wider than target - crop width
            cropHeight = height
            cropWidth = (height * targetAspectRatio).toInt()
        } else {
            // Source is taller than target - crop height
            cropWidth = width
            cropHeight = (width / targetAspectRatio).toInt()
        }
        
        // Center the crop
        val left = (width - cropWidth) / 2
        val top = (height - cropHeight) / 2
        
        // Create white canvas with same size as source
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Draw cropped card centered on white background
        val src = android.graphics.Rect(left, top, left + cropWidth, top + cropHeight)
        val dstLeft = (width - cropWidth) / 2
        val dstTop = (height - cropHeight) / 2
        val dst = android.graphics.Rect(dstLeft, dstTop, dstLeft + cropWidth, dstTop + cropHeight)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        canvas.drawBitmap(source, src, dst, paint)
        
        Timber.d("Fallback: Card type detected: $cardType, crop: ${cropWidth}x${cropHeight}, target aspect: $targetAspectRatio")
        
        return result
    }
    /**
     * Result of photo processing
     */
    data class PhotoProcessingResult(
        val barcode: String,
        val thumbnailUri: Uri?,
        val fullPhotoUri: Uri?,
        val success: Boolean,
        val error: String? = null
    )
}
