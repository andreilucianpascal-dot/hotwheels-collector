package com.example.hotwheelscollectors.utils

import com.example.hotwheelscollectors.model.HotWheelsCar
import com.example.hotwheelscollectors.model.StorageType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartCategorizer @Inject constructor() {

    /**
     * Main entry point for smart categorization
     * Returns SaveLocation with category, series, and brand information
     */
    fun categorizeCarAutomatically(
        barcode: String?,
        detectedText: List<String>,
        userProvidedInfo: CarUserInfo? = null,
        storageType: StorageType = StorageType.LOCAL,
    ): SaveLocation {

        // Step 1: Try barcode analysis first (most reliable)
        barcode?.let { code ->
            val barcodeResult = analyzeBarcode(code, storageType)
            if (barcodeResult.confidence > 0.8f) {
                return barcodeResult.location
            }
        }

        // Step 2: Analyze detected text from photo
        val textResult = analyzeDetectedText(detectedText, storageType)
        if (textResult.confidence > 0.7f) {
            return textResult.location
        }

        // Step 3: Use user-provided information
        userProvidedInfo?.let { info ->
            val userResult = analyzeUserInfo(info, storageType)
            if (userResult.confidence > 0.6f) {
                return userResult.location
            }
        }

        // Step 4: Fallback - require user selection
        return SaveLocation(
            category = CategoryType.UNKNOWN,
            series = null,
            brand = null,
            storageType = storageType,
            requiresUserSelection = true,
            confidence = 0.0f
        )
    }

    /**
     * Analyze die-cast car barcode patterns
     */
    private fun analyzeBarcode(barcode: String, storageType: StorageType): CategoryResult {
        val cleanBarcode = barcode.trim()

        // Real die-cast car barcode patterns
        when {
            // Mainline patterns (887961XXXXXX)
            cleanBarcode.matches(Regex("^887961\\d{6}$")) -> {
                val carInfo = getMainlineInfoFromBarcode(cleanBarcode)
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.MAINLINE,
                        series = carInfo.series,
                        brand = carInfo.brand,
                        storageType = storageType,
                        requiresUserSelection = carInfo.brand == null,
                        confidence = if (carInfo.brand != null) 0.9f else 0.7f
                    ),
                    confidence = 0.9f
                )
            }

            // Premium series patterns
            cleanBarcode.matches(Regex("^194735\\d{6}$")) -> {
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.PREMIUM,
                        series = determinePremiumSeries(cleanBarcode),
                        brand = null,
                        storageType = storageType,
                        requiresUserSelection = false,
                        confidence = 0.9f
                    ),
                    confidence = 0.9f
                )
            }

            // Team Transport (specific pattern)
            cleanBarcode.matches(Regex("^(194735|887961)\\d{6}$")) && isTeamTransportBarcode(
                cleanBarcode
            ) -> {
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.PREMIUM,
                        series = "team_transport",
                        brand = null,
                        storageType = storageType,
                        requiresUserSelection = false,
                        confidence = 0.95f
                    ),
                    confidence = 0.95f
                )
            }

            // Others category (different patterns)
            cleanBarcode.matches(Regex("^(736313|746775)\\d{6}$")) -> {
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.OTHERS,
                        series = null,
                        brand = null,
                        storageType = storageType,
                        requiresUserSelection = true,
                        confidence = 0.8f
                    ),
                    confidence = 0.8f
                )
            }

            else -> {
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.UNKNOWN,
                        series = null,
                        brand = null,
                        storageType = storageType,
                        requiresUserSelection = true,
                        confidence = 0.0f
                    ),
                    confidence = 0.0f
                )
            }
        }
    }

    /**
     * Analyze text detected from packaging photos
     */
    private fun analyzeDetectedText(detectedText: List<String>, storageType: StorageType): CategoryResult {
        val allText = detectedText.joinToString(" ").lowercase()

        // Premium series detection
        val premiumSeries = mapOf(
            "hw exotics" to "hw_exotics",
            "exotics" to "hw_exotics",
            "team transport" to "team_transport",
            "car culture" to "car_culture",
            "premium" to "premium_general",
            "fast & furious" to "fast_furious",
            "fast and furious" to "fast_furious",
            "boulevard" to "boulevard",
            "art cars" to "art_cars"
        )

        for ((textPattern, seriesId) in premiumSeries) {
            if (allText.contains(textPattern)) {
                return CategoryResult(
                    location = SaveLocation(
                        category = CategoryType.PREMIUM,
                        series = seriesId,
                        brand = extractBrandFromText(allText),
                        storageType = storageType,
                        requiresUserSelection = false,
                        confidence = 0.85f
                    ),
                    confidence = 0.85f
                )
            }
        }

        // Mainline detection
        val mainlineIndicators = listOf("mainline", "basic", "2024", "2023", "2025")
        if (mainlineIndicators.any { allText.contains(it) }) {
            val brand = extractBrandFromText(allText)
            val category = brand?.let { determineCategoryFromBrand(it) } ?: "unknown"

            return CategoryResult(
                location = SaveLocation(
                    category = CategoryType.MAINLINE,
                    series = category,
                    brand = brand,
                    storageType = storageType,
                    requiresUserSelection = brand == null,
                    confidence = if (brand != null) 0.8f else 0.6f
                ),
                confidence = 0.8f
            )
        }

        // Others category detection
        val othersIndicators = listOf("trucks", "buses", "motorcycles", "planes", "boats")
        if (othersIndicators.any { allText.contains(it) }) {
            return CategoryResult(
                location = SaveLocation(
                    category = CategoryType.OTHERS,
                    series = null,
                    brand = null,
                    storageType = storageType,
                    requiresUserSelection = true,
                    confidence = 0.7f
                ),
                confidence = 0.7f
            )
        }

        return CategoryResult(
            location = SaveLocation(
                category = CategoryType.UNKNOWN,
                series = null,
                brand = null,
                storageType = storageType,
                requiresUserSelection = true,
                confidence = 0.0f
            ),
            confidence = 0.0f
        )
    }

    /**
     * Analyze user-provided information
     */
    private fun analyzeUserInfo(userInfo: CarUserInfo, storageType: StorageType): CategoryResult {
        val brand = userInfo.brand?.lowercase()
        val name = userInfo.name?.lowercase()
        val series = userInfo.series?.lowercase()

        // Determine category from brand
        val category = brand?.let { determineCategoryFromBrand(it) }

        return CategoryResult(
            location = SaveLocation(
                category = when {
                    userInfo.isPremium == true -> CategoryType.PREMIUM
                    category != null -> CategoryType.MAINLINE
                    else -> CategoryType.UNKNOWN
                },
                series = when {
                    userInfo.isPremium == true -> series
                    category != null -> category
                    else -> null
                },
                brand = brand,
                storageType = storageType,
                requiresUserSelection = category == null,
                confidence = if (category != null) 0.8f else 0.5f
            ),
            confidence = 0.8f
        )
    }

    /**
     * Extract brand name from detected text
     */
    private fun extractBrandFromText(text: String): String? {
        val knownBrands = listOf(
            // Supercars
            "ferrari", "lamborghini", "porsche", "mclaren", "bugatti", "pagani",
            "koenigsegg", "aston martin", "bentley", "maserati", "lotus", "alpine", "rimac",

            // Rally
            "subaru", "mitsubishi", "toyota", "ford", "audi", "bmw",
            "volkswagen", "lancia", "peugeot", "citroen", "mazda", "volvo",
            "skoda", "seat", "renault", "opel", "fiat", "alfa romeo",

            // American Muscle
            "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac",
            "corvette", "camaro", "mustang", "challenger", "plymouth", "oldsmobile",
            "mercury", "lincoln", "shelby",

            // Japanese Performance
            "honda", "nissan", "mazda", "toyota", "mitsubishi", "subaru",
            "acura", "infiniti", "lexus", "mazdaspeed", "nismo",

            // European Luxury
            "mercedes", "bmw", "audi", "porsche", "volkswagen", "jaguar",
            "land rover", "range rover", "bentley", "rolls royce", "mini",
            "smart", "alfa romeo", "fiat", "lancia", "maserati",

            // SUVs & Trucks
            "hummer", "jeep", "ram", "gmc", "land rover", "range rover",
            "ford", "chevrolet", "dodge", "toyota", "nissan", "honda",
            "mazda", "subaru", "mitsubishi", "volkswagen", "audi", "bmw",

            // Motorcycles
            "honda", "yamaha", "kawasaki", "suzuki", "ducati", "harley davidson",
            "bmw", "ktm", "aprilia", "triumph",

            // Racing & Motorsport
            "formula 1", "indycar", "nascar", "drift", "drag",

            // Movie & TV Cars
            "batmobile", "kitt", "delorean", "ecto-1", "ghostbusters",
            "fast furious", "transformers", "james bond", "007",

            // Classic & Vintage
            "classic", "vintage", "retro", "heritage", "legacy", "iconic"
        )

        return knownBrands.find { brand ->
            text.contains(brand, ignoreCase = true)
        }
    }

    /**
     * Determine mainline category from brand
     */
    private fun determineCategoryFromBrand(brand: String): String? {
        val brandLower = brand.lowercase()

        return when (brandLower) {
            in listOf(
                "ferrari", "lamborghini", "porsche", "mclaren", "bugatti", "pagani",
                "koenigsegg", "aston_martin", "bentley", "maserati", "lotus", "alpine",
                 "rimac"
            ) -> "supercars"

            in listOf(
                "subaru", "mitsubishi", "toyota", "ford", "audi", "bmw",
                "volkswagen", "lancia", "peugeot", "citroen", "mazda", "volvo",
                "skoda", "seat", "renault", "opel", "fiat", "alfa_romeo"
            ) -> "rally"

            in listOf(
                "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac",
                "corvette", "camaro", "mustang", "challenger", "plymouth", "oldsmobile",
                "mercury", "lincoln", "shelby"
            ) -> "american_muscle"

            in listOf(
                "honda", "nissan", "mazda", "toyota", "mitsubishi", "subaru",
                "acura", "infiniti", "lexus", "mazdaspeed", "nismo"
            ) -> "japanese_performance"

            in listOf(
                "mercedes", "bmw", "audi", "porsche", "volkswagen", "jaguar",
                "land_rover", "range_rover", "bentley", "rolls_royce", "mini",
                "smart", "alfa_romeo", "fiat", "lancia", "maserati"
            ) -> "european_luxury"

            in listOf(
                "hummer", "jeep", "ram", "gmc", "land_rover", "range_rover",
                "ford", "chevrolet", "dodge", "toyota", "nissan", "honda",
                "mazda", "subaru", "mitsubishi", "volkswagen", "audi", "bmw"
            ) -> "suv_trucks"

            in listOf(
                "honda", "yamaha", "kawasaki", "suzuki", "ducati", "harley_davidson",
                "bmw", "ktm", "aprilia", "triumph"
            ) -> "motorcycle"

            in listOf(
                "hot_wheels", "hw", "mattel", "original", "fantasy", "concept"
            ) -> "hot_wheels_originals"

            in listOf(
                "formula_1", "indycar","nascar", "drag"
            ) -> "racing_motorsport"

            in listOf(
                "batmobile", "kitt", "delorean", "ecto-1", "ghostbusters",
                "fast_furious", "transformers", "james_bond", "007"
            ) -> "movie_tv_cars"

            in listOf(
                "classic", "vintage", "retro", "heritage", "legacy", "iconic"
            ) -> "classic_vintage"

            else -> null
        }
    }

    /**
     * Get mainline car info from barcode (simplified)
     */
    private fun getMainlineInfoFromBarcode(barcode: String): MainlineCarInfo {
        // This would typically query a comprehensive die-cast car database
        // For now, we'll use basic pattern analysis
        val lastSixDigits = barcode.takeLast(6)

        return MainlineCarInfo(
            series = "mainline_2024",
            brand = "Unknown", // Default brand to avoid Select Brand dialog
            year = 2024
        )
    }

    /**
     * Determine premium series from barcode
     */
    private fun determinePremiumSeries(barcode: String): String? {
        // This would be enhanced with a comprehensive database
        return "premium_general"
    }

    /**
     * Check if barcode is Team Transport
     */
    private fun isTeamTransportBarcode(barcode: String): Boolean {
        // Team Transport has specific barcode ranges
        val lastSix = barcode.takeLast(6).toIntOrNull() ?: return false
        return lastSix in 100000..199999 // Example range
    }

    /**
     * Generate fallback suggestions when auto-categorization fails
     */
    fun generateCategorySuggestions(
        detectedText: List<String>,
        userInfo: CarUserInfo?,
    ): List<CategorySuggestion> {
        val suggestions = mutableListOf<CategorySuggestion>()

        // Add mainline suggestions
        suggestions.add(
            CategorySuggestion(
                category = CategoryType.MAINLINE,
                subcategories = listOf(
                    "rally",
                    "supercars",
                    "american_muscle",
                    "suv_trucks",
                    "vans",
                    "motorcycle",
                    "convertible"
                ),
                confidence = 0.7f,
                reason = "Most die-cast cars are mainline"
            )
        )

        // Add premium suggestions based on text
        val allText = detectedText.joinToString(" ").lowercase()
        if (allText.contains("premium") || allText.contains("exotics") || allText.contains("transport")) {
            suggestions.add(
                0,
                CategorySuggestion(
                    category = CategoryType.PREMIUM,
                    subcategories = listOf(
                        "hw_exotics",
                        "team_transport",
                        "car_culture",
                        "fast_furious"
                    ),
                    confidence = 0.9f,
                    reason = "Detected premium series indicators"
                )
            )
        }

        // Add others suggestions
        suggestions.add(
            CategorySuggestion(
                category = CategoryType.OTHERS,
                subcategories = listOf("trucks", "buses", "motorcycles", "planes"),
                confidence = 0.5f,
                reason = "For non-car vehicles"
            )
        )

        return suggestions
    }
}

// Data classes
data class SaveLocation(
    val category: CategoryType,
    val series: String? = null,
    val brand: String? = null,
    val storageType: StorageType = StorageType.LOCAL,
    val requiresUserSelection: Boolean = false,
    val confidence: Float = 0.0f,
)

data class CategoryResult(
    val location: SaveLocation,
    val confidence: Float,
)

data class CarUserInfo(
    val name: String? = null,
    val brand: String? = null,
    val series: String? = null,
    val isPremium: Boolean? = null,
)

data class MainlineCarInfo(
    val series: String,
    val brand: String?,
    val year: Int,
)

data class CategorySuggestion(
    val category: CategoryType,
    val subcategories: List<String>,
    val confidence: Float,
    val reason: String,
)

enum class CategoryType {
    MAINLINE,
    PREMIUM,
    OTHERS,
    HOT_ROADS,
    UNKNOWN
}