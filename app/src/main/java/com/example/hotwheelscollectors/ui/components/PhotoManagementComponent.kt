package com.example.hotwheelscollectors.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.viewmodels.CarManagementViewModel
import java.io.File

@Composable
fun PhotoManagementComponent(
    photoId: String,
    photoPath: String,
    onImageClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val carManagementViewModel: CarManagementViewModel = hiltViewModel()

    Box {
        // Photo display (you can customize this based on your photo display needs)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onImageClick() }
        ) {
            // Your photo display implementation here
        }

        // Menu button
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Photo options"
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
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
                    val sharePath = carManagementViewModel.sharePhoto(photoId)
                    if (sharePath != null) {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, sharePath)
                            type = "image/*"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                    }
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    // Show delete confirmation
                    showExportDialog = true
                    showMenu = false
                }
            )
        }
    }

    // Copy Photo Dialog
    if (showCopyDialog) {
        CopyPhotoDialog(
            currentPath = photoPath,
            onCopy = { destinationPath ->
                val success = carManagementViewModel.copyPhoto(photoId, destinationPath)
                if (success) {
                    // Show success message
                }
                showCopyDialog = false
            },
            onDismiss = { showCopyDialog = false }
        )
    }

    // Move Photo Dialog
    if (showMoveDialog) {
        MoveCarDialog(
            onMove = { category, brand ->
                carManagementViewModel.moveCar(photoId, category, brand)
                // Show success message
                showMoveDialog = false
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    // Export Photo Dialog
    if (showExportDialog) {
        ExportPhotoDialog(
            photoName = File(photoPath).name,
            onExport = { exportPath ->
                val success = carManagementViewModel.exportPhoto(photoId, exportPath)
                if (success) {
                    // Show success message
                }
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun CopyPhotoDialog(
    currentPath: String,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var destinationPath by remember { mutableStateOf("/sdcard/Download/${File(currentPath).name}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Photo") },
        text = {
            Column {
                Text("Copy photo to new location:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = destinationPath,
                    onValueChange = { destinationPath = it },
                    label = { Text("Destination Path") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCopy(destinationPath) }
            ) {
                Text("Copy")
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
private fun MoveCarDialog(
    onMove: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("") }
    var showBrandSelection by remember { mutableStateOf(false) }

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
                                showBrandSelection = true
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
private fun ExportPhotoDialog(
    photoName: String,
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var exportPath by remember { mutableStateOf("/sdcard/Download/$photoName") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Photo") },
        text = {
            Column {
                Text("Export photo to new location:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = exportPath,
                    onValueChange = { exportPath = it },
                    label = { Text("Export Path") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(exportPath) }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
