// app/src/main/java/com/example/hotwheelscollectors/ui/theme/Theme.kt

package com.example.hotwheelscollectors.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager.getBackgroundTheme

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun HotWheelsCollectorsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorSchemeName: String = "default",
    fontScale: Float = 1.0f,
    customPrimaryColor: Int? = null,
    customSecondaryColor: Int? = null,
    customTertiaryColor: Int? = null,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        // Dynamic Material You colors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Custom global scheme using 3 user-defined colors
        !dynamicColor && colorSchemeName == "custom" -> {
            val base = if (darkTheme) DarkColors else LightColors
            val primary = customPrimaryColor?.takeIf { it != 0 }?.let { Color(it) } ?: base.primary
            val secondary = customSecondaryColor?.takeIf { it != 0 }?.let { Color(it) } ?: base.secondary
            val background = customTertiaryColor?.takeIf { it != 0 }?.let { Color(it) } ?: base.background

            base.copy(
                primary = primary,
                secondary = secondary,
                background = background,
                surface = background,
            )
        }
        // Themed colors from HotWheelsThemeManager
        !dynamicColor && colorSchemeName != "default" -> {
            val hotTheme = getBackgroundTheme(colorSchemeName)
            if (hotTheme != null) {
                val base = if (darkTheme) DarkColors else LightColors
                base.copy(
                    primary = hotTheme.primaryColors.firstOrNull() ?: base.primary,
                    secondary = hotTheme.secondaryColors.firstOrNull() ?: base.secondary,
                    background = hotTheme.surfaceColor,
                    surface = hotTheme.surfaceColor,
                    onPrimary = hotTheme.textColor,
                    onBackground = hotTheme.textColor,
                    onSurface = hotTheme.textColor,
                )
            } else {
                if (darkTheme) DarkColors else LightColors
            }
        }
        // Default Material colors (current behavior)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val scaledTypography = if (fontScale != 1.0f) {
        scaleTypography(Typography, fontScale.coerceIn(0.8f, 1.2f))
    } else {
        Typography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}

private fun scaleTypography(base: Typography, scale: Float): Typography {
    if (scale == 1.0f) return base

    fun TextStyle.scaled(): TextStyle =
        copy(
            fontSize = fontSize * scale,
            lineHeight = lineHeight * scale,
            // păstrăm letterSpacing, nu îl scalăm ca să nu stricăm lizibilitatea
        )

    return Typography(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}