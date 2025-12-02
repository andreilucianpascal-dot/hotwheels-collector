package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.ui.components.CarCard
import com.example.hotwheelscollectors.utils.CategoryColors
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel
import com.example.hotwheelscollectors.viewmodels.toHotWheelsCar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandCarsScreen(
    categoryId: String,
    brandName: String,
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val allCars by viewModel.localCars.collectAsState()

    val categoryTitle = remember(categoryId) {
        when (categoryId.lowercase()) {
            "rally" -> "Rally"
            "hot_roads" -> "Hot Rods"
            "convertibles", "convertible" -> "Convertibles"
            "vans" -> "Vans"
            "supercars" -> "Supercars"
            "american_muscle", "american_muscle_car" -> "American Muscle"
            "motorcycle" -> "Motorcycle"
            "suv_trucks", "suv_and_trucks", "suv_pickups", "suv & trucks" -> "SUV & Trucks"
            else -> categoryId.replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        }
    }

    val carsInBrand = remember(allCars, categoryTitle, brandName) {
        // ✅ FIXED: Match subseries that start with category (e.g., "Supercars/Ferrari" matches "Supercars")
        android.util.Log.d("BrandCarsScreen", "=== FILTERING CARS FOR BRAND ===")
        android.util.Log.d("BrandCarsScreen", "Category: $categoryTitle, Brand: $brandName")
        android.util.Log.d("BrandCarsScreen", "Total cars: ${allCars.size}")
        
        val filtered = allCars.filter {
            val seriesMatch = it.series.equals("Mainline", ignoreCase = true)
            val categoryMatch = it.subseries?.startsWith(categoryTitle, ignoreCase = true) == true || 
                               it.subseries.equals(categoryTitle, ignoreCase = true)
            val brandMatch = it.brand.equals(brandName, ignoreCase = true)
            
            android.util.Log.d("BrandCarsScreen", "Car: ${it.model}, series: ${it.series}, subseries: ${it.subseries}, brand: ${it.brand}")
            android.util.Log.d("BrandCarsScreen", "  seriesMatch: $seriesMatch, categoryMatch: $categoryMatch, brandMatch: $brandMatch")
            
            seriesMatch && categoryMatch && brandMatch
        }
        
        android.util.Log.d("BrandCarsScreen", "Filtered cars for $brandName: ${filtered.size}")
        filtered
    }

    val brandColor = CategoryColors.getBrandColor(brandName)
    val topBarTextColor = CategoryColors.getContrastingTextColor(brandColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = brandName.uppercase(),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = topBarTextColor.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brandColor,
                    titleContentColor = topBarTextColor,
                    navigationIconContentColor = topBarTextColor
                )
            )
        }
    ) { paddingValues ->
        if (carsInBrand.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No $brandName cars in $categoryTitle yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(carsInBrand, key = { it.id }) { car ->
                    CarCard(
                        car = car.toHotWheelsCar(),
                        onImageClick = { _, _ -> },
                        onMenuClick = { },
                        onCardClick = {
                            navController.navigate("car_details/${car.id}")
                        }
                    )
                }
            }
        }
    }
}

