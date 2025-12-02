package com.example.hotwheelscollectors.ui.screens.collection

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.domain.catalog.BrandCatalog
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel
import com.example.hotwheelscollectors.viewmodels.CarManagementViewModel
import java.io.File

@Composable
fun PremiumCarsScreen(
    navController: NavController,
    categoryId: String,
    subcategoryId: String?,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val localCars by viewModel.localCars.collectAsState()
    
    val categoryDisplayName = when (categoryId) {
        "car_culture" -> "Car Culture"
        "pop_culture" -> "Pop Culture"
        "boulevard" -> "Boulevard"
        "f1" -> "F1"
        "rlc" -> "RLC"
        "large_scale" -> "1:43 Scale"
        "others_premium" -> "Others Premium"
        else -> categoryId.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
    
    val subcategoryDisplayName = subcategoryId?.let {
        when (it) {
            "race_day" -> "Race Day"
            "circuit_legends" -> "Circuit Legends"
            "team_transport" -> "Team Transport"
            "jay_lenos_garage" -> "Jay Leno's Garage"
            "rtr_vehicles" -> "RTR Vehicles"
            "real_riders" -> "Real Riders"
            "fast_wagons" -> "Fast Wagons"
            "speed_machine" -> "Speed Machine"
            "japan_historics" -> "Japan Historics"
            "hammer_drop" -> "Hammer Drop"
            "slide_street" -> "Slide Street"
            "terra_trek" -> "Terra Trek"
            "exotic_envy" -> "Exotic Envy"
            "cargo_containers" -> "Cargo Containers"
            "modern_classics" -> "Modern Classics"
            "fast_and_furious" -> "Fast & Furious"
            "mario_kart" -> "Mario Kart"
            "forza_motorsport" -> "Forza Motorsport"
            "gran_turismo" -> "Gran Turismo"
            "top_gun" -> "Top Gun"
            "jurassic_world" -> "Jurassic World"
            "back_to_the_future" -> "Back to the Future"
            "looney_tunes" -> "Looney Tunes"
            else -> it.replace("_", " ").split(" ").joinToString(" ") { word -> 
                word.replaceFirstChar { char -> char.uppercaseChar() } 
            }
        }
    }
    
    // Filter Premium cars correctly
    // ✅ FIX: subseries pentru Premium este "Category/Subcategory" (ex: "Pop Culture/Back to the Future")
    val filteredCars = if (subcategoryId != null && subcategoryDisplayName != null) {
        // With subcategory: filter by isPremium AND subseries="Category/Subcategory"
        val expectedSubseries = "$categoryDisplayName/$subcategoryDisplayName"
        localCars.filter { 
            it.isPremium &&
            it.subseries.equals(expectedSubseries, ignoreCase = true)
        }
    } else {
        // Without subcategory: filter by isPremium AND subseries starts with category
        // (pentru Boulevard, F1, RLC, etc. care NU au subcategory, subseries = doar category)
        localCars.filter { 
            it.isPremium &&
            (it.subseries.equals(categoryDisplayName, ignoreCase = true) ||
             it.subseries?.startsWith("$categoryDisplayName/", ignoreCase = true) == true)
        }
    }
    
    val screenTitle = if (subcategoryDisplayName != null) {
        "$categoryDisplayName - $subcategoryDisplayName"
    } else {
        categoryDisplayName
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredCars.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No cars in this category yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCars) { car ->
                    PremiumCarCard(
                        car = car,
                        onClick = {
                            navController.navigate("car_details/${car.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovePremiumCarDialog(
    onMove: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("") }

    val categories = listOf(
        "Car Culture" to "car_culture",
        "Pop Culture" to "pop_culture",
        "Boulevard" to "boulevard",
        "F1" to "f1",
        "RLC" to "rlc",
        "1:43 Scale" to "large_scale",
        "Others Premium" to "others_premium"
    )

    val brands = remember(selectedCategory) {
        val categoryId = categories.find { it.first == selectedCategory }?.second
        categoryId?.let { id ->
            BrandCatalog.getBrandsForCategory(id).map { it.second }
        } ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Car") },
        text = {
            Column {
                Text("Select new category and brand:")
                Spacer(modifier = Modifier.height(16.dp))

                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (displayName, _) ->
                        FilterChip(
                            onClick = {
                                selectedCategory = displayName
                                selectedBrand = ""
                            },
                            label = { Text(displayName) },
                            selected = selectedCategory == displayName
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedCategory.isNotEmpty() && brands.isNotEmpty()) {
                    Text("Brand:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(brands) { brandName ->
                            FilterChip(
                                onClick = { selectedBrand = brandName },
                                label = { Text(brandName) },
                                selected = selectedBrand == brandName
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCategory.isNotEmpty()) {
                        val brandValue = if (brands.isEmpty()) "" else selectedBrand
                        onMove(selectedCategory, brandValue)
                    }
                },
                enabled = selectedCategory.isNotEmpty() && (brands.isEmpty() || selectedBrand.isNotEmpty())
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeletePremiumCarDialog(
    carName: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Car") },
        text = {
            Text("Are you sure you want to delete \"$carName\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PremiumCarCard(
    car: CarEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val carManagementViewModel: CarManagementViewModel = hiltViewModel()
    var showMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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
            // Display thumbnail
            if (car.combinedPhotoPath.isNotBlank()) {
                val thumbnailFile = File(car.combinedPhotoPath)
                if (thumbnailFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailFile)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .crossfade(true) // ✅ SMOOTH TRANSITIONS
                            .build(),
                        contentDescription = "Car Thumbnail",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            
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
                if (car.subseries.isNotBlank()) Text(
                    text = "Category: ${car.subseries}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (car.year != 0) Text(
                    text = "Year: ${car.year}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit Details") },
                onClick = {
                    showMenu = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Move") },
                onClick = {
                    showMenu = false
                    showMoveDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    val shareText = carManagementViewModel.shareCar(car.id)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Car"))
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    showDeleteDialog = true
                }
            )
        }
    }

    if (showMoveDialog) {
        MovePremiumCarDialog(
            onMove = { newCategory, newBrand ->
                carManagementViewModel.moveCar(car.id, newCategory, newBrand)
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeletePremiumCarDialog(
            carName = car.model.ifEmpty { car.series },
            onDelete = {
                carManagementViewModel.deleteCar(car.id)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

