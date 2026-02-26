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
    
    // TFLite/OpenCV DEZACTIVAT COMPLET - folosim doar crop simplu
    // private val tfliteManager = TFLiteSegmentationManager(context)

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
            
            // 3. Process ONCE with TFLite (at high resolution), then downscale for thumbnail
            // ✅ OPTIMIZATION: TFLite runs only once, thumbnail is derived from full photo
            // ✅ CONSISTENCY: Thumbnail and zoom show EXACTLY the same image (just scaled)
            
            val originalBitmap = loadBitmapFromUri(frontPhotoUri)
            if (originalBitmap == null) {
                Timber.e("Failed to load original bitmap")
                return@withContext PhotoProcessingResult(
                    barcode = barcode,
                    thumbnailUri = null,
                    fullPhotoUri = null,
                    success = false,
                    error = "Failed to load image"
                )
            }
            
            // STEP 1: Process FULL photo with TFLite (2048px max)
            val preScaled = scaleBitmapToMax(originalBitmap, maxSidePx = 2048)
            Timber.d("📸 PROCESSING: ${preScaled.width}x${preScaled.height}")
            android.util.Log.d("CameraManager", "📸 PROCESSING: ${preScaled.width}x${preScaled.height}")
            val processedBitmap = drawCardOnWhiteBackground(preScaled)
            
            // STEP 2: Generate FULL photo from processed bitmap
            val fullPhotoUri = saveBitmapToFile(processedBitmap, "full_photo", 500_000, 90)
            Timber.d("Generated full photo: $fullPhotoUri")
            
            // STEP 3: Generate THUMBNAIL from SAME processed bitmap (no TFLite again)
            val thumbnailBitmap = scaleBitmapToMax(processedBitmap, maxSidePx = 720)
            val thumbnailUri = saveBitmapToFile(thumbnailBitmap, "thumbnail", 300_000, 85)
            Timber.d("Generated thumbnail: $thumbnailUri")
            
            // Cleanup
            originalBitmap.recycle()
            preScaled.recycle()
            processedBitmap.recycle()
            thumbnailBitmap.recycle()
            
            val totalTime = System.currentTimeMillis() - startTime
            Timber.d("✅ Photo processing completed in ${totalTime}ms (TFLite run once, thumbnail derived)")
            
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
     * Saves a bitmap to file with compression to target size
     * Optimized: single compression pass, no decode/re-encode
     */
    private fun saveBitmapToFile(
        bitmap: Bitmap,
        prefix: String,
        maxSizeBytes: Int,
        initialQuality: Int = 90
    ): Uri? {
        try {
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            
            // Single-pass compression directly to file
            var quality = initialQuality
            val minQuality = 60
            
            do {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                
                if (file.length() <= maxSizeBytes || quality <= minQuality) {
                    break
                }
                quality -= 10
            } while (true)
            
            Timber.d("Saved ${prefix}: ${file.absolutePath}, size: ${file.length()} bytes, quality: $quality")
            return Uri.fromFile(file)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to save bitmap to file (prefix: $prefix)")
            return null
        }
    }


    /**
     * Scales a bitmap so that its largest side equals maxSidePx, preserving aspect ratio.
     * Returns a NEW bitmap (even if no scaling needed) to avoid double-recycle issues.
     */
    private fun scaleBitmapToMax(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxSidePx) {
            // Return a copy to avoid double-recycle when caller does bitmap.recycle()
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        }

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
    /**
     * DEPRECATED - Funcții vechi de template matching/PNG masks
     * Vor fi șterse după ce verificăm că TFLite funcționează corect
     */
    
    // CardType enum - nu mai este necesar
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
     * TEST: Applies PNG mask to extract card and place on white background.
     * This is a test function to verify mask functionality before TFLite integration.
     */
    suspend fun applyPngMaskTest(source: Bitmap, maskPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (maskPath == null) return@withContext null
        
        try {
            val maskFile = File(maskPath)
            if (!maskFile.exists()) {
                Timber.w("Mask file not found: $maskPath")
                return@withContext null
            }
            
            val maskBitmap = BitmapFactory.decodeFile(maskFile.absolutePath)
            if (maskBitmap == null) {
                Timber.w("Failed to decode mask: $maskPath")
                return@withContext null
            }
            
            // Resize mask to match source dimensions
            val resizedMask = Bitmap.createScaledBitmap(
                maskBitmap,
                source.width,
                source.height,
                true
            )
            
            // Create result bitmap with white background
            val result = Bitmap.createBitmap(
                source.width,
                source.height,
                Bitmap.Config.ARGB_8888
            )
            
            // Apply mask: where mask is white (255), keep source pixel; where black (0), use white
            val sourcePixels = IntArray(source.width * source.height)
            source.getPixels(sourcePixels, 0, source.width, 0, 0, source.width, source.height)
            
            val maskPixels = IntArray(resizedMask.width * resizedMask.height)
            resizedMask.getPixels(maskPixels, 0, resizedMask.width, 0, 0, resizedMask.width, resizedMask.height)
            
            val resultPixels = IntArray(source.width * source.height)
            
            for (i in sourcePixels.indices) {
                // Get grayscale value from mask (use red channel)
                val maskValue = (maskPixels[i] shr 16) and 0xFF
                
                if (maskValue > 127) { // White in mask = keep source pixel
                    resultPixels[i] = sourcePixels[i]
                } else { // Black in mask = white background
                    resultPixels[i] = 0xFFFFFFFF.toInt()
                }
            }
            
            result.setPixels(resultPixels, 0, source.width, 0, 0, source.width, source.height)
            
            // Cleanup
            if (resizedMask != maskBitmap) {
                resizedMask.recycle()
            }
            maskBitmap.recycle()
            
            Timber.d("✅ Applied PNG mask successfully")
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply PNG mask")
            null
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
        
        // Ensure cardRect is within source bounds
        val safeLeft = cardRect.left.coerceIn(0, source.width - 1)
        val safeTop = cardRect.top.coerceIn(0, source.height - 1)
        val safeRight = cardRect.right.coerceIn(safeLeft + 1, source.width)
        val safeBottom = cardRect.bottom.coerceIn(safeTop + 1, source.height)
        
        val cardWidth = safeRight - safeLeft
        val cardHeight = safeBottom - safeTop
        
        // Scale template to match cardRect size exactly
        val scaledTemplate = Bitmap.createScaledBitmap(
            template,
            cardWidth,
            cardHeight,
            true
        )
        
        // Extract card region from source using safe coordinates
        val cardBitmap = Bitmap.createBitmap(
            source,
            safeLeft,
            safeTop,
            cardWidth,
            cardHeight
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
        
        // Draw card on white background at correct position (using safe coordinates)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(cardBitmap, safeLeft.toFloat(), safeTop.toFloat(), paint)
        
        if (scaledTemplate != template) {
            scaledTemplate.recycle()
        }
        cardBitmap.recycle()
        
        Timber.d("Applied template mask: card ${finalCardWidth}x${finalCardHeight} at (${safeLeft}, ${safeTop}) [original rect: (${cardRect.left}, ${cardRect.top}) to (${cardRect.right}, ${cardRect.bottom})]")
        
        return result
    }

    /**
     * TEST: Loads PNG mask from assets for testing
     */
    private fun loadMaskFromAssets(maskName: String): Bitmap? {
        return try {
            Timber.d("🔍 Trying to load mask: mask/$maskName")
            val inputStream = context.assets.open("mask/$maskName")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                Timber.d("✅ Successfully loaded mask: $maskName (${bitmap.width}x${bitmap.height})")
            } else {
                Timber.w("⚠️ Failed to decode mask: $maskName")
            }
            bitmap
        } catch (e: Exception) {
            Timber.w(e, "❌ Mask not found in assets: masks/$maskName")
            null
        }
    }

    /**
     * TEST: Applies PNG mask directly (for testing with 4 masks)
     * NOTE: This is just for testing - applies first available mask to any photo
     * In production, TFLite will generate masks dynamically for each photo
     */
    private suspend fun applyPngMaskFromAssets(source: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        // STEP 1: Detect card in photo using template matching
        val smallTemplate = loadTemplateFromAssets("108.png")
        val longTemplate = loadTemplateFromAssets("165.png")
        
        var bestMatch: android.graphics.Rect? = null
        var bestTemplate: Bitmap? = null
        var bestScore = 0f
        
        // Try small template
        if (smallTemplate != null) {
            val match = findCardWithTemplate(source, smallTemplate)
            if (match != null) {
                val score = calculateMatchScoreForRect(source, smallTemplate, match)
                Timber.d("🔍 Small template match score: $score")
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = match
                    bestTemplate = smallTemplate
                }
            }
        }
        
        // Try long template
        if (longTemplate != null) {
            val match = findCardWithTemplate(source, longTemplate)
            if (match != null) {
                val score = calculateMatchScoreForRect(source, longTemplate, match)
                Timber.d("🔍 Long template match score: $score")
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = match
                    bestTemplate = longTemplate
                    if (smallTemplate != null && smallTemplate != longTemplate) {
                        smallTemplate.recycle()
                    }
                }
            }
        }
        
        if (bestMatch == null || bestScore < 0.3f) {
            Timber.w("⚠️ No card detected with template matching, cannot apply PNG mask")
            bestTemplate?.recycle()
            return@withContext null
        }
        
        Timber.d("✅ Card detected at: ${bestMatch.left}, ${bestMatch.top}, ${bestMatch.width()}x${bestMatch.height()}")
        
        // STEP 2: Load PNG mask
        val maskNames = listOf("0.png", "11.png", "24.png", "33.png")
        var mask: Bitmap? = null
        var maskName: String? = null
        for (name in maskNames) {
            mask = loadMaskFromAssets(name)
            if (mask != null) {
                maskName = name
                Timber.d("Using test mask: $maskName (this is just for testing - not the correct mask for this photo)")
                break
            }
        }
        
        if (mask == null) {
            Timber.d("No test masks found in assets")
            bestTemplate?.recycle()
            return@withContext null
        }
        
        // STEP 3: Extract card region and apply mask
        try {
            // Ensure card rect is within bounds
            val safeLeft = bestMatch.left.coerceIn(0, source.width - 1)
            val safeTop = bestMatch.top.coerceIn(0, source.height - 1)
            val safeRight = bestMatch.right.coerceIn(safeLeft + 1, source.width)
            val safeBottom = bestMatch.bottom.coerceIn(safeTop + 1, source.height)
            
            val cardWidth = safeRight - safeLeft
            val cardHeight = safeBottom - safeTop
            
            // Extract card region from source
            val cardBitmap = Bitmap.createBitmap(source, safeLeft, safeTop, cardWidth, cardHeight)
            
            // Calculate scale to fit mask while preserving aspect ratio
            val maskAspectRatio = mask.width.toFloat() / mask.height.toFloat()
            val cardAspectRatio = cardWidth.toFloat() / cardHeight.toFloat()
            
            val scaledMaskWidth: Int
            val scaledMaskHeight: Int
            val maskOffsetX: Int
            val maskOffsetY: Int
            
            if (maskAspectRatio > cardAspectRatio) {
                // Mask is wider - fit to width
                scaledMaskWidth = cardWidth
                scaledMaskHeight = (cardWidth / maskAspectRatio).toInt()
                maskOffsetX = 0
                maskOffsetY = (cardHeight - scaledMaskHeight) / 2
            } else {
                // Mask is taller - fit to height
                scaledMaskHeight = cardHeight
                scaledMaskWidth = (cardHeight * maskAspectRatio).toInt()
                maskOffsetX = (cardWidth - scaledMaskWidth) / 2
                maskOffsetY = 0
            }
            
            // Resize mask preserving aspect ratio
            val resizedMask = Bitmap.createScaledBitmap(
                mask,
                scaledMaskWidth,
                scaledMaskHeight,
                true
            )
            
            // Apply mask to card region
            val cardPixels = IntArray(cardWidth * cardHeight)
            cardBitmap.getPixels(cardPixels, 0, cardWidth, 0, 0, cardWidth, cardHeight)
            
            val maskPixels = IntArray(resizedMask.width * resizedMask.height)
            resizedMask.getPixels(maskPixels, 0, resizedMask.width, 0, 0, resizedMask.width, resizedMask.height)
            
            // Apply mask with offset (centered)
            for (y in 0 until cardHeight) {
                for (x in 0 until cardWidth) {
                    val cardIdx = y * cardWidth + x
                    
                    // Check if this pixel is within the mask bounds
                    val maskX = x - maskOffsetX
                    val maskY = y - maskOffsetY
                    
                    if (maskX >= 0 && maskX < scaledMaskWidth && maskY >= 0 && maskY < scaledMaskHeight) {
                        val maskIdx = maskY * scaledMaskWidth + maskX
                        val maskValue = (maskPixels[maskIdx] shr 16) and 0xFF // Red channel
                        if (maskValue < 127) {
                            cardPixels[cardIdx] = 0xFFFFFFFF.toInt() // White background where mask is black
                        }
                    } else {
                        // Outside mask bounds - make white
                        cardPixels[cardIdx] = 0xFFFFFFFF.toInt()
                    }
                }
            }
            
            cardBitmap.setPixels(cardPixels, 0, cardWidth, 0, 0, cardWidth, cardHeight)
            
            // STEP 4: Create result with white background and draw card
            val result = Bitmap.createBitmap(
                source.width,
                source.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(result)
            canvas.drawColor(0xFFFFFFFF.toInt()) // White background
            
            // Draw masked card at detected position
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(cardBitmap, safeLeft.toFloat(), safeTop.toFloat(), paint)
            
            // Cleanup
            cardBitmap.recycle()
            if (resizedMask != mask) resizedMask.recycle()
            mask.recycle()
            bestTemplate?.recycle()
            
            Timber.d("✅ Applied test PNG mask: $maskName to detected card region (${cardWidth}x${cardHeight})")
            return@withContext result
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply PNG mask")
            mask?.recycle()
            bestTemplate?.recycle()
            null
        }
    }

    /**
     * Draws the card on white background.
     * 
     * SIMPLIFICAT: Crop dreptunghiular simplu centrat cu padding minim.
     * Elimină fundalul excesiv fără procesare complexă (FĂRĂ TFLite/OpenCV).
     */
    private suspend fun drawCardOnWhiteBackground(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        Timber.d("📸 Frame with padding: preserve 100% of original photo, add white frame")
        
        val width = source.width
        val height = source.height
        
        // ✅ NO CROP: Păstrează 100% din poza originală
        // Doar adaugă padding alb în jur (5% pe fiecare latură, min 20px, max 50px)
        val paddingPercent = 0.05f
        val paddingPx = (width.coerceAtLeast(height) * paddingPercent).toInt()
            .coerceAtLeast(20)
            .coerceAtMost(50)
        
        // Creează canvas cu dimensiunea originală + padding
        val result = Bitmap.createBitmap(
            width + paddingPx * 2,
            height + paddingPx * 2,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(result)
        
        // Desenează fundal alb
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Plasează poza originală completă pe fundal alb (centrată cu padding)
        canvas.drawBitmap(source, paddingPx.toFloat(), paddingPx.toFloat(), null)
        
        Timber.d("✅ Frame with padding: ${width}x${height} -> ${result.width}x${result.height} (preserved 100%, padding: ${paddingPx}px)")
        
        return@withContext result
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
