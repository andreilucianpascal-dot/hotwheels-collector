package com.example.hotwheelscollectors.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarDetailsExtractor @Inject constructor() {

    /**
     * Extract complete car details from ML Kit detected text
     * Returns structured car information with confidence scores
     */
    fun extractCarDetails(detectedText: String): AutoDetectedDetails {
        val cleanText = detectedText.uppercase()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Extract individual components
        val brand = extractBrand(cleanText)
        val model = extractModel(cleanText, brand)
        val year = extractYear(cleanText)
        val series = extractSeries(cleanText)
        val color = extractColor(cleanText)
        val category = extractCategory(cleanText)

        // Calculate overall confidence
        val confidence = calculateOverallConfidence(brand, model, year, series)

        return AutoDetectedDetails(
            brand = brand?.brandName,
            model = model?.modelName,
            year = year?.year,
            series = series?.seriesName,
            color = color?.colorName,
            category = category?.categoryName,
            subcategory = determineSubcategory(brand?.brandName, category?.categoryName),
            confidence = confidence,
            detectedComponents = mapOf(
                "brand" to (brand?.confidence ?: 0.0f),
                "model" to (model?.confidence ?: 0.0f),
                "year" to (year?.confidence ?: 0.0f),
                "series" to (series?.confidence ?: 0.0f),
                "color" to (color?.confidence ?: 0.0f)
            )
        )
    }

    /**
     * Extract brand from detected text with confidence scoring
     */
    private fun extractBrand(text: String): BrandResult? {
        val brandPatterns = mapOf(
            // Supercars
            "FERRARI" to listOf("ferrari", "f40", "f50", "488", "portofino", "laferrari", "enzo"),
            "LAMBORGHINI" to listOf("lamborghini", "lambo", "huracan", "aventador", "gallardo", "murcielago"),
            "PORSCHE" to listOf("porsche", "911", "carrera", "turbo", "gt3", "cayman", "boxster"),
            "MCLAREN" to listOf("mclaren", "720s", "650s", "p1", "570s"),
            "BUGATTI" to listOf("bugatti", "veyron", "chiron"),
            "PAGANI" to listOf("pagani", "zonda", "huayra"),
            "KOENIGSEGG" to listOf("koenigsegg", "regera", "agera", "ccx"),
            "ASTON MARTIN" to listOf("aston martin", "db11", "vantage", "dbs"),
            
            // Rally
            "SUBARU" to listOf("subaru", "wrx", "sti", "impreza", "outback", "legacy"),
            "MITSUBISHI" to listOf("mitsubishi", "evo", "evolution", "lancer", "eclipse"),
            "TOYOTA" to listOf("toyota", "supra", "celica", "corolla", "yaris", "prius", "camry"),
            "FORD" to listOf("ford", "focus", "fiesta", "mustang", "gt", "raptor", "f-150"),
            "AUDI" to listOf("audi", "quattro", "rs4", "rs6", "tt", "a3", "a4", "r8"),
            "BMW" to listOf("bmw", "m3", "m5", "x5", "i8", "z4", "series"),
            "VOLKSWAGEN" to listOf("volkswagen", "vw", "golf", "beetle", "jetta"),
            
            // American Muscle
            "CHEVROLET" to listOf("chevrolet", "chevy", "corvette", "camaro", "chevelle", "nova", "impala"),
            "DODGE" to listOf("dodge", "challenger", "charger", "viper", "demon", "hellcat"),
            "CHRYSLER" to listOf("chrysler", "300", "pacifica"),
            "PONTIAC" to listOf("pontiac", "firebird", "gto", "trans am"),
            "BUICK" to listOf("buick", "grand national", "regal", "skylark"),
            "CADILLAC" to listOf("cadillac", "escalade", "cts", "ats"),
            
            // SUV & Trucks
            "JEEP" to listOf("jeep", "wrangler", "cherokee", "grand cherokee", "compass"),
            "HUMMER" to listOf("hummer", "h1", "h2", "h3"),
            "RAM" to listOf("ram", "1500", "2500", "3500", "rebel"),
            "GMC" to listOf("gmc", "sierra", "yukon", "terrain"),
            "LAND ROVER" to listOf("land rover", "defender", "discovery", "range rover"),
            
            // Luxury
            "MERCEDES" to listOf("mercedes", "benz", "amg", "c-class", "e-class", "s-class"),
            "LEXUS" to listOf("lexus", "lfa", "ls", "rx", "nx"),
            "INFINITI" to listOf("infiniti", "g35", "g37", "q50", "q60"),
            "ACURA" to listOf("acura", "nsx", "tsx", "tlx"),
            
            // Motorcycles  
            "HONDA" to listOf("honda", "cbr", "goldwing", "shadow", "rebel"),
            "YAMAHA" to listOf("yamaha", "r1", "r6", "mt", "fz"),
            "KAWASAKI" to listOf("kawasaki", "ninja", "zx", "z1000"),
            "SUZUKI" to listOf("suzuki", "gsxr", "hayabusa", "katana"),
            "DUCATI" to listOf("ducati", "panigale", "monster", "diavel"),
            "HARLEY DAVIDSON" to listOf("harley", "davidson", "sportster", "dyna", "touring")
        )

        var bestMatch: BrandResult? = null
        var highestScore = 0.0f

        for ((brandName, indicators) in brandPatterns) {
            var score = 0.0f
            var matches = 0

            for (indicator in indicators) {
                if (text.contains(indicator.uppercase())) {
                    matches++
                    score += when (indicator) {
                        brandName.lowercase() -> 1.0f // Exact brand name match
                        else -> 0.7f // Model/indicator match
                    }
                }
            }

            if (matches > 0) {
                val confidence = (score / indicators.size).coerceAtMost(1.0f)
                if (confidence > highestScore) {
                    highestScore = confidence
                    bestMatch = BrandResult(brandName, confidence)
                }
            }
        }

        return bestMatch
    }

    /**
     * Extract specific model based on detected brand
     */
    private fun extractModel(text: String, brand: BrandResult?): ModelResult? {
        if (brand == null) return null

        val modelPatterns = when (brand.brandName) {
            "FERRARI" -> listOf("F40", "F50", "488", "PORTOFINO", "LAFERRARI", "ENZO", "TESTAROSSA")
            "LAMBORGHINI" -> listOf("HURACAN", "AVENTADOR", "GALLARDO", "MURCIELAGO", "DIABLO")
            "FORD" -> listOf("MUSTANG", "GT", "F-150", "RAPTOR", "FOCUS", "FIESTA", "BRONCO")
            "CHEVROLET" -> listOf("CORVETTE", "CAMARO", "CHEVELLE", "NOVA", "IMPALA", "SILVERADO")
            "DODGE" -> listOf("CHALLENGER", "CHARGER", "VIPER", "DEMON", "HELLCAT", "RAM")
            "TOYOTA" -> listOf("SUPRA", "CELICA", "COROLLA", "CAMRY", "PRIUS", "HILUX")
            "BMW" -> listOf("M3", "M5", "X5", "I8", "Z4", "3 SERIES", "5 SERIES")
            "SUBARU" -> listOf("WRX", "STI", "IMPREZA", "OUTBACK", "FORESTER", "LEGACY")
            else -> emptyList()
        }

        for (model in modelPatterns) {
            if (text.contains(model)) {
                return ModelResult(model, 0.9f)
            }
        }

        return null
    }

    /**
     * Extract year from text (1990-2030 range)
     */
    private fun extractYear(text: String): YearResult? {
        val yearRegex = Regex("(19[9][0-9]|20[0-3][0-9])")
        val matches = yearRegex.findAll(text)

        return matches.firstOrNull()?.let { match ->
            val year = match.value.toInt()
            YearResult(year, if (year in 2020..2024) 0.9f else 0.7f)
        }
    }

    /**
     * Extract series information from die-cast car packaging text
     */
    private fun extractSeries(text: String): SeriesResult? {
        val seriesPatterns = mapOf(
            "HW EXOTICS" to listOf("hw exotics", "exotics"),
            "TEAM TRANSPORT" to listOf("team transport", "transport"),
            "CAR CULTURE" to listOf("car culture", "culture"),
            "FAST & FURIOUS" to listOf("fast & furious", "fast and furious", "f&f"),
            "BOULEVARD" to listOf("boulevard"),
            "ART CARS" to listOf("art cars"),
            "MAINLINE" to listOf("mainline", "basic", "regular"),
            "PREMIUM" to listOf("premium", "collector edition")
        )

        for ((seriesName, indicators) in seriesPatterns) {
            for (indicator in indicators) {
                if (text.contains(indicator.uppercase())) {
                    return SeriesResult(seriesName, 0.9f)
                }
            }
        }

        return null
    }

    /**
     * Extract color information
     */
    private fun extractColor(text: String): ColorResult? {
        val colorPatterns = listOf(
            "RED", "BLUE", "GREEN", "YELLOW", "BLACK", "WHITE", "SILVER", "GOLD",
            "ORANGE", "PURPLE", "PINK", "BROWN", "GRAY", "GREY", "CHROME", "METALLIC",
            "MATTE", "PEARL", "COPPER", "BRONZE", "LIME", "MAGENTA", "CYAN"
        )

        for (color in colorPatterns) {
            if (text.contains(color)) {
                return ColorResult(color, 0.8f)
            }
        }

        return null
    }

    /**
     * Extract category from text
     */
    private fun extractCategory(text: String): ExtractedCategoryResult? {
        val categoryIndicators = mapOf(
            "SUPERCARS" to listOf("supercar", "exotic", "sports car", "racing"),
            "RALLY" to listOf("rally", "wrc", "off-road", "dirt"),
            "AMERICAN MUSCLE" to listOf("muscle", "american", "classic", "vintage"),
            "SUV TRUCKS" to listOf("suv", "truck", "pickup", "4x4", "off road"),
            "MOTORCYCLE" to listOf("motorcycle", "bike", "motorbike", "chopper"),
            "VANS" to listOf("van", "minivan", "delivery"),
            "CONVERTIBLE" to listOf("convertible", "roadster", "cabriolet", "spyder")
        )

        for ((category, indicators) in categoryIndicators) {
            for (indicator in indicators) {
                if (text.contains(indicator.uppercase())) {
                    return ExtractedCategoryResult(category, 0.8f)
                }
            }
        }

        return null
    }

    /**
     * Determine subcategory based on brand and category
     */
    private fun determineSubcategory(brand: String?, category: String?): String? {
        if (brand == null) return null

        return when (category) {
            "SUPERCARS" -> when (brand) {
                "FERRARI", "LAMBORGHINI", "PORSCHE" -> "exotic_supercars"
                "FORD", "CHEVROLET" -> "american_supercars" 
                else -> "supercars"
            }
            "RALLY" -> when (brand) {
                "SUBARU", "MITSUBISHI" -> "japanese_rally"
                "AUDI", "VOLKSWAGEN" -> "european_rally"
                "FORD" -> "american_rally"
                else -> "rally"
            }
            else -> category?.lowercase()?.replace(" ", "_")
        }
    }

    /**
     * Calculate overall confidence based on detected components
     */
    private fun calculateOverallConfidence(
        brand: BrandResult?,
        model: ModelResult?,
        year: YearResult?,
        series: SeriesResult?
    ): Float {
        val components = listOfNotNull(
            brand?.confidence,
            model?.confidence,
            year?.confidence,
            series?.confidence
        )

        return if (components.isEmpty()) 0.0f
        else components.average().toFloat()
    }
}

// Data classes for extraction results
data class AutoDetectedDetails(
    val brand: String?,
    val model: String?,
    val year: Int?,
    val series: String?,
    val color: String?,
    val category: String?,
    val subcategory: String?,
    val confidence: Float,
    val detectedComponents: Map<String, Float>
)

data class BrandResult(val brandName: String, val confidence: Float)
data class ModelResult(val modelName: String, val confidence: Float)
data class YearResult(val year: Int, val confidence: Float)
data class SeriesResult(val seriesName: String, val confidence: Float)
data class ColorResult(val colorName: String, val confidence: Float)
data class ExtractedCategoryResult(val categoryName: String, val confidence: Float)