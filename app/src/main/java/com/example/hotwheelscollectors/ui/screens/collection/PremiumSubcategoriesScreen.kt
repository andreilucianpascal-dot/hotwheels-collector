package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel

@Composable
fun PremiumSubcategoriesScreen(
    navController: NavController,
    categoryId: String,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val localCars by viewModel.localCars.collectAsState()
    
    val categoryDisplayName = when (categoryId) {
        "car_culture" -> "Car Culture"
        "pop_culture" -> "Pop Culture"
        else -> categoryId.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
    
    // Filter Premium cars that belong to this main category
    val carsInCategory = localCars.filter { 
        it.isPremium && it.subseries.startsWith(categoryDisplayName)
    }
    
    val subcategories = when (categoryId) {
        "car_culture" -> listOf(
            "Modern Classics",
            "Race Day",
            "Circuit Legends",
            "Team Transport",
            "Silhouettes",
            "Jay Leno's Garage",
            "RTR Vehicles",
            "Real Riders",
            "Fast Wagons",
            "Speed Machine",
            "Japan Historics",
            "Hammer Drop",
            "Slide Street",
            "Terra Trek",
            "Exotic Envy",
            "Cargo Containers"
        )
        "pop_culture" -> listOf(
            "Fast & Furious",
            "Mario Kart",
            "Forza Motorsport",
            "Gran Turismo",
            "Top Gun",
            "Batman",
            "Star Wars",
            "Marvel",
            "Jurassic World",
            "Back to the Future",
            "Looney Tunes"
        )
        else -> emptyList()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$categoryDisplayName - Subcategories") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            subcategories.forEach { subcategory ->
                val (bgColor, textColor) = subcategoryColors(categoryId, subcategory)
                // Count cars that have this specific subcategory in their subseries
                val subcategoryCars = carsInCategory.filter { 
                    it.subseries.contains(subcategory, ignoreCase = true)
                }
                
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val subcategoryId = subcategory.lowercase().replace(" ", "_").replace("&", "and").replace("'", "")
                                navController.navigate("premium_cars/$categoryId/$subcategoryId")
                            },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = textColor
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = subcategory,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Text(
                                    text = "${subcategoryCars.size} cars",
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun subcategoryColors(categoryId: String, subcategory: String): Pair<Color, Color> {
    return when (categoryId) {
        "car_culture" -> when (subcategory) {
            "Modern Classics" -> Color(0xFF3F51B5) to Color.White  // Indigo vivid
            "Race Day" -> Color(0xFFFFC107) to Color(0xFF212121)  // Amber vivid + dark text
            "Circuit Legends" -> Color(0xFF4CAF50) to Color.White  // Green vivid
            "Team Transport" -> Color(0xFFFF5722) to Color.White  // Deep orange
            "Silhouettes" -> Color(0xFF9C27B0) to Color.White  // Purple vivid
            "Jay Leno's Garage" -> Color(0xFF546E7A) to Color.White  // Blue grey
            "RTR Vehicles" -> Color(0xFF00BCD4) to Color(0xFF004D40)  // Cyan vivid
            "Real Riders" -> Color(0xFFE91E63) to Color.White  // Pink vivid
            "Fast Wagons" -> Color(0xFF8BC34A) to Color(0xFF1B5E20)  // Light green vivid
            "Speed Machine" -> Color(0xFFFF9800) to Color(0xFF212121)  // Orange vivid + dark text
            "Japan Historics" -> Color(0xFF673AB7) to Color.White  // Deep purple
            "Hammer Drop" -> Color(0xFF7B1FA2) to Color.White  // Purple
            "Slide Street" -> Color(0xFFFF6F00) to Color.White  // Amber deep
            "Terra Trek" -> Color(0xFF00ACC1) to Color(0xFF004D40)  // Cyan
            "Exotic Envy" -> Color(0xFFCDDC39) to Color(0xFF33691E)  // Lime vivid
            "Cargo Containers" -> Color(0xFF26A69A) to Color.White  // Teal vivid
            else -> defaultSubcategoryColors()
        }
        "pop_culture" -> when (subcategory) {
            "Fast & Furious" -> Color(0xFFD32F2F) to Color.White  // Red vivid
            "Mario Kart" -> Color(0xFFFFEB3B) to Color(0xFF212121)  // Yellow vivid + dark text
            "Forza Motorsport" -> Color(0xFF009688) to Color.White  // Teal vivid
            "Gran Turismo" -> Color(0xFF2196F3) to Color.White  // Blue vivid
            "Top Gun" -> Color(0xFF4CAF50) to Color.White  // Green vivid
            "Batman" -> Color(0xFF424242) to Color.White  // Dark grey
            "Star Wars" -> Color(0xFFFF9800) to Color(0xFF212121)  // Orange vivid + dark text
            "Marvel" -> Color(0xFFE53935) to Color.White  // Red vivid
            "Jurassic World" -> Color(0xFF66BB6A) to Color(0xFF1B5E20)  // Green
            "Back to the Future" -> Color(0xFFAB47BC) to Color.White  // Purple
            "Looney Tunes" -> Color(0xFFFF7043) to Color.White  // Deep orange
            else -> defaultSubcategoryColors()
        }
        else -> defaultSubcategoryColors()
    }
}private fun defaultSubcategoryColors(): Pair<Color, Color> =
    Color(0xFFE0E0E0) to Color(0xFF424242)