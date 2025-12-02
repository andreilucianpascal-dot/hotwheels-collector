package com.example.hotwheelscollectors.utils

import androidx.compose.ui.graphics.Color

/**
 * A centralized object for managing dynamic color palettes for categories and brands.
 * This makes it easy to maintain a consistent and vibrant color scheme across the app.
 */
object CategoryColors {

    // A default color to fall back on if a specific color is not defined.
    val defaultColor = Color(0xFF455A64) // A neutral blue-grey

    // 1. --- Color Palette for Car Series (Categories) ---
    // Maps a series ID (lowercase) to a specific color.
    private val seriesColors = mapOf(
        "rally" to Color(0xFFD32F2F),             // Strong Red
        "supercars" to Color(0xFFFFC107),        // Amber Yellow
        "american_muscle" to Color(0xFF1976D2),   // Strong Blue
        "convertible" to Color(0xFF388E3C),       // Strong Green
        "hw_exotics" to Color(0xFF7B1FA2),        // Purple
        "suv_trucks" to Color(0xFFF57C00),      // Orange
        "team_transport" to Color(0xFF00796B),    // Teal
        "car_culture" to Color(0xFF512DA8),      // Deep Purple
        "fast_furious" to Color(0xFF000000)       // Black for Fast & Furious
        // Add other series here...
    )

    // 2. --- Color Palette for Brands ---
    // Maps a brand ID (lowercase) to a specific color.
    private val brandColors = mapOf(
        // Iconic Colors
        "ferrari" to Color(0xFFDC0000),         // Ferrari Red
        "lamborghini" to Color(0xFFDDB60A),       // Lamborghini Gold/Yellow
        "porsche" to Color(0xFFB1B6B8),         // Porsche Silver/Grey
        "bmw" to Color(0xFF0066B1),             // BMW Blue
        "ford" to Color(0xFF003478),             // Ford Blue
        "honda" to Color(0xFFE40521),             // Honda Red
        "nissan" to Color(0xFFc3002f),             // Nissan Red
        "subaru" to Color(0xFF013A71),             // Subaru Blue
        "toyota" to Color(0xFFEB0A1E),             // Toyota Red
        "mercedes" to Color(0xFFC0C0C0),         // Mercedes Silver
        "audi" to Color(0xFFBB0000),             // Audi Red
        "volkswagen" to Color(0xFF1E4D8B)        // VW Blue
        // Add other brands here...
    )

    /**
     * Retrieves the color for a given car series ID.
     * Falls back to a default color if the ID is not found.
     */
    fun getSeriesColor(seriesId: String): Color {
        return seriesColors[seriesId.lowercase()] ?: defaultColor
    }

    /**
     * Retrieves the color for a given brand ID.
     * Falls back to a default color if the ID is not found.
     */
    fun getBrandColor(brandId: String): Color {
        return brandColors[brandId.lowercase()] ?: getSeriesColor(brandId) // Fallback to series color if brand not specific
    }

    /**
     * Calculates the best contrasting text color (black or white) for a given background color.
     * This is crucial for accessibility and readability.
     *
     * @param backgroundColor The color of the background.
     * @return [Color.White] or [Color.Black] depending on the background's luminance.
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        // Formula for calculating luminance
        val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
        // If luminance is high (bright background), use black text. Otherwise, use white text.
        return if (luminance > 0.5) Color.Black else Color.White
    }
}
