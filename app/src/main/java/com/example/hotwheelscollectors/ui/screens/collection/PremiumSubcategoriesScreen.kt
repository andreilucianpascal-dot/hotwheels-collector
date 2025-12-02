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
        it.series == "Premium" && it.subseries.startsWith(categoryDisplayName)
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = subcategory,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${subcategoryCars.size} cars",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

