package com.example.hotwheelscollectors.domain.manager

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Manager pentru inferența TensorFlow Lite pentru segmentarea cartonașelor.
 * 
 * Folosește modelul TFLite pentru a genera măști de segmentare pentru cartonașe Hot Wheels.
 * 
 * FLUX:
 * 1. Încarcă modelul TFLite din assets/models/card_segmentation.tflite
 * 2. Preprocesează imaginea (resize, normalizare)
 * 3. Rulează inferența pentru a obține masca
 * 4. Postprocesează masca (threshold, cleanup)
 * 5. Returnează masca binară (negru = background, alb = cartonaș)
 */
class TFLiteSegmentationManager(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelPath = "models/card_segmentation.tflite"
    
    // Dimensiuni input/output ale modelului (vor fi setate când modelul este încărcat)
    private var inputWidth = 256  // Default - va fi actualizat din model
    private var inputHeight = 256  // Default - va fi actualizat din model
    
    init {
        loadModel()
    }
    
    /**
     * Încarcă modelul TFLite din assets.
     * Dacă modelul nu există, interpreter va rămâne null și se va folosi fallback (măști PNG).
     */
    private fun loadModel() {
        try {
            val modelFile = loadModelFile(modelPath)
            if (modelFile == null) {
                Timber.w("⚠️ TFLite model not found at $modelPath - will use PNG masks fallback")
                return
            }
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)  // Folosește 4 thread-uri pentru inferență
                // setUseXNNPACK(true)  // Automat activat în TFLite 2.14+
            }
            
            interpreter = Interpreter(modelFile, options)
            
            // Obține dimensiunile input/output din model
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            
            if (inputShape != null && inputShape.size >= 3) {
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
            }
            
            Timber.d("✅ TFLite model loaded successfully")
            Timber.d("   Model path: $modelPath")
            Timber.d("   Input shape: ${inputShape?.contentToString()}")
            Timber.d("   Output shape: ${outputShape?.contentToString()}")
            Timber.d("   Input size: ${inputWidth}x${inputHeight}")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load TFLite model")
            interpreter = null
        }
    }
    
    /**
     * Verifică dacă modelul TFLite este disponibil și încărcat.
     */
    fun isModelAvailable(): Boolean {
        return interpreter != null
    }
    
    /**
     * Încarcă modelul TFLite din assets ca MappedByteBuffer.
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer? {
        return try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: IOException) {
            Timber.e(e, "Error loading model file: $modelPath")
            null
        }
    }
    
    /**
     * Segmentă cartonașul din imagine folosind TFLite.
     * 
     * @param sourceBitmap Imaginea sursă (poate fi orice dimensiune)
     * @return Mască binară (Bitmap) - alb = cartonaș, negru = background
     *         Returnează null dacă modelul nu este disponibil sau dacă apare o eroare
     */
    suspend fun segmentCard(sourceBitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            Timber.w("⚠️ TFLite model not available - cannot segment")
            return@withContext null
        }
        
        try {
            // STEP 0: Verifică și rotește imaginea dacă are EXIF rotation
            val rotatedSource = correctImageOrientation(sourceBitmap)
            
            // STEP 1: DIRECT STRETCH resize (COCO compatible - exact ca la training)
            // ❌ NU letterbox, ❌ NU padding, ❌ NU aspect ratio
            // ✅ STRETCH direct la inputWidth x inputHeight (ex: 256×256)
            val resizedBitmap = Bitmap.createScaledBitmap(
                rotatedSource,
                inputWidth,
                inputHeight,
                true  // Bilinear filtering
            )
            
            Timber.d("STRETCH resize: source ${rotatedSource.width}x${rotatedSource.height} -> ${inputWidth}x${inputHeight} (COCO compatible)")
            
            // STEP 2: Convertește Bitmap la ByteBuffer (normalizat [0, 1])
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            
            // STEP 3: Alocă output buffer pentru mască
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outputSize = outputShape[1] * outputShape[2] * outputShape[3]  // H * W * channels
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)  // 4 bytes per float
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // STEP 4: Rulează inferența
            interpreter!!.run(inputBuffer, outputBuffer)
            
            // STEP 5: Convertește output buffer la Bitmap (mască GRAYSCALE 0-255)
            val maskBitmap = outputBufferToMask(outputBuffer, outputShape[1], outputShape[2])
            
            // STEP 6: Masca este deja perfectă la inputWidth x inputHeight (ex: 256×256)
            // NU mai face upscale aici - se va face în extractCardWithMask() dacă e nevoie
            // NU mai face OpenCV cleanup - masca este deja corectă geometric
            
            // Cleanup
            resizedBitmap.recycle()
            if (rotatedSource != sourceBitmap) {
                rotatedSource.recycle()
            }
            
            Timber.d("✅ TFLite segmentation completed: ${maskBitmap.width}x${maskBitmap.height} (perfect mask, no post-processing needed)")
            return@withContext maskBitmap
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error during TFLite segmentation")
            return@withContext null
        }
    }
    
    /**
     * Corectează orientarea imaginii bazată pe EXIF (dacă vine din cameră).
     * Returnează imaginea rotită corect sau originalul dacă nu e nevoie de rotire.
     */
    private fun correctImageOrientation(bitmap: Bitmap): Bitmap {
        // Nota: Pentru imagini din cameră, rotirea ar trebui făcută ÎNAINTE de a ajunge aici
        // Această funcție este un backup - în general, CameraManager ar trebui să trimită
        // imagini deja rotite corect.
        // Pentru moment, returnăm imaginea neschimbată.
        return bitmap
    }
    
    /**
     * Convertește Bitmap la ByteBuffer normalizat [0, 1].
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = inputWidth * inputHeight * 3  // RGB
        val byteBuffer = ByteBuffer.allocateDirect(inputSize * 4)  // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var pixel = 0
        for (i in 0 until inputHeight) {
            for (j in 0 until inputWidth) {
                val pixelValue = intValues[pixel++]
                
                // Normalizează la [0, 1]
                byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)  // R
                byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)   // G
                byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)            // B
            }
        }
        
        return byteBuffer
    }
    
    /**
     * Convertește output buffer (float array) la Bitmap binar (mască).
     * Simplificat: doar conversie la binar cu threshold normal (0.5).
     * Tot cleanup-ul (threshold agresiv, morphology, contours) se face în OpenCV.
     */
    private fun outputBufferToMask(buffer: ByteBuffer, height: Int, width: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        buffer.rewind()
        var whitePixels = 0
        var blackPixels = 0
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE
        var sumValue = 0.0f
        
        // Prima trecere: calculează statistici
        for (i in pixels.indices) {
            val value = buffer.float
            if (value < minValue) minValue = value
            if (value > maxValue) maxValue = value
            sumValue += value
        }
        
        val meanValue = sumValue / (width * height)
        Timber.d("TFLite mask output stats: min=${String.format("%.4f", minValue)}, max=${String.format("%.4f", maxValue)}, mean=${String.format("%.4f", meanValue)}")
        android.util.Log.d("TFLiteSegmentationManager", "TFLite mask output stats: min=${String.format("%.4f", minValue)}, max=${String.format("%.4f", maxValue)}, mean=${String.format("%.4f", meanValue)}")
        
        // ✅ FIX: Conversie la GRAYSCALE (0-255), NU binară
        // Threshold-ul se va face DOAR în OpenCV la rezoluție mare
        buffer.rewind()
        Timber.d("TFLite: Converting to GRAYSCALE mask (0-255) - NO threshold here, OpenCV will handle it")
        
        for (i in pixels.indices) {
            val value = buffer.float  // value is 0.0-1.0 from TFLite
            // Convert float (0.0-1.0) to grayscale (0-255)
            val gray = (value * 255f).toInt().coerceIn(0, 255)
            // Create ARGB pixel: alpha=255, R=G=B=gray
            pixels[i] = android.graphics.Color.argb(255, gray, gray, gray)
        }
        
        val totalPixels = width * height
        Timber.d("TFLite: Created GRAYSCALE mask (0-255) - ready for upscaling, threshold will be done in OpenCV")
        
        mask.setPixels(pixels, 0, width, 0, 0, width, height)
        return mask
    }
    
    /**
     * Extrage cartonașul din imagine folosind masca generată de TFLite.
     * 
     * PIPELINE CORECT (production-grade):
     * 1. Masca TFLite (256×256) → DOAR pentru localizare (bounding box)
     * 2. Calculează bounding box din mască (threshold 150)
     * 3. Scalează bounding box la rezoluția originală
     * 4. CROP DIRECT din imaginea originală (rezoluție mare)
     * 5. OpenCV pe imaginea HIGH-RES: Canny edge detection → findContours → largest contour
     * 6. Creează mască high-res din conturul real (geometric, nu AI)
     * 7. Aplică masca (pixel 1:1, fundal #F5F5F5)
     * 8. Padding #F5F5F5 simplu
     * 
     * Fallback: Dacă OpenCV eșuează → force aspect ratio ~0.65
     * 
     * @param sourceBitmap Imaginea originală
     * @param mask Masca grayscale (0-255) de la TFLite (256×256)
     * @return Bitmap cu cartonașul pe fundal #F5F5F5 (scan oficial, marginile reale)
     */
    suspend fun extractCardWithMask(sourceBitmap: Bitmap, mask: Bitmap): Bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        // ✅ Masca TFLite = DOAR detector de poziție, NU pentru decupare vizuală
        
        val maskW = mask.width
        val maskH = mask.height

        val maskPixels = IntArray(maskW * maskH)
        mask.getPixels(maskPixels, 0, maskW, 0, 0, maskW, maskH)

        // Calculează statistici despre mască pentru diagnosticare
        var minGray = 255
        var maxGray = 0
        var sumGray = 0
        var pixelsAbove128 = 0
        var pixelsAbove200 = 0
        
        for (i in maskPixels.indices) {
            val gray = (maskPixels[i] shr 16) and 0xFF
            if (gray < minGray) minGray = gray
            if (gray > maxGray) maxGray = gray
            sumGray += gray
            if (gray > 128) pixelsAbove128++
            if (gray > 200) pixelsAbove200++
        }
        
        val meanGray = sumGray / maskPixels.size
        val percentAbove128 = (pixelsAbove128 * 100.0f / maskPixels.size)
        val percentAbove200 = (pixelsAbove200 * 100.0f / maskPixels.size)
        
        Timber.d("Mask stats: min=$minGray, max=$maxGray, mean=$meanGray, >128: ${percentAbove128}%, >200: ${percentAbove200}%")
        android.util.Log.d("TFLiteSegmentationManager", "Mask stats: min=$minGray, max=$maxGray, mean=$meanGray, >128: ${percentAbove128}%, >200: ${percentAbove200}%")

        var minX = maskW
        var maxX = 0
        var minY = maskH
        var maxY = 0
        var found = false

        // Threshold FOARTE permisiv pentru bounding box: 64 (pentru a captura întregul cartonaș, inclusiv zonele mai puțin clare)
        // Threshold-ul anterior (150) era prea strict și detecta doar literele colorate, nu întregul cartonaș
        val boundingBoxThreshold = 64
        
        for (y in 0 until maskH) {
            for (x in 0 until maskW) {
                val gray = (maskPixels[y * maskW + x] shr 16) and 0xFF
                if (gray > boundingBoxThreshold) {  // Threshold permisiv pentru bounding box
                    found = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        
        Timber.d("Bounding box detection: threshold=$boundingBoxThreshold, found=$found, box: ($minX, $minY) to ($maxX, $maxY)")
        android.util.Log.d("TFLiteSegmentationManager", "Bounding box: ($minX, $minY) to ($maxX, $maxY), size: ${maxX - minX + 1}×${maxY - minY + 1}")

        if (!found) {
            Timber.w("⚠️ No white pixels found in mask - returning original on gray background")
            val fallback = Bitmap.createBitmap(
                sourceBitmap.width,
                sourceBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val c = android.graphics.Canvas(fallback)
            c.drawColor(0xFFF5F5F5.toInt())  // #F5F5F5 gray background
            c.drawBitmap(sourceBitmap, 0f, 0f, null)
            return@withContext fallback
        }

        // Validare bounding box (păstrată pentru siguranță)
        if (minX >= maxX || minY >= maxY) {
            Timber.w("⚠️ Invalid bounding box - using full image")
            minX = 0
            maxX = maskW - 1
            minY = 0
            maxY = maskH - 1
        }

        val xRatio = sourceBitmap.width.toFloat() / maskW
        val yRatio = sourceBitmap.height.toFloat() / maskH

        // Calculează bounding box la rezoluția originală
        val rx0 = (minX * xRatio).toInt()
        val ry0 = (minY * yRatio).toInt()
        val rx1 = (maxX * xRatio).toInt()
        val ry1 = (maxY * yRatio).toInt()

        // ✅ PAS 2: Crop-ul trebuie să fie PUȚIN MAI MARE decât cardul (padding pentru marginile reale)
        // Adaugă padding de ~5% pe fiecare latură pentru a captura marginile reale detectate de OpenCV
        val paddingX = ((rx1 - rx0) * 0.05f).toInt().coerceAtLeast(20).coerceAtMost(100)
        val paddingY = ((ry1 - ry0) * 0.05f).toInt().coerceAtLeast(20).coerceAtMost(100)
        
        val cropX0 = (rx0 - paddingX).coerceAtLeast(0)
        val cropY0 = (ry0 - paddingY).coerceAtLeast(0)
        val cropX1 = (rx1 + paddingX).coerceAtMost(sourceBitmap.width - 1)
        val cropY1 = (ry1 + paddingY).coerceAtMost(sourceBitmap.height - 1)

        val w = cropX1 - cropX0 + 1
        val h = cropY1 - cropY0 + 1

        Timber.d("Bounding box: mask ($minX, $minY) to ($maxX, $maxY) -> original ($rx0, $ry0) to ($rx1, $ry1)")
        Timber.d("Crop with padding: ($cropX0, $cropY0) to ($cropX1, $cropY1), size: ${w}×${h} (padding: ${paddingX}×${paddingY})")
        android.util.Log.d("TFLiteSegmentationManager", "Bounding box scaled: original size ${sourceBitmap.width}×${sourceBitmap.height}, crop size: ${w}×${h} (with padding)")
        
        // Validare: dacă bounding box-ul este prea mic, probabil threshold-ul este prea strict
        if (w < 100 || h < 100) {
            Timber.w("⚠️ Bounding box is very small (${w}×${h}) - mask might be incorrect or threshold too strict")
            android.util.Log.w("TFLiteSegmentationManager", "⚠️ Bounding box too small: ${w}×${h}, mask stats: mean=$meanGray")
        }

        // Crop direct din imaginea originală (rezoluție mare) CU PADDING pentru marginile reale
        val crop = Bitmap.createBitmap(sourceBitmap, cropX0, cropY0, w, h)

        // ✅ PAS 4: OpenCV pe imaginea HIGH-RES pentru detectare contur real
        // Pasează masca TFLite scalată pentru intersectare (opțional, îmbunătățește rezultatele)
        val tfliteMaskScaled = if (mask.width != w || mask.height != h) {
            Bitmap.createScaledBitmap(mask, w, h, true)
        } else {
            mask
        }
        val highResMask = OpenCVMaskProcessor.detectCardContourHighRes(crop, tfliteMaskScaled)
        if (tfliteMaskScaled != mask) {
            tfliteMaskScaled.recycle()
        }
        
        val finalCrop: Bitmap
        val finalW: Int
        val finalH: Int
        
        if (highResMask != null) {
            // ✅ OpenCV a găsit contur real → aplică masca high-res
            Timber.d("OpenCV: High-res contour detected successfully, applying mask")
            android.util.Log.d("TFLiteSegmentationManager", "OpenCV: High-res mask size: ${highResMask.width}×${highResMask.height}, crop size: ${w}×${h}")
            
            // Verifică dacă dimensiunile măștii se potrivesc cu crop-ul
            if (highResMask.width != w || highResMask.height != h) {
                Timber.w("⚠️ Mask size mismatch: mask ${highResMask.width}×${highResMask.height} vs crop ${w}×${h} - scaling mask")
                android.util.Log.w("TFLiteSegmentationManager", "⚠️ Mask size mismatch, scaling mask to match crop")
                
                // Scalează masca la dimensiunile crop-ului
                val scaledMask = Bitmap.createScaledBitmap(highResMask, w, h, true)
                highResMask.recycle()
                
                // Aplică masca scalată
                val cropPixels = IntArray(w * h)
                val maskPixels = IntArray(w * h)
                crop.getPixels(cropPixels, 0, w, 0, 0, w, h)
                scaledMask.getPixels(maskPixels, 0, w, 0, 0, w, h)
                
                val resultPixels = IntArray(w * h)
                val bgColor = 0xFFF5F5F5.toInt()  // #F5F5F5 gray background
                var whitePixels = 0
                var blackPixels = 0
                
                for (i in cropPixels.indices) {
                    val maskGray = (maskPixels[i] shr 16) and 0xFF
                    if (maskGray > 128) {
                        // Mască albă = cartonaș → păstrează pixel original
                        resultPixels[i] = cropPixels[i]
                        whitePixels++
                    } else {
                        // Mască neagră = fundal → fundal #F5F5F5
                        resultPixels[i] = bgColor
                        blackPixels++
                    }
                }
                
                Timber.d("OpenCV: Mask stats: white=$whitePixels, black=$blackPixels (${whitePixels * 100 / (w * h)}% white)")
                android.util.Log.d("TFLiteSegmentationManager", "OpenCV: Mask applied - ${whitePixels * 100 / (w * h)}% white pixels")
                
                finalCrop = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                finalCrop.setPixels(resultPixels, 0, w, 0, 0, w, h)
                finalW = w
                finalH = h
                
                scaledMask.recycle()
                crop.recycle()
            } else {
                // Dimensiunile se potrivesc perfect
                val cropPixels = IntArray(w * h)
                val maskPixels = IntArray(w * h)
                crop.getPixels(cropPixels, 0, w, 0, 0, w, h)
                highResMask.getPixels(maskPixels, 0, w, 0, 0, w, h)
                
                val resultPixels = IntArray(w * h)
                val bgColor = 0xFFF5F5F5.toInt()  // #F5F5F5 gray background
                var whitePixels = 0
                var blackPixels = 0
                
                for (i in cropPixels.indices) {
                    val maskGray = (maskPixels[i] shr 16) and 0xFF
                    if (maskGray > 128) {
                        // Mască albă = cartonaș → păstrează pixel original
                        resultPixels[i] = cropPixels[i]
                        whitePixels++
                    } else {
                        // Mască neagră = fundal → fundal #F5F5F5
                        resultPixels[i] = bgColor
                        blackPixels++
                    }
                }
                
                Timber.d("OpenCV: Mask stats: white=$whitePixels, black=$blackPixels (${whitePixels * 100 / (w * h)}% white)")
                android.util.Log.d("TFLiteSegmentationManager", "OpenCV: Mask applied - ${whitePixels * 100 / (w * h)}% white pixels")
                
                finalCrop = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                finalCrop.setPixels(resultPixels, 0, w, 0, 0, w, h)
                finalW = w
                finalH = h
                
                highResMask.recycle()
                crop.recycle()
            }
            
            Timber.d("OpenCV: Mask applied successfully, result: ${finalW}×${finalH}")
            
        } else {
            // ❌ OpenCV eșuează → fallback: force aspect ratio
            Timber.w("⚠️ OpenCV contour detection failed - using fallback (force aspect ratio)")
            
            val targetAspectRatio = 0.65f
            val currentAspectRatio = w.toFloat() / h.toFloat()
            
            if (kotlin.math.abs(currentAspectRatio - targetAspectRatio) > 0.05f) {
                if (currentAspectRatio > targetAspectRatio) {
                    finalW = (h * targetAspectRatio).toInt()
                    finalH = h
                    val cropX = (w - finalW) / 2
                    finalCrop = Bitmap.createBitmap(crop, cropX, 0, finalW, finalH)
                } else {
                    finalW = w
                    finalH = (w / targetAspectRatio).toInt()
                    val cropY = (h - finalH) / 2
                    finalCrop = Bitmap.createBitmap(crop, 0, cropY, finalW, finalH)
                }
                crop.recycle()
                Timber.d("Fallback: Geometric correction: ${w}×${h} -> ${finalW}×${finalH}")
            } else {
                finalCrop = crop
                finalW = w
                finalH = h
                Timber.d("Fallback: No geometric correction needed: ${w}×${h}")
            }
        }

        // ✅ PAS 5: Padding #F5F5F5 simplu
        val pad = (finalW * 6 / 256).coerceAtLeast(20).coerceAtMost(50)
        val finalBmp = Bitmap.createBitmap(
            finalW + pad * 2,
            finalH + pad * 2,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(finalBmp)
        canvas.drawColor(0xFFF5F5F5.toInt())  // #F5F5F5 gray background
        canvas.drawBitmap(finalCrop, pad.toFloat(), pad.toFloat(), null)

        Timber.d("Final result: ${finalBmp.width}×${finalBmp.height} (from ${finalW}×${finalH} at HIGH resolution, padding: ${pad}px)")
        android.util.Log.d("TFLiteSegmentationManager", "Final result: ${finalBmp.width}×${finalBmp.height} (OpenCV contour detection, background #F5F5F5)")

        // Cleanup
        finalCrop.recycle()

        return@withContext finalBmp
    }
    
    /**
     * Cleanup - eliberează resursele.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

