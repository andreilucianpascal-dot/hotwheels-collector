package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.hotwheelscollectors.R
import android.net.Uri
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel
import com.example.hotwheelscollectors.model.HotWheelsCar
import java.io.File
import com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager
import com.example.hotwheelscollectors.viewmodels.AppThemeViewModel

// Collection categories similar to mainlines
data class CollectionCategory(
    val id: String,
    val title: String,
    val backgroundColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val carCount: Int = 0,
)

@Composable
fun CollectionScreen(
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showTotalPaidDialog by remember { mutableStateOf(false) }

    // Refresh user data on screen load
    LaunchedEffect(Unit) {
        try {
            viewModel.refreshUserData()
        } catch (e: Exception) {
            android.util.Log.e("CollectionScreen", "Error refreshing user data: ${e.message}", e)
        }
    }

    // Car count from database
    val localCars by viewModel.localCars.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalCarsCount = localCars.size
    val defaultCurrency = remember {
        runCatching { java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode }.getOrDefault("EUR")
    }
    val totalsByCurrency = remember(localCars, defaultCurrency) {
        localCars
            .asSequence()
            .filter { it.purchasePrice > 0.0 }
            .groupBy { it.purchaseCurrency.ifBlank { defaultCurrency } }
            .mapValues { (_, cars) -> cars.sumOf { it.purchasePrice } }
            .toSortedMap()
    }
    val totalPaidSummaryText = remember(totalsByCurrency) {
        if (totalsByCurrency.isEmpty()) {
            "Total paid: —"
        } else {
            val parts = totalsByCurrency.entries.map { (currency, sum) ->
                "${String.format(java.util.Locale.getDefault(), "%.2f", sum)} $currency"
            }
            "Total paid: " + parts.joinToString(" • ")
        }
    }
    val tabCarCounts = remember(localCars) {
        try {
            android.util.Log.d("CollectionScreen", "=== DEBUGGING TAB COUNTS ===")
            android.util.Log.d("CollectionScreen", "Total cars: ${localCars.size}")
            localCars.forEach { car ->
                android.util.Log.d("CollectionScreen", "Car: ${car.model}, series: '${car.series}', isPremium: ${car.isPremium}, isTH: ${car.isTH}, isSTH: ${car.isSTH}")
            }
            
            val mainlineCount = localCars.count { it.series == "Mainline" && !it.isTH && !it.isSTH }
            val premiumCount = localCars.count { it.isPremium } // ✅ Fixed: Use isPremium field instead of series
            val silverSeriesCount = localCars.count { it.series.equals("Silver Series", ignoreCase = true) }
            val thCount = localCars.count { it.isTH }
            val sthCount = localCars.count { it.isSTH }
            val othersCount = localCars.count { it.series == "Others" }
            
            android.util.Log.d("CollectionScreen", "Mainline count: $mainlineCount")
            android.util.Log.d("CollectionScreen", "Premium count: $premiumCount")
            android.util.Log.d("CollectionScreen", "Silver Series count: $silverSeriesCount")
            android.util.Log.d("CollectionScreen", "TH count: $thCount")
            android.util.Log.d("CollectionScreen", "STH count: $sthCount")
            android.util.Log.d("CollectionScreen", "Others count: $othersCount")
            
            listOf(mainlineCount, premiumCount, silverSeriesCount, thCount, sthCount, othersCount)
        } catch (e: Exception) {
            android.util.Log.e("CollectionScreen", "Error calculating tab counts: ${e.message}", e)
            listOf(0, 0, 0, 0, 0, 0) // Return safe default values
        }
    }
    
    // Safety check to ensure we have at least 6 elements
    val safeTabCarCounts = if (tabCarCounts.size >= 6) {
        tabCarCounts
    } else {
        listOf(0, 0, 0, 0, 0, 0) // Default values
    }
    val categories = remember(safeTabCarCounts) {
        listOf(
            CollectionCategory(
                id = "mainline",
                title = "Mainline",
                backgroundColor = Color(0xFF87CEEB), // Light blue
                textColor = Color.White,
                icon = Icons.Default.DirectionsCar,
                carCount = safeTabCarCounts[0]
            ),
            CollectionCategory(
                id = "premium",
                title = "Premium",
                backgroundColor = Color.Black,
                textColor = Color(0xFFFFD700), // Gold
                icon = Icons.Default.Star,
                carCount = safeTabCarCounts[1]
            ),
            CollectionCategory(
                id = "silver_series",
                title = "SilverSeries",
                backgroundColor = Color(0xFFC0C0C0), // Silver
                textColor = Color.Black,
                icon = Icons.Default.Star,
                carCount = safeTabCarCounts[2]
            ),
            CollectionCategory(
                id = "treasure_hunt",
                title = "Treasure Hunt",
                backgroundColor = Color.White,
                textColor = Color.Gray,
                icon = Icons.Default.Diamond,
                carCount = safeTabCarCounts[3]
            ),
            CollectionCategory(
                id = "super_treasure_hunt",
                title = "Super Treasure Hunt",
                backgroundColor = Color.White,
                textColor = Color(0xFFFFD700),
                icon = Icons.Default.Star,
                carCount = safeTabCarCounts[4]
            ),
            CollectionCategory(
                id = "others",
                title = "Others",
                backgroundColor = Color(0xFF4CAF50),
                textColor = Color.White,
                icon = Icons.Default.Category,
                carCount = safeTabCarCounts[5]
            )
        )
    }

    var showDebugDialog by remember { mutableStateOf(false) }

    // Themed background using current color scheme
    val themeViewModel: AppThemeViewModel = hiltViewModel()
    val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val bgTheme = try {
        HotWheelsThemeManager.getBackgroundTheme(themeState.colorScheme)
    } catch (e: Exception) {
        android.util.Log.e("CollectionScreen", "Error getting background theme: ${e.message}", e)
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = bgTheme?.secondaryGradient
                    ?: androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
            )
    ) {
        // Debug Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ALL CARS (DEBUG) Button
            OutlinedButton(
                onClick = { showDebugDialog = true },
                modifier = Modifier
                    .height(40.dp)
                    .background(Color.Blue),
                enabled = true
            ) {
                Text(
                    "ALL CARS (DEBUG)",
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            
            // DATABASE CLEANUP Button
            OutlinedButton(
                onClick = { navController.navigate("database_cleanup") },
                modifier = Modifier
                    .height(40.dp)
                    .background(Color.Red),
                enabled = true
            ) {
                Text(
                    "DATABASE CLEANUP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Top App Bar with integrated search
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2196F3),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    placeholder = { Text("Search your collection...", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = Color.White
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color.White
                    )
                )
            }
        }

        // Category Tabs - Custom buttons with individual colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { /* display-only */ },
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Total cars: $totalCarsCount")
            }

            FilledTonalButton(
                onClick = { showTotalPaidDialog = true },
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(totalPaidSummaryText, maxLines = 1)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.size.coerceAtMost(6)) { index ->
                if (index >= categories.size) return@items
                val category = categories[index]
                val isSelected = selectedTab == index
                
                Button(
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) category.backgroundColor else category.backgroundColor.copy(alpha = 0.5f),
                        contentColor = category.textColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${category.title}(${category.carCount})",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Filter cars by search query
        val filteredLocalCars = remember(localCars, searchQuery) {
            if (searchQuery.isBlank()) {
                localCars
            } else {
                val query = searchQuery.lowercase().trim()
                localCars.filter { car ->
                    car.model.lowercase().contains(query) ||
                    car.brand.lowercase().contains(query) ||
                    car.series.lowercase().contains(query) ||
                    car.subseries.lowercase().contains(query) ||
                    car.color.lowercase().contains(query) ||
                    car.year.toString().contains(query) ||
                    car.barcode.lowercase().contains(query)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ✅ FIX: When search query exists, show ALL filtered results regardless of selected tab
            if (searchQuery.isNotBlank()) {
                // Show all search results in a unified view
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredLocalCars.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No results found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try a different search term",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Found ${filteredLocalCars.size} result(s) for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(filteredLocalCars) { car ->
                            CarCard(
                                car = car,
                                navController = navController,
                                onClick = {
                                    navController.navigate("car_details/${car.id}")
                                }
                            )
                        }
                    }
                }
            } else {
                // Content based on selected tab (only when no search query)
                when (selectedTab) {
                    0 -> {
                        val mainlineCars = filteredLocalCars.filter { it.series == "Mainline" && !it.isTH && !it.isSTH }
                        android.util.Log.d("CollectionScreen", "=== MAINLINE TAB CONTENT ===")
                        android.util.Log.d("CollectionScreen", "Mainline filtered cars: ${mainlineCars.size}")
                        mainlineCars.forEach { car ->
                            android.util.Log.d("CollectionScreen", "Mainline car: ${car.model}, series: '${car.series}', isTH: ${car.isTH}, isSTH: ${car.isSTH}")
                        }
                        MainlineCollectionContent(navController, mainlineCars)
                    }

                    1 -> PremiumCategoriesScreen(navController, filteredLocalCars.filter { it.isPremium }) // ✅ Fixed: Use isPremium field

                    2 -> {
                        val silverSeriesCars = filteredLocalCars.filter { it.series.equals("Silver Series", ignoreCase = true) }
                        android.util.Log.d("CollectionScreen", "=== SILVER SERIES TAB CONTENT ===")
                        android.util.Log.d("CollectionScreen", "Silver Series filtered cars: ${silverSeriesCars.size}")
                        silverSeriesCars.forEach { car ->
                            android.util.Log.d("CollectionScreen", "Silver Series car: ${car.model}, series: '${car.series}'")
                        }
                        SilverSeriesScreen(navController)
                    }

                    3 -> TreasureHuntCollectionContent(navController, filteredLocalCars.filter { it.isTH })
                    4 -> SuperTreasureHuntCollectionContent(navController, filteredLocalCars.filter { it.isSTH })
                    5 -> {
                        val othersCars = filteredLocalCars.filter { it.series == "Others" }
                        android.util.Log.d("CollectionScreen", "=== OTHERS TAB CONTENT ===")
                        android.util.Log.d("CollectionScreen", "Others filtered cars: ${othersCars.size}")
                        othersCars.forEach { car ->
                            android.util.Log.d("CollectionScreen", "Others car: ${car.model}, series: '${car.series}'")
                        }
                        OthersCollectionContent(navController, othersCars)
                    }
                }
            }

        }

        if (showTotalPaidDialog) {
            AlertDialog(
                onDismissRequest = { showTotalPaidDialog = false },
                title = { Text("Total price paid") },
                text = {
                    if (totalsByCurrency.isEmpty()) {
                        Text("No purchase prices set yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Cars: $totalCarsCount")
                            totalsByCurrency.forEach { (currency, sum) ->
                                Text("${String.format(java.util.Locale.getDefault(), "%.2f", sum)} $currency")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTotalPaidDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("All Cars (Debug)") },
            text = {
                val cars = viewModel.localCars.collectAsState().value
                LazyColumn {
                    items(cars) { car ->
                        Text(
                            text = "id: ${car.id}\nuserId: ${car.userId}\nmodel: ${car.model}\nbrand: ${car.brand}\ntimestamp: ${car.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebugDialog = false }) { Text("Close") }
            }
        )
    }
    } // ✅ Close main Column
}

@Composable
private fun MainlineCollectionContent(
    navController: NavController,
    cars: List<CarEntity>,
) {
    // Debug logging
    android.util.Log.d("CollectionScreen", "MainlineCollectionContent - cars count: ${cars.size}")
    cars.forEach { car ->
        android.util.Log.d("CollectionScreen", "Car: ${car.model}, brand: ${car.brand}, series: ${car.series}, subseries: '${car.subseries}', isPremium: ${car.isPremium}, isTH: ${car.isTH}, isSTH: ${car.isSTH}")
        android.util.Log.d("CollectionScreen", "  - frontPhotoPath: '${car.frontPhotoPath}'")
        android.util.Log.d("CollectionScreen", "  - combinedPhotoPath: '${car.combinedPhotoPath}'")
        android.util.Log.d("CollectionScreen", "  - barcode: '${car.barcode}'")
    }
    
    // Group cars by category (extract main category from subseries like "Supercars/Ferrari" -> "Supercars")
    val carsByCategory = cars.groupBy { car ->
        // Extract main category before "/"
        val subseries = car.subseries ?: ""
        if (subseries.contains("/")) {
            subseries.substringBefore("/")
        } else {
            subseries
        }
    }
    
    // Debug category grouping
    android.util.Log.d("CollectionScreen", "Cars grouped by category:")
    carsByCategory.forEach { (category, categoryCars) ->
        android.util.Log.d("CollectionScreen", "  Category '$category': ${categoryCars.size} cars")
        categoryCars.forEach { car ->
            android.util.Log.d("CollectionScreen", "    - ${car.brand} ${car.model}")
        }
    }
    
    // Define categories with their visual styling and original fonts
    val categoryStyles = mapOf(
        "Rally" to CategoryStyle(Color.Black, Color.Red, "Rally", FontFamily(Font(R.font.racingsansone_regular))),
        "Hot Rods" to CategoryStyle(Color(0xFFFF9800), Color.Black, "Hot Rods", FontFamily(Font(R.font.lobster))),
        "Convertibles" to CategoryStyle(Color.White, Color.Red, "Convertibles", FontFamily(Font(R.font.greatvibes_regular))),
        "Vans" to CategoryStyle(Color.Blue, Color.White, "Vans", FontFamily(Font(R.font.permanentmarker))),
        "Supercars" to CategoryStyle(Color.White, Color.Black, "Supercars", FontFamily(Font(R.font.special_speed_agent))),
        "American Muscle" to CategoryStyle(Color(0xFFD2691E), Color(0xFFFFFDD0), "American Muscle", FontFamily(Font(R.font.retrofunk_script_personal_use))),
        "Motorcycle" to CategoryStyle(Color(0xFFFF9800), Color.Red, "Motorcycle", FontFamily(Font(R.font.motor_personal_use_only))),
        "SUV & Trucks" to CategoryStyle(Color(0xFF8B4513), Color.Black, "SUV & Trucks", FontFamily(Font(R.font.clayborn)))
    )
    
    // ✅ FIXED: Always show categories, even when no cars exist
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show ALL categories with their cars (even empty ones)
        categoryStyles.forEach { (category, style) ->
            val categoryCars = carsByCategory[category] ?: emptyList()
            
            item {
                // Category Header with styling - CLICKABLE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                        .height(80.dp)
                    .clickable {
                            // Navigate to category brands screen
                            android.util.Log.d("CollectionScreen", "Category clicked: $category")
                            // ✅ FIXED: Handle SUV & Trucks navigation correctly
                            val categoryId = when (category) {
                                "SUV & Trucks" -> "suv_trucks"
                                else -> category.lowercase().replace(" ", "_").replace("&", "and")
                            }
                            navController.navigate("mainline_brands/$categoryId")
                        },
                    colors = CardDefaults.cardColors(containerColor = style.backgroundColor),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        Text(
                                text = style.displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = style.fontFamily,
                                color = style.textColor,
                                textAlign = TextAlign.Center
                        )
                        Text(
                                text = "${categoryCars.size} cars",
                                fontSize = 14.sp,
                                color = style.textColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            // REMOVED: Brand listing under categories for clean design
            // Cars will only be visible inside the specific brand screens
        }
    }
}

// Data class for category styling
private data class CategoryStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val displayName: String,
    val fontFamily: FontFamily
)


@Composable
private fun TreasureHuntCollectionContent(
    navController: NavController,
    cars: List<CarEntity>,
) {
    if (cars.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Text(
                text = "No Treasure Hunt cars in your collection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cars) { car ->
                CarCard(
                    car = car,
                    navController = navController,
                    onClick = { 
                        // Navigate to car details
                        navController.navigate("car_details/${car.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun SuperTreasureHuntCollectionContent(
    navController: NavController,
    cars: List<CarEntity>,
) {
    if (cars.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Text(
                text = "No Super Treasure Hunt cars in your collection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cars) { car ->
                CarCard(
                    car = car,
                    navController = navController,
                    onClick = { 
                        // Navigate to car details
                        navController.navigate("car_details/${car.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun OthersCollectionContent(
    navController: NavController,
    cars: List<CarEntity>,
) {
    if (cars.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Text(
                text = "No Other cars in your collection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cars) { car ->
                CarCard(
                    car = car,
                    navController = navController,
                    onClick = { 
                        // Navigate to car details
                        navController.navigate("car_details/${car.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun MainlineCategoryCard(
    category: CollectionCategory,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = category.backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = category.textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = category.textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${category.carCount} cars",
                fontSize = 12.sp,
                color = category.textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CarListItem(car: CarEntity, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* navController.navigate to details/edit car screen with car.id */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = car.model, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Brand: ${car.brand.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall
            )
            if (car.series.isNotBlank()) Text(
                text = "Series: ${car.series}",
                style = MaterialTheme.typography.bodySmall
            )
            if (car.year != null) Text(
                text = "Year: ${car.year}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Added: ${
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(car.timestamp))
                }", style = MaterialTheme.typography.bodySmall, color = Color.Gray
            )
        }
    }
}

@Composable
private fun CarCard(
    car: CarEntity,
    navController: NavController,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Load actual car photo from database
            CarPhoto(
                car = car,
                modifier = Modifier
                    .size(180.dp)
                    .clickable {
                        // ✅ FIX: Correct parameter order: carId first, then photoUri
                        val photoUri = car.frontPhotoPath.ifEmpty { car.combinedPhotoPath }
                        val encodedUri = java.net.URLEncoder.encode(photoUri, "UTF-8")
                        navController.navigate("full_photo_view/${car.id}/$encodedUri")
                    }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = car.model.ifEmpty { "Unknown Model" }, 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Brand: ${car.brand.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (car.series.isNotBlank()) Text(
                    text = "Series: ${car.series}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (car.year != 0) Text(
                    text = "Year: ${car.year}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (car.color.isNotBlank()) Text(
                    text = "Color: ${car.color}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Added: ${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(car.timestamp))
                    }", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CarPhoto(
    car: CarEntity,
    modifier: Modifier = Modifier
) {
    // Use CarEntity photo paths directly (like PremiumCarsScreen does)
    val photoPath = car.combinedPhotoPath.ifEmpty { car.frontPhotoPath }
    
    // 🔍 DEBUG: Log photo paths to understand what's happening
    LaunchedEffect(car.id) {
        Log.d("CollectionScreen", "=== CAR PHOTO DEBUG ===")
        Log.d("CollectionScreen", "Car ID: ${car.id}")
        Log.d("CollectionScreen", "Car Model: ${car.model}")
        Log.d("CollectionScreen", "Combined Path: ${car.combinedPhotoPath}")
        Log.d("CollectionScreen", "Front Path: ${car.frontPhotoPath}")
        Log.d("CollectionScreen", "Final Path: $photoPath")
        Log.d("CollectionScreen", "Path exists: ${File(photoPath).exists()}")
        Log.d("CollectionScreen", "Path is blank: ${photoPath.isBlank()}")
    }
    
    if (photoPath.isNotBlank() && File(photoPath).exists()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoPath)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                .crossfade(true)
                .build(),
            contentDescription = "Car photo",
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_gallery)
        )
    } else {
        PhotoPlaceholder(modifier = modifier)
    }
}

@Composable
private fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.DirectionsCar,
            contentDescription = "Car photo",
            modifier = Modifier.size(40.dp),
            tint = Color.Gray
        )
    }
}