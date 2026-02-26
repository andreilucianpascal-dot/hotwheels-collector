package com.example.hotwheelscollectors.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

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

                // Custom scheme editor: only visible when "custom" is selected
                if (uiState.colorScheme == "custom") {
                    item {
                        SettingsSection(title = "Custom color scheme") {
                            Text(
                                text = "Pick three colors used for the global theme:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            CustomSchemeColorRow(
                                label = "Primary color (buttons, accents)",
                                currentColorInt = uiState.customSchemeColor1,
                                defaultColor = MaterialTheme.colorScheme.primary,
                                onColorSelected = { color ->
                                    viewModel.updateCustomSchemeColor(1, color?.toArgb() ?: 0)
                                }
                            )

                            CustomSchemeColorRow(
                                label = "Secondary color (highlights)",
                                currentColorInt = uiState.customSchemeColor2,
                                defaultColor = MaterialTheme.colorScheme.secondary,
                                onColorSelected = { color ->
                                    viewModel.updateCustomSchemeColor(2, color?.toArgb() ?: 0)
                                }
                            )

                            CustomSchemeColorRow(
                                label = "Background color",
                                currentColorInt = uiState.customSchemeColor3,
                                defaultColor = MaterialTheme.colorScheme.background,
                                onColorSelected = { color ->
                                    viewModel.updateCustomSchemeColor(3, color?.toArgb() ?: 0)
                                }
                            )
                        }
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.dynamic_color)) {
                        SettingsItem(
                            title = stringResource(R.string.use_dynamic_color),
                            subtitle = stringResource(R.string.dynamic_color_description),
                            icon = Icons.Default.Palette,
                            onClick = { viewModel.setUseDynamicColor(!uiState.useDynamicColor) },
                            trailing = {
                                Switch(
                                    checked = uiState.useDynamicColor,
                                    onCheckedChange = { checked -> viewModel.setUseDynamicColor(checked) }
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
                            onClick = { viewModel.setCustomFontSizeEnabled(!uiState.useCustomFontSize) },
                            trailing = {
                                Switch(
                                    checked = uiState.useCustomFontSize,
                                    onCheckedChange = { checked -> viewModel.setCustomFontSizeEnabled(checked) }
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset to defaults button
                        OutlinedButton(
                            onClick = {
                                // Reset theme to default values
                                viewModel.updateThemeMode("system")
                                viewModel.updateColorScheme("default")
                                viewModel.setUseDynamicColor(true)
                                viewModel.updateFontScale(1.0f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Reset to default theme")
                        }
                    }
                }

                item {
                    SettingsSection(title = "Main screen buttons") {
                        MainScreenButtonsSection(
                            uiState = uiState,
                            onColorSelected = { category, color ->
                                viewModel.updateMainButtonColor(category, color?.toArgb() ?: 0)
                            },
                            onFontSelected = { fontKey ->
                                viewModel.updateMainScreenFontFamily(fontKey)
                            }
                        )
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
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        "midnight_cool" to "Midnight Cool",
        "custom" to "Custom"
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
                        getSchemeColors(scheme, currentScheme).forEach { color ->
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
                        onClick = { onSchemeSelected(scheme) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
    // Local state for immediate UI feedback
    var sliderValue by remember(currentSize) { mutableStateOf(currentSize) }
    
    // Sync with external state when it changes
    LaunchedEffect(currentSize) {
        sliderValue = currentSize
    }
    
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
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue // Update local state immediately
                onSizeChanged(newValue) // Trigger ViewModel update
            },
            valueRange = 0.7f..1.4f,
            steps = 7
        )
    }
}

@Composable
private fun MainScreenButtonsSection(
    uiState: ThemeSettingsViewModel.UiState,
    onColorSelected: (category: String, color: Color?) -> Unit,
    onFontSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Customize category buttons on main screen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ButtonColorRow(
            label = "Mainline",
            currentColorInt = uiState.mainlineButtonColor,
            defaultColor = Color(0xFF87CEEB),
            onColorSelected = { onColorSelected("mainline", it) }
        )

        ButtonColorRow(
            label = "Premium",
            currentColorInt = uiState.premiumButtonColor,
            defaultColor = Color.Black,
            onColorSelected = { onColorSelected("premium", it) }
        )

        ButtonColorRow(
            label = "Silver Series",
            currentColorInt = uiState.silverButtonColor,
            defaultColor = Color(0xFFC0C0C0),
            onColorSelected = { onColorSelected("silver", it) }
        )

        ButtonColorRow(
            label = "Treasure Hunt",
            currentColorInt = uiState.treasureHuntButtonColor,
            defaultColor = Color.White,
            onColorSelected = { onColorSelected("treasure_hunt", it) }
        )

        ButtonColorRow(
            label = "Super Treasure Hunt",
            currentColorInt = uiState.superTreasureHuntButtonColor,
            defaultColor = Color.White,
            onColorSelected = { onColorSelected("super_treasure_hunt", it) }
        )

        ButtonColorRow(
            label = "Others",
            currentColorInt = uiState.othersButtonColor,
            defaultColor = Color(0xFF4CAF50),
            onColorSelected = { onColorSelected("others", it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        MainScreenFontSelector(
            currentFont = uiState.mainScreenFontFamily,
            onFontSelected = onFontSelected
        )
    }
}

@Composable
private fun ButtonColorRow(
    label: String,
    currentColorInt: Int,
    defaultColor: Color,
    onColorSelected: (Color?) -> Unit
) {
    val currentColor = if (currentColorInt != 0) Color(currentColorInt) else defaultColor
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onColorSelected(null) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text("Original")
            }
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text("Change")
            }
        }
    }

    if (showDialog) {
        ColorPickerDialog(
            initialColor = currentColor,
            onConfirm = { color ->
                showDialog = false
                onColorSelected(color)
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color?) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableStateOf((initialColor.red * 255).roundToInt()) }
    var green by remember { mutableStateOf((initialColor.green * 255).roundToInt()) }
    var blue by remember { mutableStateOf((initialColor.blue * 255).roundToInt()) }

    val previewColor = Color(
        red = red.coerceIn(0, 255),
        green = green.coerceIn(0, 255),
        blue = blue.coerceIn(0, 255)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(previewColor) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Pick color") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(previewColor)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Quick preset colors for easier selection
                Text("Quick colors")
                Spacer(modifier = Modifier.height(4.dp))

                val quickColors = listOf(
                    Color(0xFF87CEEB), // Light blue (Mainline default)
                    Color.Black,       // Black (Premium default)
                    Color(0xFFC0C0C0), // Silver (Silver Series default)
                    Color(0xFFFFD700), // Gold (TH/STH accent)
                    Color(0xFFE60012), // Hot Wheels red
                    Color(0xFF1976D2), // Hot Wheels blue
                    Color(0xFF4CAF50), // Green
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    quickColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 1.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    red = (color.red * 255).roundToInt()
                                    green = (color.green * 255).roundToInt()
                                    blue = (color.blue * 255).roundToInt()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Red: $red")
                Slider(
                    value = red.toFloat(),
                    onValueChange = { red = it.roundToInt() },
                    valueRange = 0f..255f
                )
                Text("Green: $green")
                Slider(
                    value = green.toFloat(),
                    onValueChange = { green = it.roundToInt() },
                    valueRange = 0f..255f
                )
                Text("Blue: $blue")
                Slider(
                    value = blue.toFloat(),
                    onValueChange = { blue = it.roundToInt() },
                    valueRange = 0f..255f
                )
            }
        }
    )
}

@Composable
private fun CustomSchemeColorRow(
    label: String,
    currentColorInt: Int,
    defaultColor: Color,
    onColorSelected: (Color?) -> Unit
) {
    val currentColor = if (currentColorInt != 0) Color(currentColorInt) else defaultColor
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onColorSelected(null) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text("Default")
            }
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text("Change")
            }
        }
    }

    if (showDialog) {
        ColorPickerDialog(
            initialColor = currentColor,
            onConfirm = { color ->
                showDialog = false
                onColorSelected(color)
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenFontSelector(
    currentFont: String,
    onFontSelected: (String) -> Unit
) {
    val fonts = listOf(
        "default" to "Default (Material)",
        "sans" to "System Sans Serif",
        "serif" to "System Serif",
        "mono" to "System Monospace",
        "clayborn" to "Clayborn (Hot Wheels style)",
        "lobster" to "Lobster (Bold script)",
        "greatvibes" to "Great Vibes (Elegant script)",
        "permanentmarker" to "Permanent Marker (Marker)",
        "racingsansone" to "Racing Sans One",
        "specialspeed" to "Special Speed Agent",
        "motor" to "Motor (Racing)",
        "retrofunk" to "Retrofunk Script"
    )

    fun fontFamilyForKey(key: String): androidx.compose.ui.text.font.FontFamily {
        return when (key) {
            "sans" -> androidx.compose.ui.text.font.FontFamily.SansSerif
            "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
            "mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
            "clayborn" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.clayborn))
            "lobster" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.lobster))
            "greatvibes" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.greatvibes_regular))
            "permanentmarker" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.permanentmarker))
            "racingsansone" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.racingsansone_regular))
            "specialspeed" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.special_speed_agent))
            "motor" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.motor_personal_use_only))
            "retrofunk" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.retrofunk_script_personal_use))
            else -> androidx.compose.ui.text.font.FontFamily.Default
        }
    }

    val selectedFont = fonts.firstOrNull { it.first == currentFont } ?: fonts.first()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Main screen font",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedFont.second,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fonts.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamilyForKey(key))
                            )
                        },
                        onClick = {
                            onFontSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
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

private fun getSchemeColors(scheme: String, currentScheme: String): List<Color> {
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
        "custom" -> {
            // For custom scheme row, show placeholders; actual chosen colors are edited below
            if (currentScheme == "custom") {
                // Colors will be previewed in CustomSchemeSection
                listOf(Color(0xFFBBBBBB), Color(0xFF888888), Color(0xFF555555))
            } else {
                listOf(Color(0xFFBBBBBB), Color(0xFF888888), Color(0xFF555555))
            }
        }
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