package com.example.hotwheelscollectors.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.viewmodels.ThemeSettingsViewModel
import com.example.hotwheelscollectors.ui.components.SettingsItem

@Composable
fun ThemeSettingsScreen(
    navController: NavController,
    viewModel: ThemeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = ThemeSettingsViewModel.UiState())
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom TopAppBar using stable components
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.theme_settings),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SettingsSection(title = stringResource(R.string.theme_mode)) {
                        ThemeModeSelector(
                            currentMode = uiState.themeMode,
                            onModeSelected = viewModel::updateThemeMode
                        )
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.color_scheme)) {
                        ColorSchemeSelector(
                            currentScheme = uiState.colorScheme,
                            onSchemeSelected = viewModel::updateColorScheme
                        )
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.dynamic_color)) {
                        SettingsItem(
                            title = stringResource(R.string.use_dynamic_color),
                            subtitle = stringResource(R.string.dynamic_color_description),
                            icon = Icons.Default.Palette,
                            onClick = { },
                            trailing = {
                                Switch(
                                    checked = uiState.useDynamicColor,
                                    onCheckedChange = { viewModel.toggleDynamicColor() }
                                )
                            }
                        )
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.appearance)) {
                        SettingsItem(
                            title = stringResource(R.string.use_custom_font_size),
                            subtitle = stringResource(R.string.font_size_description),
                            icon = Icons.Default.FormatSize,
                            onClick = { },
                            trailing = {
                                Switch(
                                    checked = uiState.useCustomFontSize,
                                    onCheckedChange = { viewModel.toggleCustomFontSize() }
                                )
                            }
                        )

                        // Theme Preview
                        ThemePreviewCard(
                            currentScheme = uiState.colorScheme,
                            modifier = Modifier.padding(16.dp)
                        )

                        if (uiState.useCustomFontSize) {
                            FontSizeSlider(
                                currentSize = uiState.fontScale,
                                onSizeChanged = viewModel::updateFontScale
                            )
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    val themeModes = listOf(
        "light" to stringResource(R.string.light_theme),
        "dark" to stringResource(R.string.dark_theme),
        "system" to stringResource(R.string.system_theme)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        themeModes.forEach { (mode, displayName) ->
            ListItem(
                headlineContent = { Text(displayName) },
                supportingContent = { Text(getThemeDescription(mode)) },
                leadingContent = {
                    RadioButton(
                        selected = mode == currentMode,
                        onClick = { onModeSelected(mode) }
                    )
                },
                modifier = Modifier.clickable { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ColorSchemeSelector(
    currentScheme: String,
    onSchemeSelected: (String) -> Unit
) {
    val colorSchemes = listOf(
        "default" to stringResource(R.string.default_scheme),
        "hotwheels_classic" to "Hot Wheels Classic",
        "hotwheels_premium" to "Hot Wheels Premium",
        "hotwheels_racing" to "Hot Wheels Racing",
        "hotwheels_vintage" to "Hot Wheels Vintage",
        "blue_ocean" to "Blue Ocean",
        "green_forest" to "Green Forest",
        "purple_royal" to "Purple Royal",
        "sunset_warm" to "Sunset Warm",
        "midnight_cool" to "Midnight Cool"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        colorSchemes.forEach { (scheme, displayName) ->
            ListItem(
                headlineContent = { Text(displayName) },
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        getSchemeColors(scheme).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                },
                trailingContent = {
                    RadioButton(
                        selected = scheme == currentScheme,
                        onClick = { onSchemeSelected(scheme) }
                    )
                },
                modifier = Modifier.clickable { onSchemeSelected(scheme) }
            )
        }
    }
}

@Composable
private fun FontSizeSlider(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.small),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.large),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Slider(
            value = currentSize,
            onValueChange = onSizeChanged,
            valueRange = 0.8f..1.2f,
            steps = 4
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

private fun getThemeDescription(mode: String): String {
    return when (mode) {
        "light" -> "Use light theme"
        "dark" -> "Use dark theme"
        "system" -> "Follow system theme"
        else -> "Unknown theme"
    }
}

private fun getSchemeColors(scheme: String): List<Color> {
    return when (scheme) {
        "default" -> listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFFF6B6B))
        "hotwheels_classic" -> listOf(Color(0xFFFF6B00), Color(0xFF1976D2), Color(0xFFE91E63))
        "hotwheels_premium" -> listOf(Color(0xFFFFD700), Color(0xFF9C27B0), Color(0xFF607D8B))
        "hotwheels_racing" -> listOf(Color(0xFFF44336), Color(0xFF000000), Color(0xFFFFEB3B))
        "hotwheels_vintage" -> listOf(Color(0xFF795548), Color(0xFF3E2723), Color(0xFFD84315))
        "blue_ocean" -> listOf(Color(0xFF1976D2), Color(0xFF00695C), Color(0xFF64B5F6))
        "green_forest" -> listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF66BB6A))
        "purple_royal" -> listOf(Color(0xFF9C27B0), Color(0xFF4A148C), Color(0xFFBA68C8))
        "sunset_warm" -> listOf(Color(0xFFFF5722), Color(0xFFE64A19), Color(0xFFFF7043))
        "midnight_cool" -> listOf(Color(0xFF263238), Color(0xFF102027), Color(0xFF64B5F6))
        else -> listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFFF6B6B))
    }
}

@Composable
private fun ThemePreviewCard(
    currentScheme: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Theme Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Preview of different screen colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScreenColorPreview("Mainlines", "mainlines", currentScheme)
                ScreenColorPreview("Premium", "premium", currentScheme)
                ScreenColorPreview("Collection", "collection", currentScheme)
            }
        }
    }
}

@Composable
private fun ScreenColorPreview(
    screenName: String,
    screenType: String,
    currentScheme: String
) {
    val screenColors = com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager.getScreenColors(screenType, currentScheme)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = screenName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = screenColors["primary"] ?: MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = screenColors["secondary"] ?: MaterialTheme.colorScheme.secondary,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}