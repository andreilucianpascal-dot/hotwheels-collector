package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.ui.components.CarCard
import com.example.hotwheelscollectors.viewmodels.toHotWheelsCar
import com.example.hotwheelscollectors.utils.CategoryColors
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainlineBrandsScreen(
    categoryId: String,
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val allCars by viewModel.localCars.collectAsState()

    val categoryTitle = remember(categoryId) {
        // Map known IDs to display titles used when saving cars
        when (categoryId.lowercase()) {
            "rally" -> "Rally"
            "hot_roads" -> "Hot Roads"
            "convertibles", "convertible" -> "Convertibles" // ✅ FIXED: Match exact save format
            "vans" -> "Vans"
            "supercars" -> "Supercars"
            "american_muscle", "american_muscle_car" -> "American Muscle" // ✅ FIXED: Remove "Car"
            "motorcycle" -> "Motorcycle"
            "suv_trucks", "suv_and_trucks", "suv_pickups", "suv & trucks" -> "SUV & Trucks" // ✅ FIXED: Match exact format
            else -> categoryId.replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        }
    }

    val carsInCategory = remember(allCars, categoryTitle) {
        // Filter by subseries (category) instead of series for Mainline cars
        allCars.filter { 
            it.series.equals("Mainline", ignoreCase = true) && 
            it.subseries.equals(categoryTitle, ignoreCase = true) 
        }
    }

    val carsGroupedByBrand = remember(carsInCategory) {
        carsInCategory.groupBy { it.brand }
    }

    // Define available brands for each category (even if no cars exist) - SORTED ALPHABETICALLY
    val availableBrands = remember(categoryId) {
        when (categoryId.lowercase()) {
            "rally" -> listOf("Audi", "BMW", "Citroen", "Datsun", "Ford", "Lancia", "Mazda", "Mitsubishi", "Nissan", "Opel", "Peugeot", "Subaru", "Toyota", "Volkswagen", "Volvo").sorted()
            "supercars" -> listOf("Aston Martin", "Automobili Pininfarina", "Bentley", "Bugatti", "Corvette", "Ferrari", "Ford GT", "Koenigsegg", "Lamborghini", "Lucid Air", "Maserati", "Mazda 787B", "McLaren", "Pagani", "Porsche", "Rimac").sorted()
            "american_muscle" -> listOf("Barracuda", "Buick", "Cadillac", "Camaro", "Challenger", "Charger", "Chevelle", "Chevy", "Chevrolet", "Chrysler", "Corvette", "Cougar", "Dodge", "El Camino", "Firebird", "Ford", "GTO", "Impala", "Lincoln", "Mercury", "Mustang", "Nova", "Oldsmobile", "Plymouth", "Pontiac", "Super Bee", "Thunderbird").sorted()
            "vans" -> listOf("Chevrolet", "Chrysler", "Dodge", "Ford", "Honda", "Mercedes", "Nissan", "Toyota", "Volkswagen").sorted()
            "convertibles" -> listOf("Abarth", "Acura", "Alfa Romeo", "Aston Martin", "Audi", "Bentley", "BMW", "Bugatti", "Cadillac", "Chevrolet", "Chrysler", "Citroen", "Corvette", "Daihatsu", "Datsun", "Dodge", "Ferrari", "Fiat", "Ford", "Honda", "Infiniti", "Jaguar", "Koenigsegg", "Lamborghini", "Land Rover", "Lancia", "Lexus", "Lincoln", "Lotus", "Maserati", "Mazda", "McLaren", "Mercedes", "Mercury", "Mini", "Mitsubishi", "Nissan", "Oldsmobile", "Opel", "Pagani", "Peugeot", "Plymouth", "Pontiac", "Porsche", "Renault", "Subaru", "Suzuki", "Toyota", "Volkswagen", "Volvo").sorted()
            "suv_trucks", "suv_and_trucks", "suv_pickups", "suv & trucks" -> listOf("Audi", "BMW", "Chevrolet", "Dodge", "Ford", "GMC", "Honda", "Hummer", "Jeep", "Land Rover", "Mercedes", "Nissan", "Porsche", "Ram", "Toyota", "Volkswagen").sorted()
            "motorcycle" -> listOf("BMW", "Ducati", "Harley Davidson", "Honda", "Indian", "Kawasaki", "Suzuki", "Triumph", "Yamaha").sorted()
            "hot_roads" -> emptyList() // Hot Roads has no specific brands
            else -> emptyList()
        }
    }

    // --- COLOR LOGIC START ---
    val categoryColor = CategoryColors.getSeriesColor(categoryId)
    val topBarTextColor = CategoryColors.getContrastingTextColor(categoryColor)
    // --- COLOR LOGIC END ---

    Scaffold(
        // Use a subtle version of the category color for the background
        containerColor = categoryColor.copy(alpha = 0.1f),
        topBar = {
            TopAppBar(
                title = { Text(text = categoryTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                // Apply the full category color to the TopAppBar
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = categoryColor,
                    titleContentColor = topBarTextColor,
                    navigationIconContentColor = topBarTextColor
                )
            )
        }
    ) { paddingValues ->
        // ✅ FIXED: Always show brands, even if category is empty
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            availableBrands.forEach { brand ->
                val carsInBrand = carsGroupedByBrand[brand] ?: emptyList()

                // --- COLOR LOGIC START ---
                val brandColor = CategoryColors.getBrandColor(brand)
                // --- COLOR LOGIC END ---

                item {
                    BrandFolderHeader(
                        brandName = brand,
                        carCount = carsInBrand.size,
                        backgroundColor = brandColor,
                        onClick = {
                            // Navigate to brand cars screen
                            navController.navigate("brand_cars/$categoryId/$brand")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandFolderHeader(
    brandName: String,
    carCount: Int = 0,
    backgroundColor: Color,
    onClick: () -> Unit = {}
) {
    // Determine if text should be light or dark based on the background
    val textColor = CategoryColors.getContrastingTextColor(backgroundColor)

    Surface(
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "Brand folder",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = brandName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            Text(
                text = "$carCount cars",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No cars in this category yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
