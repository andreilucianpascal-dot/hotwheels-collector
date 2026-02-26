package com.example.hotwheelscollectors.domain.manager

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Procesor OpenCV pentru post-procesarea măștilor TFLite.
 * 
 * Pipeline profesional:
 * 1. Threshold (probabilități → mască binară)
 * 2. Morphology OPEN (elimină zgomot)
 * 3. Morphology CLOSE (umple goluri)
 * 4. Contour detection (găsește contururi)
 * 5. Select biggest contour (cartonașul principal)
 * 6. Fill contour → mască perfectă
 * 
 * Folosit împreună cu TFLite:
 * - TFLite = ce este obiectul (semantic segmentation)
 * - OpenCV = cum arată marginile perfect (post-procesare)
 */
object OpenCVMaskProcessor {
    
    private var isInitialized = false
    
    /**
     * Inițializează OpenCV (trebuie apelat o singură dată la startul aplicației).
     * Pentru Android, folosește OpenCVLoader pentru a încărca biblioteca nativă.
     */
    fun initialize(context: Context? = null) {
        if (!isInitialized) {
            try {
                // Pentru Android, OpenCV necesită OpenCVLoader
                if (OpenCVLoader.initDebug()) {
                    isInitialized = true
                    Timber.d("✅ OpenCV initialized: ${org.opencv.core.Core.getVersionString()}")
                } else {
                    Timber.w("⚠️ OpenCV library not available - mask refinement will be disabled")
                    isInitialized = false
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to initialize OpenCV - mask refinement will be disabled")
                isInitialized = false
            }
        }
    }
    
    /**
     * Verifică dacă OpenCV este disponibil.
     */
    fun isAvailable(): Boolean {
        return try {
            isInitialized || !org.opencv.core.Core.getVersionString().isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Refinează masca TFLite folosind post-procesare OpenCV profesională.
     * 
     * @param maskBitmap Mască binară de la TFLite (alb = cartonaș, negru = background)
     * @return Mască curățată și rafinată
     */
    suspend fun refineMask(maskBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isAvailable()) {
            Timber.w("⚠️ OpenCV not available - returning original mask")
            return@withContext maskBitmap
        }
        
        try {
            // Convert Bitmap to OpenCV Mat
            val maskMat = Mat()
            Utils.bitmapToMat(maskBitmap, maskMat)
            
            // Convert to grayscale if needed
            val grayMat = if (maskMat.channels() > 1) {
                val gray = Mat()
                Imgproc.cvtColor(maskMat, gray, Imgproc.COLOR_BGR2GRAY)
                maskMat.release()
                gray
            } else {
                maskMat
            }
            
            Timber.d("OpenCV: Input mask size: ${grayMat.width()}x${grayMat.height()}")
            
            // STEP 1: Threshold la rezoluție mare (180-210 din 255)
            // Masca vine deja ca GRAYSCALE (0-255) de la TFLite, scalată la rezoluție mare
            val binaryMat = Mat()
            val THRESHOLD_VALUE = 200.0  // 200/255 ≈ 0.78 (echilibrat: nu prea agresiv, nu prea permisiv)
            Timber.d("OpenCV: Using threshold: $THRESHOLD_VALUE (${String.format("%.2f", THRESHOLD_VALUE/255.0)}) at HIGH resolution")
            Imgproc.threshold(
                grayMat,
                binaryMat,
                THRESHOLD_VALUE,  // Threshold agresiv
                255.0,  // Max value
                Imgproc.THRESH_BINARY
            )
            
            // STEP 2: Morphology OPEN (elimină zgomot mic)
            val kernelOpen = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(5.0, 5.0)
            )
            val openedMat = Mat()
            Imgproc.morphologyEx(
                binaryMat,
                openedMat,
                Imgproc.MORPH_OPEN,
                kernelOpen
            )
            
            // STEP 3: Morphology CLOSE (umple goluri)
            val kernelClose = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(7.0, 7.0)
            )
            val closedMat = Mat()
            Imgproc.morphologyEx(
                openedMat,
                closedMat,
                Imgproc.MORPH_CLOSE,
                kernelClose
            )
            
            // STEP 4: Find contours
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                closedMat,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            Timber.d("OpenCV: Found ${contours.size} contours")
            
            // STEP 5: Select biggest contour (cartonașul principal)
            val refinedMat = Mat.zeros(closedMat.size(), CvType.CV_8UC1)
            
            if (contours.isNotEmpty()) {
                // Găsește conturul cu cea mai mare arie
                var maxArea = 0.0
                var biggestContour: MatOfPoint? = null
                
                for (contour in contours) {
                    val area = Imgproc.contourArea(contour)
                    if (area > maxArea) {
                        maxArea = area
                        biggestContour = contour
                    }
                }
                
                if (biggestContour != null) {
                    Timber.d("OpenCV: Biggest contour area: $maxArea pixels")
                    
                    // Fill biggest contour → mască perfectă
                    Imgproc.drawContours(
                        refinedMat,
                        listOf(biggestContour),
                        -1,  // Draw all contours in the list
                        Scalar(255.0),  // White color
                        -1  // Fill contour
                    )
                } else {
                    // Fallback: folosește masca închisă dacă nu găsim contur
                    Timber.w("⚠️ No biggest contour found - using closed mask")
                    closedMat.copyTo(refinedMat)
                }
            } else {
                // Fallback: folosește masca închisă dacă nu găsim contururi
                Timber.w("⚠️ No contours found - using closed mask")
                closedMat.copyTo(refinedMat)
            }
            
            // Calculează statistici ÎNAINTE de release
            val whitePixels = Core.countNonZero(refinedMat)
            val totalPixels = refinedMat.width() * refinedMat.height()
            val whitePercent = if (totalPixels > 0) (whitePixels * 100.0 / totalPixels) else 0.0
            Timber.d("OpenCV: Refined mask stats: ${whitePixels}/${totalPixels} white (${String.format("%.2f", whitePercent)}%)")
            android.util.Log.d("OpenCVMaskProcessor", "OpenCV: Refined mask stats: ${whitePixels}/${totalPixels} white (${String.format("%.2f", whitePercent)}%)")
            
            // Convert Mat back to Bitmap
            val resultBitmap = Bitmap.createBitmap(
                refinedMat.width(),
                refinedMat.height(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(refinedMat, resultBitmap)
            
            // Cleanup
            grayMat.release()
            binaryMat.release()
            openedMat.release()
            closedMat.release()
            refinedMat.release()
            hierarchy.release()
            kernelOpen.release()
            kernelClose.release()
            contours.forEach { it.release() }
            
            return@withContext resultBitmap
            
        } catch (e: Exception) {
            Timber.e(e, "❌ OpenCV mask refinement failed - returning original")
            return@withContext maskBitmap
        }
    }
    
    /**
     * Detectează conturul real al cartonașului pe imaginea HIGH-RES folosind OpenCV.
     * 
     * Pipeline profesional pentru detectare margini reale:
     * 1. Downscale pentru performanță (max 2000px pe latura lungă)
     * 2. Grayscale
     * 3. CLAHE (Contrast Limited Adaptive Histogram Equalization) pentru iluminare neuniformă
     * 4. Blur ușor (Gaussian) pentru eliminare zgomot
     * 5. Adaptive threshold (pentru contrast slab) + Canny edge detection
     * 6. findContours (găsește contururi geometrice)
     * 7. approxPolyDP (netezire contur, păstrează forma reală)
     * 8. Filtrare contururi (largest + aspect ratio ~0.65)
     * 9. Intersectare cu masca TFLite (opțional, dacă e furnizată)
     * 10. Scale contur înapoi la rezoluția originală
     * 11. Creează mască high-res din contur
     * 
     * @param cropBitmap Crop-ul high-res din imaginea originală
     * @param tfliteMask Masca TFLite scalată la dimensiunile crop-ului (opțional, pentru intersectare)
     * @return Mască binară high-res (alb = cartonaș, negru = background) sau null dacă eșuează
     */
    suspend fun detectCardContourHighRes(cropBitmap: Bitmap, tfliteMask: Bitmap? = null): Bitmap? = withContext(Dispatchers.Default) {
        if (!isAvailable()) {
            Timber.w("⚠️ OpenCV not available - cannot detect contour")
            return@withContext null
        }
        
        try {
            val originalW = cropBitmap.width
            val originalH = cropBitmap.height
            val maxDimension = 2000  // Downscale pentru performanță
            
            // Downscale pentru performanță dacă e prea mare
            val scaleFactor: Double
            val workingBitmap: Bitmap
            val workingW: Int
            val workingH: Int
            
            if (originalW > maxDimension || originalH > maxDimension) {
                val scale = maxDimension.toDouble() / maxOf(originalW, originalH)
                workingW = (originalW * scale).toInt()
                workingH = (originalH * scale).toInt()
                scaleFactor = 1.0 / scale
                workingBitmap = Bitmap.createScaledBitmap(cropBitmap, workingW, workingH, true)
                Timber.d("OpenCV: Downscaled ${originalW}×${originalH} -> ${workingW}×${workingH} for processing")
            } else {
                workingBitmap = cropBitmap
                workingW = originalW
                workingH = originalH
                scaleFactor = 1.0
            }
            
            // Convert Bitmap to OpenCV Mat
            val sourceMat = Mat()
            Utils.bitmapToMat(workingBitmap, sourceMat)
            
            // STEP 1: Grayscale
            val grayMat = Mat()
            if (sourceMat.channels() > 1) {
                Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_BGR2GRAY)
            } else {
                sourceMat.copyTo(grayMat)
            }
            
            // STEP 2: CLAHE (Contrast Limited Adaptive Histogram Equalization) pentru iluminare neuniformă
            val claheMat = Mat()
            try {
                // CLAHE pentru iluminare neuniformă (nivel pro)
                val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))  // Clip limit 2.0, tile grid 8x8
                clahe.apply(grayMat, claheMat)
                // Note: CLAHE nu are metoda close() în OpenCV Android, se eliberează automat
                Timber.d("OpenCV: CLAHE applied for uniform illumination")
            } catch (e: Exception) {
                // Fallback: dacă CLAHE nu e disponibil, folosește grayscale direct
                Timber.w("⚠️ CLAHE not available, using grayscale directly: ${e.message}")
                grayMat.copyTo(claheMat)
            }
            
            // STEP 3: Blur ușor pentru eliminare zgomot
            val blurredMat = Mat()
            Imgproc.GaussianBlur(claheMat, blurredMat, Size(5.0, 5.0), 0.0)
            
            // STEP 4: Canny edge detection (detectează marginile reale)
            val edgesMat = Mat()
            val cannyLow = 80.0
            val cannyHigh = 160.0
            Imgproc.Canny(blurredMat, edgesMat, cannyLow, cannyHigh)
            
            // STEP 5: Adaptive threshold doar ca fallback (NU combinat cu OR permanent)
            // PROBLEMA #1 FIX: Canny bun → folosește DOAR Canny, Canny slab → fallback adaptive
            val adaptiveMat = Mat()
            val combinedEdges = if (Core.countNonZero(edgesMat) > workingW * workingH * 0.01) {
                // Canny a detectat suficiente edge-uri (>1% din imagine) → folosește DOAR Canny
                Timber.d("OpenCV: Canny edge detection completed on ${workingW}×${workingH} - using Canny only")
                edgesMat
            } else {
                // Canny slab → fallback la adaptive threshold
                Timber.d("OpenCV: Canny detected few edges, using adaptive threshold fallback")
                Imgproc.adaptiveThreshold(
                    blurredMat,
                    adaptiveMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11,  // Block size (trebuie să fie impar)
                    2.0  // C constant (subtract from mean)
                )
                adaptiveMat
            }
            
            // STEP 6: Folosește masca TFLite doar ca ghid (NU constrângere strictă)
            // Problema: Intersectarea strictă (AND) elimină marginile reale dacă masca TFLite nu acoperă întregul cartonaș
            // Soluție: Folosim Canny direct, masca TFLite doar pentru filtrare inițială (dacă e furnizată)
            val finalEdgesMat = if (tfliteMask != null) {
                try {
                    // Scalează masca TFLite la dimensiunile working
                    val scaledTfliteMask = if (tfliteMask.width != workingW || tfliteMask.height != workingH) {
                        Bitmap.createScaledBitmap(tfliteMask, workingW, workingH, true)
                    } else {
                        tfliteMask
                    }
                    
                    val tfliteMat = Mat()
                    Utils.bitmapToMat(scaledTfliteMask, tfliteMat)
                    
                    // Convertește la grayscale dacă e nevoie
                    val tfliteGray = if (tfliteMat.channels() > 1) {
                        val gray = Mat()
                        Imgproc.cvtColor(tfliteMat, gray, Imgproc.COLOR_BGR2GRAY)
                        tfliteMat.release()
                        gray
                    } else {
                        tfliteMat
                    }
                    
                    // Threshold permisiv pentru masca TFLite (folosită doar ca ghid)
                    val tfliteBinary = Mat()
                    Imgproc.threshold(tfliteGray, tfliteBinary, 64.0, 255.0, Imgproc.THRESH_BINARY)  // Threshold permisiv (64)
                    
                    // Dilatare masca TFLite pentru a acoperi zonele apropiate (ghid mai larg)
                    // OPTIMIZARE #1: Kernel de dilatare dinamic (nu fix 15x15)
                    val dilatedTflite = Mat()
                    val kernelSize = maxOf(7, workingW / 200).toInt()  // Dinamic bazat pe dimensiune
                    val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize.toDouble(), kernelSize.toDouble()))
                    Imgproc.dilate(tfliteBinary, dilatedTflite, dilateKernel)
                    
                    // Folosim Canny direct (NU intersectare strictă) - masca TFLite doar ca ghid pentru filtrare
                    // Dacă masca TFLite există, o folosim pentru a filtra doar zonele relevante, dar NU eliminăm marginile Canny
                    val guidedEdges = Mat()
                    Core.bitwise_and(combinedEdges, dilatedTflite, guidedEdges)
                    
                    // Combină marginile ghidate cu marginile Canny originale (OR) pentru a nu pierde informație
                    val finalCombined = Mat()
                    Core.bitwise_or(guidedEdges, combinedEdges, finalCombined)
                    
                    Timber.d("OpenCV: Used TFLite mask as guide (dilated, kernel=${kernelSize}x${kernelSize}) for Canny edges")
                    
                    // OPTIMIZARE #2: Clarificare eliberări Mat (separare clară)
                    if (scaledTfliteMask != tfliteMask) {
                        scaledTfliteMask.recycle()
                    }
                    tfliteGray.release()
                    tfliteBinary.release()
                    dilatedTflite.release()
                    dilateKernel.release()
                    guidedEdges.release()
                    // NOTĂ: combinedEdges NU se eliberează aici dacă e edgesMat (același obiect)
                    // Se va elibera la final doar dacă e adaptiveMat
                    
                    finalCombined
                } catch (e: Exception) {
                    Timber.w("⚠️ Failed to use TFLite mask as guide, using Canny only: ${e.message}")
                    combinedEdges
                }
            } else {
                combinedEdges
            }
            
            // STEP 7: Find contours
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                finalEdgesMat,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            Timber.d("OpenCV: Found ${contours.size} contours")
            
            if (contours.isEmpty()) {
                Timber.w("⚠️ No contours found - returning null")
                grayMat.release()
                blurredMat.release()
                edgesMat.release()
                hierarchy.release()
                sourceMat.release()
                if (workingBitmap != cropBitmap) {
                    workingBitmap.recycle()
                }
                return@withContext null
            }
            
            // STEP 8: Filtrare contururi (largest + aspect ratio ~0.65, dar relaxat)
            // PROBLEMA #2 FIX: approxPolyDP doar pe candidatele mari, nu pe toate
            val targetAspectRatio = 0.65
            val aspectRatioTolerance = 0.25  // ±25% toleranță (relaxat pentru a nu elimina contururi valide)
            var bestContour: MatOfPoint? = null
            var maxArea = 0.0
            
            // Prima trecere: găsește cel mai mare contur (prioritate principală)
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area < 500) continue  // Ignoră contururi prea mici (minim 500 pixeli)
                
                if (area > maxArea) {
                    maxArea = area
                    bestContour = contour
                }
            }
            
            // A doua trecere: dacă există un contur mare cu aspect ratio bun, preferă-l
            if (bestContour != null && maxArea > 0) {
                var bestScore = 0.0
                var bestContourWithAspect: MatOfPoint? = null
                
                for (contour in contours) {
                    val area = Imgproc.contourArea(contour)
                    if (area < maxArea * 0.3) continue  // Ignoră contururi prea mici (< 30% din cel mai mare)
                    
                    val rect = Imgproc.boundingRect(contour)
                    val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
                    val aspectRatioDiff = kotlin.math.abs(aspectRatio - targetAspectRatio)
                    
                    // Scor combinat: 70% arie, 30% aspect ratio
                    val areaScore = area / maxArea
                    val aspectScore = if (aspectRatioDiff < aspectRatioTolerance) {
                        1.0 - (aspectRatioDiff / aspectRatioTolerance)
                    } else {
                        0.0
                    }
                    val combinedScore = areaScore * 0.7 + aspectScore * 0.3
                    
                    if (combinedScore > bestScore) {
                        bestScore = combinedScore
                        bestContourWithAspect = contour
                    }
                }
                
                // Dacă găsim un contur cu aspect ratio bun și arie suficientă, folosește-l
                if (bestContourWithAspect != null && bestScore > 0.5) {
                    bestContour = bestContourWithAspect
                    Timber.d("OpenCV: Selected contour with good aspect ratio (score: ${String.format("%.2f", bestScore)})")
                } else {
                    Timber.d("OpenCV: Using largest contour (area: ${String.format("%.0f", maxArea)})")
                }
            }
            
            // Fallback final: cel mai mare contur, indiferent de aspect ratio
            if (bestContour == null) {
                for (contour in contours) {
                    val area = Imgproc.contourArea(contour)
                    if (area > maxArea) {
                        maxArea = area
                        bestContour = contour
                    }
                }
            }
            
            // STEP 9: approxPolyDP doar pe candidatele mari (PROBLEMA #2 FIX)
            // Calculează epsilon bazat pe perimetrul celui mai mare contur
            val epsilon = if (bestContour != null) {
                val contour2f = MatOfPoint2f(*bestContour.toArray())
                val perimeter = Imgproc.arcLength(contour2f, true)
                contour2f.release()
                0.005 * perimeter  // 0.5% din perimetru (mai precis decât 2%)
            } else {
                2.0  // Fallback
            }
            
            // Aplică approxPolyDP doar pe bestContour (nu pe toate)
            val smoothedContour = if (bestContour != null) {
                try {
                    val points = bestContour.toArray()
                    val contour2f = MatOfPoint2f(*points)
                    val approx2f = MatOfPoint2f()
                    Imgproc.approxPolyDP(contour2f, approx2f, epsilon, true)
                    val approxPoints = approx2f.toArray()
                    
                    // PROBLEMA #3 FIX: Convex hull pentru hanger (zona superioară)
                    val rect = Imgproc.boundingRect(MatOfPoint(*approxPoints))
                    val topThreshold = workingH * 0.15  // Primele 15% din înălțime
                    val approxArea = Imgproc.contourArea(MatOfPoint(*approxPoints))
                    val originalArea = Imgproc.contourArea(bestContour)
                    val areaLoss = (originalArea - approxArea) / originalArea
                    
                    // Dacă zona superioară e afectată SAU pierdere de arie > 10%, aplică convex hull pe zona superioară
                    val needsConvexHull = rect.y < topThreshold || areaLoss > 0.1
                    
                    if (needsConvexHull) {
                        val hull = MatOfInt()
                        Imgproc.convexHull(MatOfPoint(*approxPoints), hull)
                        
                        // Extrage punctele din hull
                        val hullPoints = hull.toArray().map { idx -> approxPoints[idx] }.toTypedArray()
                        val finalContour = MatOfPoint(*hullPoints)
                        
                        contour2f.release()
                        approx2f.release()
                        hull.release()
                        
                        Timber.d("OpenCV: Applied convex hull for hanger protection (area loss: ${String.format("%.1f", areaLoss * 100)}%)")
                        finalContour
                    } else {
                        contour2f.release()
                        approx2f.release()
                        MatOfPoint(*approxPoints)
                    }
                } catch (e: Exception) {
                    Timber.w("⚠️ approxPolyDP/convexHull failed, using original contour: ${e.message}")
                    bestContour
                }
            } else {
                bestContour
            }
            
            if (smoothedContour == null) {
                Timber.w("⚠️ No contour after smoothing - returning null")
                grayMat.release()
                claheMat.release()
                blurredMat.release()
                if (combinedEdges != edgesMat) {
                    adaptiveMat.release()
                }
                edgesMat.release()
                if (finalEdgesMat != combinedEdges && finalEdgesMat != edgesMat) {
                    finalEdgesMat.release()
                }
                hierarchy.release()
                sourceMat.release()
                contours.forEach { it.release() }
                if (workingBitmap != cropBitmap) {
                    workingBitmap.recycle()
                }
                return@withContext null
            }
            
            bestContour = smoothedContour
            
            // STEP 10: Scale contur înapoi la rezoluția originală dacă e necesar
            val scaledContour = if (scaleFactor != 1.0) {
                val points = bestContour.toArray()
                val scaledPoints = points.map { point ->
                    Point(point.x * scaleFactor, point.y * scaleFactor)
                }.toTypedArray()
                MatOfPoint(*scaledPoints)
            } else {
                bestContour
            }
            
            // STEP 7: Creează mască high-res din contur
            val maskMat = Mat.zeros(originalH, originalW, CvType.CV_8UC1)
            Imgproc.drawContours(
                maskMat,
                listOf(scaledContour),
                -1,
                Scalar(255.0),
                -1  // Fill contour
            )
            
            val whitePixels = Core.countNonZero(maskMat)
            val totalPixels = originalW * originalH
            val whitePercent = if (totalPixels > 0) (whitePixels * 100.0 / totalPixels) else 0.0
            Timber.d("OpenCV: High-res mask created: ${whitePixels}/${totalPixels} white (${String.format("%.2f", whitePercent)}%)")
            android.util.Log.d("OpenCVMaskProcessor", "OpenCV: High-res contour mask: ${whitePixels}/${totalPixels} white (${String.format("%.2f", whitePercent)}%)")
            
            // Convert Mat back to Bitmap
            val resultBitmap = Bitmap.createBitmap(originalW, originalH, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(maskMat, resultBitmap)
            
            // Cleanup (OPTIMIZARE #2: Clarificare eliberări Mat)
            grayMat.release()
            claheMat.release()
            blurredMat.release()
            // combinedEdges poate fi edgesMat SAU adaptiveMat - eliberăm doar dacă e adaptiveMat
            if (combinedEdges == adaptiveMat) {
                adaptiveMat.release()
            } else {
                edgesMat.release()
            }
            // finalEdgesMat poate fi combinedEdges SAU finalCombined - eliberăm doar dacă e diferit
            if (finalEdgesMat != combinedEdges && finalEdgesMat != edgesMat) {
                finalEdgesMat.release()
            }
            hierarchy.release()
            sourceMat.release()
            maskMat.release()
            contours.forEach { it.release() }
            if (scaledContour != bestContour) {
                scaledContour.release()
            }
            if (workingBitmap != cropBitmap) {
                workingBitmap.recycle()
            }
            
            return@withContext resultBitmap
            
        } catch (e: Exception) {
            Timber.e(e, "❌ OpenCV contour detection failed")
            return@withContext null
        }
    }
}

