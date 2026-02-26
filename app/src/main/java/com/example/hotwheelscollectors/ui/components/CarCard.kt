package com.example.hotwheelscollectors.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.example.hotwheelscollectors.viewmodels.CarManagementViewModel
import java.io.File

@Composable
fun CarCard(
    car: HotWheelsCar,
    onImageClick: (String, String) -> Unit,
    onMenuClick: () -> Unit,
    onCardClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    navController: NavController? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val carManagementViewModel: CarManagementViewModel = hiltViewModel()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .let { modifier ->
                if (onLongClick != null) {
                    modifier.combinedClickable(
                        onClick = { onCardClick() },
                        onLongClick = { onLongClick() }
                    )
                } else {
                    modifier
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ CORRECT: Display ONLY thumbnail in list, click opens full size photo
            android.util.Log.d("CarCard", "=== CAR CARD DEBUG ===")
            android.util.Log.d("CarCard", "Car: ${car.brand} ${car.model}")
            android.util.Log.d("CarCard", "Car series: ${car.series}")
            android.util.Log.d("CarCard", "Car subseries: ${car.subseries}")
            android.util.Log.d("CarCard", "Displaying thumbnail: ${car.combinedPhotoPath}")
            
            if (car.combinedPhotoPath.isNotBlank()) {
                // ✅ FIX: Support both local file paths and URLs (Firebase Storage, Google Drive)
                // If combinedPhotoPath is a URL (starts with http), use it directly
                // Otherwise, check if local file exists
                val isUrl = car.combinedPhotoPath.startsWith("http://") || car.combinedPhotoPath.startsWith("https://")
                val thumbnailData = if (isUrl) {
                    // ✅ FIX: Convert Google Drive web URLs to direct download URLs
                    // Handle both web view URLs (drive.google.com/file/d/...) and direct download URLs (uc?export=download&id=...)
                    val url = when {
                        // Already a direct download URL - use as is
                        car.combinedPhotoPath.contains("uc?export=download&id=") -> {
                            car.combinedPhotoPath
                        }
                        // Web view URL - convert to direct download
                        car.combinedPhotoPath.contains("drive.google.com/file/d/") -> {
                            // Extract file ID from web URL: https://drive.google.com/file/d/FILE_ID/view...
                            val fileIdMatch = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)").find(car.combinedPhotoPath)
                            if (fileIdMatch != null) {
                                val fileId = fileIdMatch.groupValues[1]
                                "https://drive.google.com/uc?export=download&id=$fileId"
                            } else {
                                car.combinedPhotoPath
                            }
                        }
                        // Other URL (Firebase, etc.) - use as is
                        else -> {
                            car.combinedPhotoPath
                        }
                    }
                    android.util.Log.d("CarCard", "Using thumbnail as URL: $url")
                    url // Use URL directly
                } else {
                    val thumbnailFile = File(car.combinedPhotoPath)
                    android.util.Log.d("CarCard", "Thumbnail file path: ${car.combinedPhotoPath}")
                    android.util.Log.d("CarCard", "Thumbnail file exists: ${thumbnailFile.exists()}")
                    if (thumbnailFile.exists()) {
                        android.util.Log.d("CarCard", "Thumbnail file size: ${thumbnailFile.length()} bytes")
                        thumbnailFile // Use local file
                    } else {
                        android.util.Log.w("CarCard", "Thumbnail file doesn't exist: ${car.combinedPhotoPath}")
                        null // File doesn't exist
                    }
                }
                
                if (thumbnailData != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailData)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .crossfade(true) // ✅ SMOOTH TRANSITIONS
                            .build(),
                        contentDescription = "Car Thumbnail",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { 
                                if (navController != null) {
                                    // Navigate to full photo view screen
                                    val photoUri = car.frontPhotoPath.ifEmpty { car.combinedPhotoPath }
                                    val encodedUri = java.net.URLEncoder.encode(photoUri, "UTF-8")
                                    navController.navigate("full_photo_view/${car.id}/$encodedUri")
                                } else {
                                    // Fallback to old behavior
                                    onImageClick("FullSize", car.frontPhotoPath)
                                }
                            }
                    )
                } else {
                    android.util.Log.w("CarCard", "No valid thumbnail data available (not URL, file doesn't exist)")
                }
            } else {
                android.util.Log.w("CarCard", "No thumbnail path available")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (car.isPremium) {
                        // For Premium: Show category and model (brand is empty)
                        "${car.series} - ${car.model.ifEmpty { "Edit Model" }}"
                    } else {
                        // For Mainline: Show brand and model
                        "${car.brand} ${car.model}"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Year: ${car.year}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Barcode: ${car.barcode}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Series: ${if (car.isPremium) "Premium" else "Mainline"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Menu button
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Details") },
                    onClick = {
                        onMenuClick()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move") },
                    onClick = {
                        showMoveDialog = true
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        val shareText = carManagementViewModel.shareCar(car.id)
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
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
                        showDeleteDialog = true
                        showMenu = false
                    }
                )
            }
        }
    }

    // Move Car Dialog
    if (showMoveDialog) {
        MoveCarDialog(
            onMove = { newCategory, newBrand ->
                carManagementViewModel.moveCar(car.id, newCategory, newBrand)
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    // Delete Car Dialog
    if (showDeleteDialog) {
        DeleteCarDialog(
            carName = "${car.brand} ${car.model}",
            onDelete = {
                carManagementViewModel.deleteCar(car.id)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun MoveCarDialog(
    onMove: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("") }

    val categories = listOf(
        "Rally", "Supercars", "American Muscle", "Vans", 
        "Convertibles", "SUV & Trucks", "Motorcycle", "Hot Rods"
    )

    val brands = remember(selectedCategory) {
        when (selectedCategory) {
            "Rally" -> listOf("Subaru", "Mitsubishi", "Lancia", "Peugeot", "Citroen", "Toyota", "Ford", "Audi", "Volkswagen", "Mazda", "BMW", "Volvo", "Datsun", "Opel", "Nissan")
            "Supercars" -> listOf("Ferrari", "Lamborghini", "Maserati", "Pagani", "Bugatti", "McLaren", "Koenigsegg", "Aston Martin", "Rimac", "Lucid Air", "Ford GT", "Mazda 787B", "Automobili Pininfarina", "Bentley", "Porsche", "Corvette")
            "American Muscle" -> listOf("Ford", "Chevrolet", "Dodge", "Chrysler", "Pontiac", "Buick", "Cadillac", "Oldsmobile", "Plymouth", "Lincoln", "Mercury", "Camaro", "Chevy", "Corvette", "Chevelle", "El Camino", "Impala", "Nova", "Challenger", "Charger", "Super Bee", "Mustang", "Thunderbird", "Cougar", "Barracuda", "Firebird", "GTO")
            "Vans" -> listOf("Ford", "Chevrolet", "Dodge", "Chrysler", "Toyota", "Honda", "Nissan", "Volkswagen", "Mercedes")
            "Convertibles" -> listOf("Ford", "Chevrolet", "Dodge", "Chrysler", "Pontiac", "Buick", "Cadillac", "Oldsmobile", "Plymouth", "Lincoln", "Mercury", "Toyota", "Honda", "Nissan", "Mazda", "Subaru", "Mitsubishi", "Suzuki", "Daihatsu", "Lexus", "Infiniti", "Acura", "Datsun", "BMW", "Mercedes", "Audi", "Volkswagen", "Porsche", "Opel", "Ferrari", "Lamborghini", "Maserati", "Pagani", "Bugatti", "Fiat", "Alfa Romeo", "Lancia", "Abarth", "Peugeot", "Renault", "Citroen", "Jaguar", "Land Rover", "Mini", "Bentley", "Aston Martin", "Lotus", "McLaren", "Volvo", "Koenigsegg", "Corvette")
            "SUV & Trucks" -> listOf("Hummer", "Jeep", "Ram", "GMC", "Land Rover", "Toyota", "Honda", "Nissan", "Ford", "Chevrolet", "Dodge", "BMW", "Mercedes", "Audi", "Volkswagen", "Porsche")
            "Motorcycle" -> listOf("Honda", "Yamaha", "Kawasaki", "Suzuki", "BMW", "Ducati", "Harley Davidson", "Indian", "Triumph")
            "Hot Rods" -> emptyList()
            else -> emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Car") },
        text = {
            Column {
                Text("Select new category and brand:")
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category Selection
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            onClick = { 
                                selectedCategory = category
                                selectedBrand = ""
                            },
                            label = { Text(category) },
                            selected = selectedCategory == category
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Brand Selection
                if (selectedCategory.isNotEmpty()) {
                    Text("Brand:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(brands) { brand ->
                            FilterChip(
                                onClick = { selectedBrand = brand },
                                label = { Text(brand) },
                                selected = selectedBrand == brand
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (selectedCategory.isNotEmpty() && selectedBrand.isNotEmpty()) {
                        onMove(selectedCategory, selectedBrand)
                    }
                },
                enabled = selectedCategory.isNotEmpty() && selectedBrand.isNotEmpty()
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
private fun DeleteCarDialog(
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