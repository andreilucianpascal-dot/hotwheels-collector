package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel
import com.example.hotwheelscollectors.model.HotWheelsCar
import java.io.File

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

    // Refresh user data on screen load
    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
    }

    // Car count from database
    val localCars by viewModel.localCars.collectAsState()
    val tabCarCounts = remember(localCars) {
        android.util.Log.d("CollectionScreen", "=== DEBUGGING TAB COUNTS ===")
        android.util.Log.d("CollectionScreen", "Total cars: ${localCars.size}")
        localCars.forEach { car ->
            android.util.Log.d("CollectionScreen", "Car: ${car.model}, series: '${car.series}', isPremium: ${car.isPremium}, isTH: ${car.isTH}, isSTH: ${car.isSTH}")
        }
        
        val mainlineCount = localCars.count { it.series == "Mainline" && !it.isTH && !it.isSTH }
        val premiumCount = localCars.count { it.isPremium } // ✅ Fixed: Use isPremium field instead of series
        val thCount = localCars.count { it.isTH }
        val sthCount = localCars.count { it.isSTH }
        val othersCount = localCars.count { it.series == "Others" }
        
        android.util.Log.d("CollectionScreen", "Mainline count: $mainlineCount")
        android.util.Log.d("CollectionScreen", "Premium count: $premiumCount")
        android.util.Log.d("CollectionScreen", "TH count: $thCount")
        android.util.Log.d("CollectionScreen", "STH count: $sthCount")
        android.util.Log.d("CollectionScreen", "Others count: $othersCount")
        
        listOf(mainlineCount, premiumCount, thCount, sthCount, othersCount)
    }
    
    // Safety check to ensure we have at least 5 elements
    val safeTabCarCounts = if (tabCarCounts.size >= 5) {
        tabCarCounts
    } else {
        listOf(0, 0, 0, 0, 0) // Default values
    }
    val categories = remember(safeTabCarCounts) {
        listOf(
            CollectionCategory(
                id = "mainline",
                title = "Mainline",
                backgroundColor = Color.White,
                textColor = Color(0xFF87CEEB),
                icon = Icons.Default.DirectionsCar,
                carCount = safeTabCarCounts[0]
            ),
            CollectionCategory(
                id = "premium",
                title = "Premium",
                backgroundColor = Color.Black,
                textColor = Color(0xFFFFD700),
                icon = Icons.Default.Star,
                carCount = safeTabCarCounts[1]
            ),
            CollectionCategory(
                id = "treasure_hunt",
                title = "Treasure Hunt",
                backgroundColor = Color.White,
                textColor = Color.Gray,
                icon = Icons.Default.Diamond,
                carCount = safeTabCarCounts[2]
            ),
            CollectionCategory(
                id = "super_treasure_hunt",
                title = "Super Treasure Hunt",
                backgroundColor = Color.White,
                textColor = Color(0xFFFFD700),
                icon = Icons.Default.Star,
                carCount = safeTabCarCounts[3]
            ),
            CollectionCategory(
                id = "others",
                title = "Others",
                backgroundColor = Color(0xFF4CAF50),
                textColor = Color.White,
                icon = Icons.Default.Category,
                carCount = safeTabCarCounts[4]
            )
        )
    }

    var showDebugDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
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

        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2196F3),
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
                        tint = Color.White
                    )
                }
                Text(
                    text = "My Collection (${localCars.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Search functionality */ }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search your collection...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        // Category Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text("${category.title} (${category.carCount})")
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                val mainlineCars = localCars.filter { it.series == "Mainline" && !it.isTH && !it.isSTH }
                android.util.Log.d("CollectionScreen", "=== MAINLINE TAB CONTENT ===")
                android.util.Log.d("CollectionScreen", "Mainline filtered cars: ${mainlineCars.size}")
                mainlineCars.forEach { car ->
                    android.util.Log.d("CollectionScreen", "Mainline car: ${car.model}, series: '${car.series}', isTH: ${car.isTH}, isSTH: ${car.isSTH}")
                }
                MainlineCollectionContent(navController, mainlineCars)
            }
            1 -> PremiumCategoriesScreen(navController, localCars.filter { it.isPremium }) // ✅ Fixed: Use isPremium field
            2 -> TreasureHuntCollectionContent(navController, localCars.filter { it.isTH })
            3 -> SuperTreasureHuntCollectionContent(navController, localCars.filter { it.isSTH })
            4 -> {
                val othersCars = localCars.filter { it.series == "Others" }
                android.util.Log.d("CollectionScreen", "=== OTHERS TAB CONTENT ===")
                android.util.Log.d("CollectionScreen", "Others filtered cars: ${othersCars.size}")
                othersCars.forEach { car ->
                    android.util.Log.d("CollectionScreen", "Others car: ${car.model}, series: '${car.series}'")
                }
                OthersCollectionContent(navController, othersCars)
            }
        }
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
                modifier = Modifier.size(80.dp)
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