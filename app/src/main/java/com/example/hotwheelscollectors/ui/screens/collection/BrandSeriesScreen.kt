package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.example.hotwheelscollectors.ui.components.CarCard
import com.example.hotwheelscollectors.ui.components.CategoryBrandDiscovery
import com.example.hotwheelscollectors.ui.components.EmptyState
import com.example.hotwheelscollectors.ui.components.LoadingSpinner
import com.example.hotwheelscollectors.viewmodels.BrandSeriesViewModel
import com.example.hotwheelscollectors.viewmodels.toHotWheelsCar

private fun isPremiumSeries(seriesId: String): Boolean {
    val premiumSeries = listOf(
        "car_culture",
        "pop_culture",
        "boulevard",
        "f1",
        "rlc",
        "1:43_scale",
        "others_premium"
    )
    return premiumSeries.contains(seriesId.lowercase())
}

@Composable
fun BrandSeriesScreen(
    navController: NavController,
    brandId: String,
    seriesId: String? = null,
    viewModel: BrandSeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Reactive data source: filter localCars by brand/series instead of calling loadCars
    val localCars by viewModel.localCars.collectAsStateWithLifecycle(initialValue = emptyList())
    val carsForScreen = remember(localCars, brandId, seriesId) {
        localCars.filter { car ->
            val brandOk = brandId.isNotEmpty() && car.brand.equals(brandId, ignoreCase = true)
            val seriesOk = seriesId?.let { car.series.equals(it, ignoreCase = true) } ?: true
            brandOk && seriesOk
        }.map { it.toHotWheelsCar() }
    }
    
    val title = when {
        seriesId != null && seriesId.isNotEmpty() -> {
            viewModel.getSeriesName(seriesId) ?: "Series"
        }
        else -> {
            // For mainline categories, show the proper category name
            when (brandId) {
                "rally" -> "Rally"
                "hot_roads" -> "Hot Rods"
                "supercars" -> "Supercars"
                "american_muscle" -> "American Muscle"
                "convertibles" -> "Convertibles"
                "vans" -> "Vans"
                "motorcycle" -> "Motorcycles"
                "suv_pickups" -> "SUV & Pickups"
                // Legacy mappings
                "suv_trucks" -> "SUV & Pickups"
                "convertible" -> "Convertibles"
                else -> {
                    // If it's a brand, try to get brand name
                    if (brandId.isNotEmpty()) {
                        viewModel.getBrandName(brandId)
                            ?: brandId.replaceFirstChar { it.uppercase() }
                    } else {
                        "Collection"
                    }
                }
            }
        }
    }

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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when {
            uiState.isLoading -> {
                LoadingSpinner(
                    modifier = Modifier.fillMaxSize()
                )
            }
            carsForScreen.isEmpty() -> {
                EmptyState(
                    message = "No cars in this collection yet.",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(carsForScreen) { car ->
                        CarCard(
                            car = car,
                            onImageClick = { _, _ -> },
                            onMenuClick = { 
                                navController.navigate("edit_car/${car.id}")
                            },
                            onCardClick = { navController.navigate("car_details/${car.id}") }
                        )
                    }
                }
            }
        }
    }
}
