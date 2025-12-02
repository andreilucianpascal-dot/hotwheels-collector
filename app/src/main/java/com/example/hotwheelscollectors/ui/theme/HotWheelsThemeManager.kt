package com.example.hotwheelscollectors.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode

object HotWheelsThemeManager {
    
    // Background Themes with Images
    val backgroundThemes = mapOf(
        "hotwheels_classic" to BackgroundTheme(
            name = "Hot Wheels Classic",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B00), // Hot Wheels Orange
                    Color(0xFFFF8C00), // Dark Orange
                    Color(0xFFFF4500)  // Red Orange
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1976D2), // Blue
                    Color(0xFF42A5F5), // Light Blue
                    Color(0xFF90CAF9)  // Very Light Blue
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00), Color(0xFFFF4500)),
            secondaryColors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFF90CAF9)),
            accentColor = Color(0xFFE91E63), // Pink
            surfaceColor = Color(0xFFFFFFFF), // White
            textColor = Color(0xFF000000)    // Black
        ),
        
        "hotwheels_premium" to BackgroundTheme(
            name = "Hot Wheels Premium",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFFFF8C00)  // Dark Orange
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9C27B0), // Purple
                    Color(0xFFBA68C8), // Light Purple
                    Color(0xFFE1BEE7)  // Very Light Purple
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00)),
            secondaryColors = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8), Color(0xFFE1BEE7)),
            accentColor = Color(0xFF607D8B), // Blue Grey
            surfaceColor = Color(0xFFFAFAFA), // Light Grey
            textColor = Color(0xFF212121)    // Dark Grey
        ),
        
        "hotwheels_racing" to BackgroundTheme(
            name = "Hot Wheels Racing",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF44336), // Red
                    Color(0xFFFF5722), // Deep Orange
                    Color(0xFFFF9800)  // Orange
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF000000), // Black
                    Color(0xFF424242), // Dark Grey
                    Color(0xFF757575)  // Grey
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFFF44336), Color(0xFFFF5722), Color(0xFFFF9800)),
            secondaryColors = listOf(Color(0xFF000000), Color(0xFF424242), Color(0xFF757575)),
            accentColor = Color(0xFFFFEB3B), // Yellow
            surfaceColor = Color(0xFF121212), // Dark Surface
            textColor = Color(0xFFFFFFFF)    // White
        ),
        
        "hotwheels_vintage" to BackgroundTheme(
            name = "Hot Wheels Vintage",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF795548), // Brown
                    Color(0xFF8D6E63), // Light Brown
                    Color(0xFFD7CCC8)  // Very Light Brown
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3E2723), // Dark Brown
                    Color(0xFF5D4037), // Medium Brown
                    Color(0xFF8D6E63)  // Light Brown
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFF795548), Color(0xFF8D6E63), Color(0xFFD7CCC8)),
            secondaryColors = listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF8D6E63)),
            accentColor = Color(0xFFD84315), // Deep Orange
            surfaceColor = Color(0xFFEFEBE9), // Light Brown Surface
            textColor = Color(0xFF3E2723)    // Dark Brown Text
        ),
        
        "blue_ocean" to BackgroundTheme(
            name = "Blue Ocean",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1976D2), // Blue
                    Color(0xFF42A5F5), // Light Blue
                    Color(0xFF90CAF9)  // Very Light Blue
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00695C), // Teal
                    Color(0xFF26A69A), // Light Teal
                    Color(0xFF80CBC4)  // Very Light Teal
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFF90CAF9)),
            secondaryColors = listOf(Color(0xFF00695C), Color(0xFF26A69A), Color(0xFF80CBC4)),
            accentColor = Color(0xFF00BCD4), // Cyan
            surfaceColor = Color(0xFFE0F2F1), // Light Cyan Surface
            textColor = Color(0xFF004D40)    // Dark Teal Text
        ),
        
        "green_forest" to BackgroundTheme(
            name = "Green Forest",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF388E3C), // Green
                    Color(0xFF66BB6A), // Light Green
                    Color(0xFFA5D6A7)  // Very Light Green
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2E7D32), // Dark Green
                    Color(0xFF4CAF50), // Medium Green
                    Color(0xFF81C784)  // Light Green
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFF388E3C), Color(0xFF66BB6A), Color(0xFFA5D6A7)),
            secondaryColors = listOf(Color(0xFF2E7D32), Color(0xFF4CAF50), Color(0xFF81C784)),
            accentColor = Color(0xFF8BC34A), // Light Green
            surfaceColor = Color(0xFFE8F5E8), // Light Green Surface
            textColor = Color(0xFF1B5E20)    // Dark Green Text
        ),
        
        "purple_royal" to BackgroundTheme(
            name = "Purple Royal",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7B1FA2), // Purple
                    Color(0xFFAB47BC), // Light Purple
                    Color(0xFFCE93D8)  // Very Light Purple
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4A148C), // Dark Purple
                    Color(0xFF6A1B9A), // Medium Purple
                    Color(0xFF9C27B0)  // Purple
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC), Color(0xFFCE93D8)),
            secondaryColors = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFF9C27B0)),
            accentColor = Color(0xFFE1BEE7), // Very Light Purple
            surfaceColor = Color(0xFFF3E5F5), // Light Purple Surface
            textColor = Color(0xFF4A148C)    // Dark Purple Text
        ),
        
        "sunset_warm" to BackgroundTheme(
            name = "Sunset Warm",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF5722), // Deep Orange
                    Color(0xFFFF9800), // Orange
                    Color(0xFFFFEB3B)  // Yellow
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE64A19), // Dark Deep Orange
                    Color(0xFFF57C00), // Dark Orange
                    Color(0xFFF9A825)  // Dark Yellow
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFEB3B)),
            secondaryColors = listOf(Color(0xFFE64A19), Color(0xFFF57C00), Color(0xFFF9A825)),
            accentColor = Color(0xFFFF7043), // Deep Orange
            surfaceColor = Color(0xFFFFF3E0), // Light Orange Surface
            textColor = Color(0xFFBF360C)    // Dark Deep Orange Text
        ),
        
        "midnight_cool" to BackgroundTheme(
            name = "Midnight Cool",
            primaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF263238), // Blue Grey
                    Color(0xFF37474F), // Light Blue Grey
                    Color(0xFF546E7A)  // Very Light Blue Grey
                ),
                tileMode = TileMode.Clamp
            ),
            secondaryGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF102027), // Dark Blue Grey
                    Color(0xFF1C313A), // Medium Blue Grey
                    Color(0xFF263238)  // Blue Grey
                ),
                tileMode = TileMode.Clamp
            ),
            primaryColors = listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF546E7A)),
            secondaryColors = listOf(Color(0xFF102027), Color(0xFF1C313A), Color(0xFF263238)),
            accentColor = Color(0xFF64B5F6), // Blue
            surfaceColor = Color(0xFFECEFF1), // Light Blue Grey Surface
            textColor = Color(0xFF102027)    // Dark Blue Grey Text
        )
    )
    
    // Get background theme by name
    fun getBackgroundTheme(themeName: String): BackgroundTheme? {
        return backgroundThemes[themeName]
    }
    
    // Get all available theme names
    fun getAvailableThemes(): List<String> {
        return backgroundThemes.keys.toList()
    }
    
    // Get screen-specific colors
    fun getScreenColors(screenType: String, themeName: String): Map<String, Color> {
        val theme = getBackgroundTheme(themeName) ?: getBackgroundTheme("hotwheels_classic")!!
        
        return when (screenType) {
            "mainlines" -> mapOf(
                "primary" to theme.primaryColors.first(),
                "secondary" to theme.secondaryColors.first(),
                "accent" to theme.accentColor
            )
            "premium" -> mapOf(
                "primary" to theme.primaryColors.first(),
                "secondary" to theme.secondaryColors.first(),
                "accent" to theme.accentColor
            )
            "collection" -> mapOf(
                "primary" to theme.primaryColors.first(),
                "secondary" to theme.secondaryColors.first(),
                "accent" to theme.accentColor
            )
            else -> mapOf(
                "primary" to theme.primaryColors.first(),
                "secondary" to theme.secondaryColors.first(),
                "accent" to theme.accentColor
            )
        }
    }
}

data class BackgroundTheme(
    val name: String,
    val primaryGradient: Brush,
    val secondaryGradient: Brush,
    val primaryColors: List<Color>,
    val secondaryColors: List<Color>,
    val accentColor: Color,
    val surfaceColor: Color,
    val textColor: Color
)
