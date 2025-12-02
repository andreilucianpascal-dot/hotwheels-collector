package com.example.hotwheelscollectors.utils

import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.GlobalBarcodeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalBarcodeDiscoveryService @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val carDetailsExtractor: CarDetailsExtractor,
    private val smartCategorizer: SmartCategorizer,
) {

    /**
     * Main discovery flow - checks global database first, then performs smart analysis
     */
    suspend fun discoverCarFromBarcode(
        barcode: String,
        detectedText: String? = null,
    ): DiscoveryResult {
        // Step 1: Check if barcode exists in global database
        val globalResult = firestoreRepository.checkBarcodeInGlobalDatabase(barcode)

        if (globalResult != null) {
            return DiscoveryResult.KnownCar(
                barcode = barcode,
                globalData = globalResult,
                suggestedSaveLocation = determineSaveLocationFromGlobal(globalResult)
            )
        }

        // Step 2: New car - perform smart analysis if text is available
        val smartAnalysis = if (detectedText != null) {
            carDetailsExtractor.extractCarDetails(detectedText)
        } else null

        return DiscoveryResult.NewCar(
            barcode = barcode,
            smartAnalysis = smartAnalysis,
            requiresContribution = true
        )
    }

    /**
     * Save new car contribution to global database
     */
    suspend fun contributeToGlobalDatabase(
        barcode: String,
        carName: String,
        brand: String,
        series: String,
        year: Int,
        color: String?,
        frontPhotoUrl: String?,
        backPhotoUrl: String?,
        croppedBarcodeUrl: String?,
        category: String,
        subcategory: String?,
    ): Result<Unit> {
        return firestoreRepository.saveToGlobalDatabase(
            barcode = barcode,
            carName = carName,
            brand = brand,
            series = series,
            year = year,
            color = color,
            frontPhotoUrl = frontPhotoUrl,
            backPhotoUrl = backPhotoUrl,
            croppedBarcodeUrl = croppedBarcodeUrl,
            category = category,
            subcategory = subcategory
        )
    }

    /**
     * Generate smart suggestions for new car form filling
     */
    fun generateSmartSuggestions(
        barcode: String,
        detectedText: String?,
    ): SmartFormSuggestions {
        val barcodeAnalysis = smartCategorizer.categorizeCarAutomatically(
            barcode = barcode,
            detectedText = detectedText?.split("\n") ?: emptyList()
        )

        val textAnalysis = detectedText?.let {
            carDetailsExtractor.extractCarDetails(it)
        }

        return SmartFormSuggestions(
            suggestedBrand = textAnalysis?.brand,
            suggestedModel = textAnalysis?.model,
            suggestedYear = textAnalysis?.year,
            suggestedSeries = textAnalysis?.series,
            suggestedColor = textAnalysis?.color,
            suggestedCategory = barcodeAnalysis.category,
            suggestedSubcategory = determineSubcategoryFromCategory(barcodeAnalysis.category),
            confidence = maxOf(
                barcodeAnalysis.confidence,
                textAnalysis?.confidence ?: 0.0f
            )
        )
    }

    /**
     * Determine save location from global database result
     */
    private fun determineSaveLocationFromGlobal(globalResult: GlobalBarcodeResult): SaveLocation {
        val categoryType = when (globalResult.category) {
            "mainline" -> CategoryType.MAINLINE
            "premium" -> CategoryType.PREMIUM
            "others" -> CategoryType.OTHERS
            "hot_roads" -> CategoryType.HOT_ROADS
            else -> CategoryType.UNKNOWN
        }

        return SaveLocation(
            category = categoryType,
            series = globalResult.subcategory,
            brand = globalResult.brand,
            requiresUserSelection = false,
            confidence = if (globalResult.verificationCount > 3) 0.95f else 0.85f
        )
    }

    /**
     * Determine subcategory from main category
     */
    private fun determineSubcategoryFromCategory(categoryType: CategoryType): String? {
        return when (categoryType) {
            CategoryType.MAINLINE -> "mainline_general"
            CategoryType.PREMIUM -> "premium_general"
            CategoryType.OTHERS -> "others_general"
            CategoryType.HOT_ROADS -> null
            CategoryType.UNKNOWN -> null
        }
    }

    /**
     * Validate barcode format for die-cast cars
     */
    fun isValidDiecastBarcode(barcode: String): Boolean {
        val cleanBarcode = barcode.trim()

        return when {
            // Mainline patterns
            cleanBarcode.matches(Regex("^887961\\d{6}$")) -> true
            // Premium patterns
            cleanBarcode.matches(Regex("^194735\\d{6}$")) -> true
            // Others patterns
            cleanBarcode.matches(Regex("^(736313|746775)\\d{6}$")) -> true
            // Generic UPC pattern
            cleanBarcode.matches(Regex("^\\d{12}$")) -> true
            else -> false
        }
    }

    /**
     * Extract potential car info from barcode pattern analysis
     */
    fun analyzeBarcode(barcode: String): BarcodeAnalysis {
        val cleanBarcode = barcode.trim()

        return when {
            cleanBarcode.matches(Regex("^887961\\d{6}$")) -> {
                BarcodeAnalysis(
                    isValid = true,
                    category = "Mainline",
                    confidence = 0.9f,
                    estimatedYear = extractYearFromBarcode(cleanBarcode),
                    productLine = "Die-cast Basic"
                )
            }

            cleanBarcode.matches(Regex("^194735\\d{6}$")) -> {
                BarcodeAnalysis(
                    isValid = true,
                    category = "Premium",
                    confidence = 0.9f,
                    estimatedYear = extractYearFromBarcode(cleanBarcode),
                    productLine = "Die-cast Premium"
                )
            }

            cleanBarcode.matches(Regex("^(736313|746775)\\d{6}$")) -> {
                BarcodeAnalysis(
                    isValid = true,
                    category = "Others",
                    confidence = 0.8f,
                    estimatedYear = extractYearFromBarcode(cleanBarcode),
                    productLine = "Die-cast Others"
                )
            }

            else -> {
                BarcodeAnalysis(
                    isValid = false,
                    category = "Unknown",
                    confidence = 0.0f,
                    estimatedYear = null,
                    productLine = "Unknown"
                )
            }
        }
    }

    /**
     * Extract estimated year from barcode pattern
     */
    private fun extractYearFromBarcode(barcode: String): Int? {
        // This is simplified - real implementation would use actual Mattel patterns
        val lastTwoDigits = barcode.takeLast(2).toIntOrNull() ?: return null

        return when {
            lastTwoDigits in 20..24 -> 2020 + (lastTwoDigits - 20)
            lastTwoDigits in 15..19 -> 2015 + (lastTwoDigits - 15)
            else -> null
        }
    }
}

// Data classes for discovery results
sealed class DiscoveryResult {
    data class KnownCar(
        val barcode: String,
        val globalData: GlobalBarcodeResult,
        val suggestedSaveLocation: SaveLocation,
    ) : DiscoveryResult()

    data class NewCar(
        val barcode: String,
        val smartAnalysis: AutoDetectedDetails?,
        val requiresContribution: Boolean,
    ) : DiscoveryResult()
}

data class SmartFormSuggestions(
    val suggestedBrand: String?,
    val suggestedModel: String?,
    val suggestedYear: Int?,
    val suggestedSeries: String?,
    val suggestedColor: String?,
    val suggestedCategory: CategoryType,
    val suggestedSubcategory: String?,
    val confidence: Float,
)

data class BarcodeAnalysis(
    val isValid: Boolean,
    val category: String,
    val confidence: Float,
    val estimatedYear: Int?,
    val productLine: String,
)