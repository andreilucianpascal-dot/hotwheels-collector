package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hotwheelscollectors.model.CarFilterState
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.example.hotwheelscollectors.model.SortState

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun FilterBottomSheet(
    filterState: CarFilterState,
    onFilterStateChange: (CarFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Cars") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year Range Filter
                Text(
                    text = "Year Range",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = filterState.minYear?.toString() ?: "",
                        onValueChange = { value ->
                            val year = value.toIntOrNull()
                            onFilterStateChange(filterState.copy(minYear = year))
                        },
                        label = { Text("Min Year") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedTextField(
                        value = filterState.maxYear?.toString() ?: "",
                        onValueChange = { value ->
                            val year = value.toIntOrNull()
                            onFilterStateChange(filterState.copy(maxYear = year))
                        },
                        label = { Text("Max Year") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Series Filter
                OutlinedTextField(
                    value = filterState.series ?: "",
                    onValueChange = { value ->
                        onFilterStateChange(filterState.copy(series = value.ifEmpty { null }))
                    },
                    label = { Text("Series") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Filter
                OutlinedTextField(
                    value = filterState.color ?: "",
                    onValueChange = { value ->
                        onFilterStateChange(filterState.copy(color = value.ifEmpty { null }))
                    },
                    label = { Text("Color") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onFilterStateChange(CarFilterState())
                    onDismiss()
                }
            ) {
                Text("Clear Filters")
            }
        }
    )
}

@Composable
fun SortBottomSheet(
    sortState: SortState,
    onSortStateChange: (SortState) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Cars") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sort Field Selection
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val sortFields = listOf("Name", "Year", "Number")
                sortFields.forEach { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortStateChange(sortState.copy(field = field))
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortState.field == field,
                            onClick = {
                                onSortStateChange(sortState.copy(field = field))
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = field,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort Direction
                Text(
                    text = "Sort Direction",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortState.ascending,
                            onClick = {
                                onSortStateChange(sortState.copy(ascending = true))
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Ascending")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !sortState.ascending,
                            onClick = {
                                onSortStateChange(sortState.copy(ascending = false))
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Descending")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply Sort")
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
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Cars")
        },
        text = {
            Text("Are you sure you want to delete the selected cars? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
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
fun ExportDialog(
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Format") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val formats = listOf("CSV", "JSON", "PDF")
                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFormat = format
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = {
                                selectedFormat = format
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = format,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(selectedFormat)
                    onDismiss()
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ShareDialog(
    onShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var shareText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Collection") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = shareText,
                    onValueChange = { shareText = it },
                    label = { Text("Share Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onShare(shareText)
                    onDismiss()
                }
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search cars...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CategoryBrandDiscovery(
    categoryId: String,
    onAddCarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryBrands = getCategoryBrands(categoryId)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Discover ${getCategoryDisplayName(categoryId)} Cars",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Popular brands in this category:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brand chips
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(categoryBrands) { brand ->
                SuggestionChip(
                    onClick = { /* Could navigate to brand-specific add screen */ },
                    label = { 
                        Text(
                            brand,
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddCarClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your Car")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Start building your ${getCategoryDisplayName(categoryId).lowercase()} collection by adding cars from these popular brands!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun getCategoryBrands(categoryId: String): List<String> {
    return when (categoryId) {
        "rally" -> listOf(
            "Subaru", "Mitsubishi", "Ford", "Lancia", "Audi", "Toyota", 
            "Hyundai", "Citroën", "Peugeot", "Škoda"
        )
        "hot_roads" -> listOf(
            "Nissan", "Honda", "BMW", "Toyota", "Mazda", "Volkswagen",
            "Audi", "Mercedes", "Lexus", "Infiniti"
        )
        "supercars" -> listOf(
            "Ferrari", "Lamborghini", "McLaren", "Porsche", "Bugatti",
            "Koenigsegg", "Pagani", "Aston Martin", "Maserati", "Lotus"
        )
        "american_muscle" -> listOf(
            "Ford", "Chevrolet", "Dodge", "Plymouth", "Pontiac",
            "Buick", "Oldsmobile", "Chrysler", "Cadillac", "Mercury"
        )
        "convertibles" -> listOf(
            "Porsche", "Ferrari", "BMW", "Mercedes", "Mazda",
            "Ford", "Chevrolet", "Audi", "Jaguar", "Alfa Romeo"
        )
        "vans" -> listOf(
            "Volkswagen", "Chevrolet", "Ford", "Nissan", "Mercedes",
            "Fiat", "Renault", "Toyota", "Honda", "Hyundai"
        )
        "motorcycle" -> listOf(
            "Harley Davidson", "Ducati", "Honda", "Yamaha", "Kawasaki",
            "Suzuki", "BMW", "Triumph", "Indian", "KTM"
        )
        "suv_pickups" -> listOf(
            "Ford", "Chevrolet", "Toyota", "Jeep", "Ram", "Nissan",
            "Honda", "Subaru", "Mazda", "Hyundai"
        )
        else -> emptyList() // No fallback toy brands
    }
}

private fun getCategoryDisplayName(categoryId: String): String {
    return when (categoryId) {
        "rally" -> "Rally"
        "hot_roads" -> "Hot Rods"
        "supercars" -> "Supercar"
        "american_muscle" -> "American Muscle"
        "convertibles" -> "Convertible"
        "vans" -> "Van"
        "motorcycle" -> "Motorcycle"
        "suv_pickups" -> "SUV & Pickup"
        else -> "Die-Cast"
    }
}