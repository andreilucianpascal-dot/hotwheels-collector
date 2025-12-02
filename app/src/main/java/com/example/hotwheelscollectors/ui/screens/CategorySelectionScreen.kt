package com.example.hotwheelscollectors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hotwheelscollectors.utils.CategoryType
import com.example.hotwheelscollectors.utils.SaveLocation
import com.example.hotwheelscollectors.utils.CategorySuggestion

@Composable
fun CategorySelectionScreen(
    suggestions: List<CategorySuggestion>,
    onCategorySelected: (SaveLocation) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<CategoryType?>(null) }
    var selectedSeries by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Where should we save this car?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We couldn't automatically categorize this car. Please select the correct location:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedCategory == null) {
            // Show main categories
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(suggestions) { suggestion ->
                    CategoryCard(
                        suggestion = suggestion,
                        onClick = { selectedCategory = suggestion.category }
                    )
                }
            }
        } else {
            // Show subcategories for selected category
            SubcategorySelectionView(
                category = selectedCategory!!,
                suggestions = suggestions,
                onSubcategorySelected = { series ->
                    selectedSeries = series
                    onCategorySelected(
                        SaveLocation(
                            category = selectedCategory!!,
                            series = series,
                            brand = null,
                            requiresUserSelection = false,
                            confidence = 1.0f // User selection is 100% confident
                        )
                    )
                },
                onBack = { selectedCategory = null }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    suggestion: CategorySuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when (suggestion.category) {
                CategoryType.MAINLINE -> MaterialTheme.colorScheme.primaryContainer
                CategoryType.PREMIUM -> MaterialTheme.colorScheme.secondaryContainer
                CategoryType.OTHERS -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getCategoryIcon(suggestion.category),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when (suggestion.category) {
                        CategoryType.MAINLINE -> MaterialTheme.colorScheme.primary
                        CategoryType.PREMIUM -> MaterialTheme.colorScheme.secondary
                        CategoryType.OTHERS -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getCategoryDisplayName(suggestion.category),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Confidence indicator
                ConfidenceIndicator(confidence = suggestion.confidence)
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show subcategories preview
            if (suggestion.subcategories.isNotEmpty()) {
                Text(
                    text = "Includes: ${suggestion.subcategories.take(3).joinToString(", ")}${if (suggestion.subcategories.size > 3) "..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubcategorySelectionView(
    category: CategoryType,
    suggestions: List<CategorySuggestion>,
    onSubcategorySelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subcategories = suggestions.find { it.category == category }?.subcategories ?: emptyList()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header for subcategory selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to categories")
            }
            
            Text(
                text = "Select ${getCategoryDisplayName(category)} Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(subcategories) { subcategory ->
                SubcategoryItem(
                    subcategory = subcategory,
                    onClick = { onSubcategorySelected(subcategory) }
                )
            }
            
            // Add "Other" option
            item {
                SubcategoryItem(
                    subcategory = "other",
                    displayName = "Other / Not Sure",
                    onClick = { onSubcategorySelected("other") }
                )
            }
        }
    }
}

@Composable
private fun SubcategoryItem(
    subcategory: String,
    displayName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getSubcategoryIcon(subcategory),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = displayName ?: getSubcategoryDisplayName(subcategory),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        confidence >= 0.8f -> Color(0xFF4CAF50) // Green
        confidence >= 0.6f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
    
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, RoundedCornerShape(6.dp))
            .border(1.dp, Color.White, RoundedCornerShape(6.dp))
    )
}

private fun getCategoryIcon(category: CategoryType): ImageVector {
    return when (category) {
        CategoryType.MAINLINE -> Icons.Default.DirectionsCar
        CategoryType.PREMIUM -> Icons.Default.Star
        CategoryType.OTHERS -> Icons.Default.LocalShipping
        CategoryType.HOT_ROADS -> Icons.Default.Whatshot
        CategoryType.UNKNOWN -> Icons.Default.Help
    }
}

private fun getCategoryDisplayName(category: CategoryType): String {
    return when (category) {
        CategoryType.MAINLINE -> "Mainline"
        CategoryType.PREMIUM -> "Premium Series"
        CategoryType.OTHERS -> "Others"
        CategoryType.HOT_ROADS -> "Hot Rods"
        CategoryType.UNKNOWN -> "Unknown"
    }
}

private fun getSubcategoryIcon(subcategory: String): ImageVector {
    return when (subcategory) {
        "rally" -> Icons.Default.Terrain
        "supercars" -> Icons.Default.Speed
        "american_muscle" -> Icons.Default.Flag
        "suv_trucks" -> Icons.Default.LocalShipping
        "vans" -> Icons.Default.Commute
        "motorcycle" -> Icons.Default.TwoWheeler
        "convertible" -> Icons.Default.BeachAccess
        "hw_exotics" -> Icons.Default.Diamond
        "team_transport" -> Icons.Default.LocalShipping
        "car_culture" -> Icons.Default.Palette
        "fast_furious" -> Icons.Default.MovieFilter
        "trucks" -> Icons.Default.LocalShipping
        "buses" -> Icons.Default.DirectionsBus
        "motorcycles" -> Icons.Default.TwoWheeler
        "planes" -> Icons.Default.Flight
        "other" -> Icons.Default.MoreHoriz
        else -> Icons.Default.DirectionsCar
    }
}

private fun getSubcategoryDisplayName(subcategory: String): String {
    return when (subcategory) {
        "rally" -> "Rally Cars"
        "supercars" -> "Supercars"
        "american_muscle" -> "American Muscle"
        "suv_trucks" -> "SUV & Trucks"
        "vans" -> "Vans"
        "motorcycle" -> "Motorcycles"
        "convertible" -> "Convertibles"
        "hw_exotics" -> "HW Exotics"
        "team_transport" -> "Team Transport"
        "car_culture" -> "Car Culture"
        "fast_furious" -> "Fast & Furious"
        "boulevard" -> "Boulevard"
        "art_cars" -> "Art Cars"
        "trucks" -> "Trucks"
        "buses" -> "Buses"
        "motorcycles" -> "Motorcycles"
        "planes" -> "Planes"
        "boats" -> "Boats"
        "other" -> "Other"
        else -> subcategory.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }
}

// Preview composables for development
@Composable
private fun CategorySelectionPreview() {
    val mockSuggestions = listOf(
        CategorySuggestion(
            category = CategoryType.MAINLINE,
            subcategories = listOf("rally", "supercars", "american_muscle"),
            confidence = 0.8f,
            reason = "Most Hot Wheels cars are mainline"
        ),
        CategorySuggestion(
            category = CategoryType.PREMIUM,
            subcategories = listOf("hw_exotics", "team_transport"),
            confidence = 0.6f,
            reason = "May be a premium series"
        ),
        CategorySuggestion(
            category = CategoryType.OTHERS,
            subcategories = listOf("trucks", "buses"),
            confidence = 0.4f,
            reason = "For non-car vehicles"
        )
    )

    CategorySelectionScreen(
        suggestions = mockSuggestions,
        onCategorySelected = { },
        onBack = { }
    )
}