package com.example.hotwheelscollectors.ui.screens.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.viewmodels.EditCarDetailsViewModel
import com.example.hotwheelscollectors.viewmodels.EditCarUiState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun EditCarDetailsScreen(
    carId: String,
    navController: NavController,
    viewModel: EditCarDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope() // ✅ FIX: Mutat în afara LaunchedEffect
    
    var showColorDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showCarModelsDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Load car data when screen opens
    LaunchedEffect(carId) {
        viewModel.loadCar(carId)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is EditCarUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Car details updated successfully!"
                )
                navController.navigateUp()
            }
            is EditCarUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as EditCarUiState.Error).message
                )
                // ✅ FIX: Navighează înapoi după eroare pentru a evita blocarea
                // Folosim coroutineScope deja definit la nivel de Composable
                coroutineScope.launch {
                    delay(2000) // 2 secunde pentru user să vadă mesajul
                    if (viewModel.uiState.value is EditCarUiState.Error &&
                        navController.currentDestination?.route != "main") {
                        navController.navigateUp()
                    }
                }
            }
            else -> {}
        }
    }

    // ✅ FIX: BackHandler funcționează întotdeauna, dar arată dialog doar dacă sunt schimbări nesalvate
    BackHandler(enabled = true) {
        if (viewModel.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            // Dacă nu sunt schimbări, navighează direct înapoi (chiar dacă apare eroare)
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Car Details") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (viewModel.hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, "Navigate back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.saveChanges()
                        },
                        enabled = viewModel.name.isNotEmpty() && viewModel.hasUnsavedChanges
                    ) {
                        Icon(Icons.Default.Save, "Save changes")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Photo Section (read-only, just display existing photos)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Front and back photos are already saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Car Model (EDITABLE)
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Model") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                trailingIcon = {
                    IconButton(onClick = { showCarModelsDialog = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Select from list")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Series (READ-ONLY)
            OutlinedTextField(
                value = viewModel.series,
                onValueChange = { },
                label = { Text("Series") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category (READ-ONLY)
            OutlinedTextField(
                value = viewModel.category, // Using category field for category display
                onValueChange = { },
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Brand (EDITABLE doar pentru tipurile non-Mainline)
            OutlinedTextField(
                value = viewModel.brand,
                onValueChange = { if (viewModel.isBrandEditable) viewModel.updateBrand(it) },
                label = { Text("Brand") },
                readOnly = !viewModel.isBrandEditable,
                trailingIcon = {
                    if (viewModel.isBrandEditable) {
                        IconButton(onClick = { showBrandDialog = true }) {
                            Icon(Icons.Default.ArrowDropDown, "Select brand")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode (READ-ONLY)
            OutlinedTextField(
                value = viewModel.barcode,
                onValueChange = { },
                label = { Text("Barcode") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color and Year Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.color,
                    onValueChange = { viewModel.updateColor(it) },
                    label = { Text("Color") },
                    trailingIcon = {
                        IconButton(onClick = { showColorDialog = true }) {
                            Icon(Icons.Default.Palette, "Select color")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = viewModel.year?.toString() ?: "",
                    onValueChange = { 
                        it.toIntOrNull()?.let { year -> 
                            viewModel.updateYear(year) 
                        }
                    },
                    label = { Text("Year") },
                    trailingIcon = {
                        IconButton(onClick = { showYearDialog = true }) {
                            Icon(Icons.Default.CalendarToday, "Select year")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes (EDITABLE)
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Car Type Info (READ-ONLY)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Car Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type: ${viewModel.carType.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Series: ${viewModel.series}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Category: ${viewModel.category}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!viewModel.isBrandEditable) {
                        Text(
                            text = "Brand is read-only for Mainline cars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Color Selection Dialog
    if (showColorDialog) {
        ColorSelectionDialog(
            selectedColor = viewModel.color,
            onColorSelected = { viewModel.updateColor(it) },
            onDismiss = { showColorDialog = false }
        )
    }

    // Year Selection Dialog
    if (showYearDialog) {
        YearSelectionDialog(
            selectedYear = viewModel.year,
            onYearSelected = { viewModel.updateYear(it) },
            onDismiss = { showYearDialog = false }
        )
    }

    // Car Models Dialog
    if (showCarModelsDialog) {
        CarModelsDialog(
            onModelSelected = { viewModel.updateName(it) },
            onDismiss = { showCarModelsDialog = false }
        )
    }

    // Brand Selection Dialog
    if (showBrandDialog) {
        BrandSelectionDialog(
            selectedBrand = viewModel.brand,
            onBrandSelected = { viewModel.updateBrand(it) },
            onDismiss = { showBrandDialog = false }
        )
    }

    // Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { viewModel.updateBarcode(it) },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Discard Changes Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes") },
            text = { Text("Are you sure you want to discard your changes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.resetChanges()
                        navController.navigateUp()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ColorSelectionDialog(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colorCategories = mapOf(
        "Normal" to listOf("Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Black", "White", "Gray", "Brown"),
        "Metallic" to listOf("Silver", "Gold", "Chrome", "Metallic Blue", "Metallic Red", "Metallic Green", "Metallic Silver", "Metallic Gold"),
        "Special" to listOf("Pearl White", "Flat Black", "Matte Blue", "Satin Silver", "Candy Red", "Candy Blue")
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    colorCategories.forEach { (category, colors) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        colors.forEach { color ->
                            item {
                                TextButton(
                                    onClick = { 
                                        onColorSelected(color)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(color)
                                }
                            }
                        }
                    }
                    
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Button(
                            onClick = { 
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ADD COLOR")
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun YearSelectionDialog(
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = (1968..2100).toList().reversed()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Text(
                    text = "Select Year",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(years) { year ->
                        TextButton(
                            onClick = { 
                                onYearSelected(year)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(year.toString())
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun CarModelsDialog(
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allCarModels = remember {
        com.example.hotwheelscollectors.domain.catalog.ModelCatalog.getCommonModels()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Text(
                    text = "Select Car Model",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(allCarModels) { model ->
                        TextButton(
                            onClick = { 
                                onModelSelected(model)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(model)
                        }
                    }
                    
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Button(
                            onClick = { 
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ADD MODEL")
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun BrandSelectionDialog(
    selectedBrand: String,
    onBrandSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allBrands = remember {
        com.example.hotwheelscollectors.domain.catalog.BrandCatalog
            .getAllBrandDisplayNames()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Text(
                    text = "Select Brand",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(allBrands) { brand ->
                        TextButton(
                            onClick = { 
                                onBrandSelected(brand)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(brand)
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Barcode") },
        text = { Text("Barcode scanner functionality") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
