package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Edit

@Composable
fun ColorSelectionDialog(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf("Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Black", "White", "Silver", "Gold")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column {
                colors.forEach { colorName ->
                    TextButton(
                        onClick = { onColorSelected(colorName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(colorName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun YearSelectionDialog(
    selectedYear: Int?,
    minYear: Int,
    maxYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = (minYear..maxYear).toList().reversed()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year") },
        text = {
            Column {
                years.forEach { yearValue ->
                    TextButton(
                        onClick = { onYearSelected(yearValue) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(yearValue.toString())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManufacturerSelectionDialog(
    selectedManufacturer: String,
    onManufacturerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val manufacturers = listOf("Ford", "Chevrolet", "Toyota", "Honda", "BMW", "Mercedes", "Audi", "Volkswagen", "Porsche", "Ferrari", "Lamborghini", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Manufacturer") },
        text = {
            Column {
                manufacturers.forEach { manufacturerName ->
                    TextButton(
                        onClick = { onManufacturerSelected(manufacturerName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(manufacturerName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ScaleSelectionDialog(
    selectedScale: String,
    onScaleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scales = listOf("1:64", "1:43", "1:32", "1:24", "1:18", "1:12", "1:8", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Scale") },
        text = {
            Column {
                scales.forEach { scaleValue ->
                    TextButton(
                        onClick = { onScaleSelected(scaleValue) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(scaleValue)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PhotoOptionsDialog(
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column {
                TextButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Camera, "Camera")
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo")
                }
                TextButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery")
                    Spacer(Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BarcodeScanDialog(
    onScanBarcode: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barcode Scanner") },
        text = {
            Column {
                Text("Choose how to add the car:")
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onScanBarcode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Scan")
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Barcode")
                }
                TextButton(
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, "Manual")
                    Spacer(Modifier.width(8.dp))
                    Text("Enter Manually")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}