package com.example.hotwheelscollectors.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.ui.components.loading.LoadingScreen
import com.example.hotwheelscollectors.ui.components.error.ErrorScreen
import com.example.hotwheelscollectors.viewmodels.CarDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CarDetailsScreen(
    carId: String,
    navController: NavController,
    viewModel: CarDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(carId) {
        viewModel.loadCar(carId)
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Car Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState) {
                is CarDetailsViewModel.UiState.Loading -> {
                    LoadingScreen()
                }
                is CarDetailsViewModel.UiState.Success -> {
                    val carWithPhotos = (uiState as CarDetailsViewModel.UiState.Success).carWithPhotos
                    val car = carWithPhotos.car
                    CarDetails(
                        car = car,
                        contentPadding = PaddingValues(0.dp),
                        onPhotoClick = { photoIndex ->
                            navController.navigate("photo_viewer/${car.id}/$photoIndex")
                        },
                        onModelEditClick = { showModelDropdown = true },
                        onYearEditClick = { showYearDropdown = true },
                        onColorEditClick = { showColorDropdown = true }
                    )

                    // Model Dropdown Dialog
                    if (showModelDropdown) {
                        ModelDropdownDialog(
                            currentModel = car.model,
                            brand = car.brand,
                            onModelSelected = { newModel ->
                                viewModel.updateModel(car.id, newModel)
                                showModelDropdown = false
                            },
                            onDismiss = { showModelDropdown = false }
                        )
                    }

                    // Year Dropdown Dialog
                    if (showYearDropdown) {
                        YearDropdownDialog(
                            currentYear = car.year,
                            onYearSelected = { newYear ->
                                viewModel.updateYear(car.id, newYear)
                                showYearDropdown = false
                            },
                            onDismiss = { showYearDropdown = false }
                        )
                    }

                    // Color Dropdown Dialog
                    if (showColorDropdown) {
                        ColorDropdownDialog(
                            currentColor = car.color,
                            onColorSelected = { newColor ->
                                viewModel.updateColor(car.id, newColor)
                                showColorDropdown = false
                            },
                            onDismiss = { showColorDropdown = false }
                        )
                    }
                }
                is CarDetailsViewModel.UiState.Error -> {
                    ErrorScreen(
                        message = (uiState as CarDetailsViewModel.UiState.Error).message,
                        onRetry = { viewModel.loadCar(carId) }
                    )
                }
                is CarDetailsViewModel.UiState.Deleted -> {
                    LaunchedEffect(Unit) {
                        navController.navigateUp()
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Car") },
            text = { Text("Are you sure you want to delete this car? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCar(carId)
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        val carWithPhotos = (uiState as? CarDetailsViewModel.UiState.Success)?.carWithPhotos
        if (carWithPhotos != null) {
            val car = carWithPhotos.car
            val carType = when {
                car.isPremium -> "premium"
                car.isSTH -> "sth"
                car.isTH -> "th"
                else -> "mainline"
            }

            when (carType) {
                "mainline" -> navController.navigate("edit_mainline/${car.id}")
                "premium" -> navController.navigate("edit_premium/${car.id}")
                "sth" -> navController.navigate("edit_sth/${car.id}")
                "th" -> navController.navigate("edit_th/${car.id}")
            }
            showEditDialog = false
        }
    }

    if (showShareDialog) {
        val carWithPhotos = (uiState as? CarDetailsViewModel.UiState.Success)?.carWithPhotos
        if (carWithPhotos != null) {
            val car = carWithPhotos.car
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Share Car") },
                text = { Text("Share details for ${car.brand} ${car.model}") },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Share")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CarDetails(
    car: CarEntity,
    contentPadding: PaddingValues,
    onPhotoClick: (Int) -> Unit,
    onModelEditClick: () -> Unit,
    onYearEditClick: () -> Unit,
    onColorEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            val photos = listOfNotNull(
                car.frontPhotoPath,
                car.combinedPhotoPath
            ).filter { it.isNotEmpty() }
            // Note: Removed backPhotoPath as it should be deleted after barcode extraction

            if (photos.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photos[0])
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit, // ✅ FIXED: Use Fit instead of Crop to show full image
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPhotoClick(0) }
                    )

                    if (photos.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(photos.size) { page ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (page == 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = car.brand,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (car.number.isNotEmpty()) {
                        Chip(
                            label = "#${car.number}",
                            icon = Icons.Default.Tag
                        )
                    }

                    // Always show Year and Color chips for editing, even if empty
                    EditableChip(
                        label = if (car.year > 0) car.year.toString() else "Add Year",
                        icon = Icons.Default.CalendarToday,
                        onEditClick = onYearEditClick
                    )

                    EditableChip(
                        label = if (car.color.isNotEmpty()) car.color else "Add Color",
                        icon = Icons.Default.Palette,
                        onEditClick = onColorEditClick
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                DetailSection(title = "Details") {
                    if (car.series.isNotEmpty()) {
                        DetailItem(
                            label = "Series",
                            value = car.series
                        )
                    }

                    if (car.subseries.isNotEmpty()) {
                        DetailItem(
                            label = "Category",
                            value = car.subseries
                        )
                    }

                    if (car.brand.isNotEmpty()) {
                        DetailItem(
                            label = "Brand",
                            value = car.brand
                        )
                    }

                    EditableDetailItem(
                        label = "Model",
                        value = if (car.model.isNotEmpty()) car.model else "Add Model",
                        onEditClick = onModelEditClick
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DetailSection(title = "Collection Info") {
                    DetailItem(
                        label = "Added On",
                        value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(Date(car.timestamp))
                    )

                    if (car.barcode.isNotEmpty()) {
                        DetailItem(
                            label = "Barcode",
                            value = car.barcode
                        )
                    }
                }
            }
        }

        item {
            if (car.notes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = car.notes,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        content()
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun Chip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EditableDetailItem(
    label: String,
    value: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EditableChip(
    label: String,
    icon: ImageVector,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onEditClick() },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// Model Dropdown Dialog
@Composable
private fun ModelDropdownDialog(
    currentModel: String,
    brand: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val models = remember(brand) {
        when (brand.lowercase()) {
            // FERRARI
            "ferrari" -> listOf(
                "250 GTO", "288 GTO", "308 GTB", "328 GTB", "348", "355", "360 Modena", "430 Scuderia", 
                "458 Italia", "488 GTB", "512 BB", "575M Maranello", "599 GTB", "612 Scaglietti", 
                "California", "Enzo Ferrari", "F12 Berlinetta", "F40", "F50", "F355", "F430", 
                "FF", "LaFerrari", "Portofino", "Roma", "SF90 Stradale", "Testarossa"
            )
            
            // LAMBORGHINI
            "lamborghini" -> listOf(
                "Aventador", "Aventador LP 700-4", "Aventador SVJ", "Countach", "Diablo", 
                "Gallardo", "Huracán", "Huracán Performante", "Miura", "Murciélago", 
                "Reventón", "Sesto Elemento", "Silhouette", "Urus", "Veneno"
            )
            
            // PORSCHE
            "porsche" -> listOf(
                "911", "911 Carrera", "911 GT2", "911 GT3", "911 Turbo", "918 Spyder", 
                "924", "928", "944", "959", "962", "Boxster", "Carrera GT", "Cayenne", 
                "Cayman", "Macan", "Panamera", "Taycan"
            )
            
            // FORD
            "ford" -> listOf(
                "Bronco", "Escort", "Explorer", "F-150", "F-150 Raptor", "Fiesta", "Focus", 
                "GT", "GT40", "Mustang", "Mustang Boss 302", "Mustang GT", "Mustang Mach 1", 
                "Mustang Shelby GT350", "Mustang Shelby GT500", "Ranger", "Thunderbird", "Transit"
            )
            
            // CHEVROLET
            "chevrolet", "chevy" -> listOf(
                "Bel Air", "Blazer", "Camaro", "Camaro SS", "Camaro Z28", "Chevelle", "Corvette", 
                "Corvette C1", "Corvette C2", "Corvette C3", "Corvette C4", "Corvette C5", 
                "Corvette C6", "Corvette C7", "Corvette C8", "Corvette Stingray", "Cruze", 
                "El Camino", "Impala", "Malibu", "Nova", "Silverado", "Suburban", "Tahoe"
            )
            
            // DODGE
            "dodge" -> listOf(
                "Challenger", "Challenger Hellcat", "Challenger SRT", "Charger", "Charger Hellcat", 
                "Charger R/T", "Dart", "Durango", "Ram 1500", "Ram 2500", "Super Bee", "Viper", 
                "Viper ACR", "Viper GTS"
            )
            
            // BMW
            "bmw" -> listOf(
                "1 Series", "2 Series", "3 Series", "4 Series", "5 Series", "6 Series", "7 Series", 
                "8 Series", "i3", "i8", "M1", "M2", "M3", "M4", "M5", "M6", "M8", "X1", "X2", 
                "X3", "X4", "X5", "X6", "X7", "Z3", "Z4", "Z8"
            )
            
            // MERCEDES
            "mercedes" -> listOf(
                "190E", "300SL", "A-Class", "AMG GT", "B-Class", "C-Class", "CLA", "CLS", "E-Class", 
                "G-Class", "GLA", "GLB", "GLC", "GLE", "GLS", "ML", "S-Class", "SL", "SLK", "SLR McLaren", 
                "SLS AMG", "Sprinter", "Vito"
            )
            
            // AUDI
            "audi" -> listOf(
                "80", "90", "100", "A1", "A3", "A4", "A5", "A6", "A7", "A8", "e-tron", "Q2", "Q3", 
                "Q4", "Q5", "Q7", "Q8", "Quattro", "R8", "RS3", "RS4", "RS5", "RS6", "RS7", "S3", 
                "S4", "S5", "S6", "S7", "S8", "TT", "TTS"
            )
            
            // VOLKSWAGEN
            "volkswagen" -> listOf(
                "Beetle", "Bus", "Caddy", "Golf", "Golf GTI", "Golf R", "Jetta", "Passat", "Polo", 
                "Scirocco", "Tiguan", "Touareg", "Up!"
            )
            
            // TOYOTA
            "toyota" -> listOf(
                "4Runner", "86", "Avalon", "Camry", "Celica", "Corolla", "Crown", "FJ Cruiser", 
                "Highlander", "Land Cruiser", "MR2", "Prius", "RAV4", "Sequoia", "Sienna", 
                "Supra", "Tacoma", "Tundra", "Yaris"
            )
            
            // HONDA
            "honda" -> listOf(
                "Accord", "Civic", "Civic Si", "Civic Type R", "CR-V", "CR-Z", "Element", "Fit", 
                "Insight", "NSX", "Odyssey", "Pilot", "Prelude", "Ridgeline", "S2000", "CBR", 
                "CBR600RR", "CBR1000RR"
            )
            
            // NISSAN
            "nissan" -> listOf(
                "350Z", "370Z", "Altima", "Armada", "GT-R", "Juke", "Leaf", "Maxima", "Murano", 
                "Pathfinder", "Rogue", "Sentra", "Skyline", "Skyline GT-R", "Titan", "Versa", "Xterra"
            )
            
            // MAZDA
            "mazda" -> listOf(
                "3", "6", "787B", "CX-3", "CX-5", "CX-9", "Miata", "MX-5", "RX-7", "RX-8"
            )
            
            // SUBARU
            "subaru" -> listOf(
                "Ascent", "BRZ", "Forester", "Impreza", "Impreza WRX", "Impreza WRX STI", 
                "Legacy", "Outback", "SVX", "Tribeca", "WRX", "WRX STI"
            )
            
            // MITSUBISHI
            "mitsubishi" -> listOf(
                "3000GT", "Eclipse", "Evo", "Galant", "Lancer", "Lancer Evolution", "Mirage", 
                "Montero", "Outlander", "Pajero"
            )
            
            // JEEP
            "jeep" -> listOf(
                "Cherokee", "Compass", "Grand Cherokee", "Grand Cherokee SRT", "Grand Wagoneer", 
                "Gladiator", "Liberty", "Patriot", "Renegade", "Wagoneer", "Wrangler", "Wrangler Rubicon"
            )
            
            // CHRYSLER
            "chrysler" -> listOf(
                "300", "300C", "300 SRT", "Aspen", "Crossfire", "LeBaron", "Pacifica", "PT Cruiser", 
                "Sebring", "Town & Country", "Voyager"
            )
            
            // PONTIAC
            "pontiac" -> listOf(
                "Aztek", "Bonneville", "Fiero", "Firebird", "G6", "G8", "Grand Am", "Grand Prix", 
                "GTO", "Solstice", "Sunfire", "Trans Am"
            )
            
            // BUICK
            "buick" -> listOf(
                "Enclave", "Encore", "Envision", "Grand National", "LaCrosse", "Lucerne", 
                "Park Avenue", "Regal", "Riviera", "Skylark", "Verano"
            )
            
            // CADILLAC
            "cadillac" -> listOf(
                "ATS", "CTS", "CTS-V", "DeVille", "DTS", "Eldorado", "Escalade", "Fleetwood", 
                "SRX", "STS", "XLR", "XT4", "XT5", "XT6"
            )
            
            // MCLAREN
            "mclaren" -> listOf(
                "12C", "540C", "570S", "600LT", "650S", "675LT", "720S", "765LT", "Artura", 
                "F1", "MP4-12C", "P1", "Senna"
            )
            
            // BUGATTI
            "bugatti" -> listOf(
                "Chiron", "Chiron Sport", "Divo", "EB110", "Veyron", "Veyron Grand Sport", 
                "Veyron Super Sport"
            )
            
            // ASTON MARTIN
            "aston martin" -> listOf(
                "DB5", "DB7", "DB9", "DB11", "DBS", "DBX", "Rapide", "V8 Vantage", "V12 Vantage", 
                "Vanquish", "Vantage", "Vulcan"
            )
            
            // BENTLEY
            "bentley" -> listOf(
                "Arnage", "Bentayga", "Continental", "Continental GT", "Flying Spur", "Mulsanne"
            )
            
            // MASERATI
            "maserati" -> listOf(
                "GranTurismo", "Levante", "MC20", "Quattroporte", "Ghibli"
            )
            
            // PAGANI
            "pagani" -> listOf(
                "Huayra", "Zonda", "Zonda C12", "Zonda F", "Zonda R"
            )
            
            // KOENIGSEGG
            "koenigsegg" -> listOf(
                "Agera", "Agera R", "Agera RS", "CC8S", "CCR", "CCXR", "Gemera", "Jesko", 
                "One:1", "Regera"
            )
            
            // LOTUS
            "lotus" -> listOf(
                "Elise", "Esprit", "Evora", "Exige"
            )
            
            // JAGUAR
            "jaguar" -> listOf(
                "E-Type", "F-Pace", "F-Type", "I-Pace", "XE", "XF", "XJ", "XK", "XKR"
            )
            
            // LAND ROVER
            "land rover" -> listOf(
                "Defender", "Discovery", "Discovery Sport", "Evoque", "Freelander", "Range Rover", 
                "Range Rover Sport", "Range Rover Velar"
            )
            
            // MINI
            "mini" -> listOf(
                "Cooper", "Cooper S", "Countryman", "Clubman", "Paceman"
            )
            
            // VOLVO
            "volvo" -> listOf(
                "240", "740", "850", "S40", "S60", "S80", "S90", "V40", "V60", "V70", "V90", 
                "XC40", "XC60", "XC70", "XC90"
            )
            
            // ALFA ROMEO
            "alfa romeo" -> listOf(
                "4C", "Giulia", "Giulietta", "GTV", "Spider", "Stelvio"
            )
            
            // FIAT
            "fiat" -> listOf(
                "124 Spider", "500", "500 Abarth", "Panda", "Punto", "Uno"
            )
            
            // LANCIA
            "lancia" -> listOf(
                "Delta", "Delta HF Integrale", "Stratos"
            )
            
            // PEUGEOT
            "peugeot" -> listOf(
                "205", "206", "207", "208", "306", "307", "308", "405", "406", "407", "504", "505"
            )
            
            // CITROEN
            "citroen" -> listOf(
                "2CV", "C1", "C3", "C4", "C5", "DS", "Xantia", "Xsara"
            )
            
            // RENAULT
            "renault" -> listOf(
                "5", "Clio", "Megane", "Scenic", "Twingo"
            )
            
            // OPEL
            "opel" -> listOf(
                "Astra", "Corsa", "Insignia", "Vectra", "Zafira"
            )
            
            // DATSUN
            "datsun" -> listOf(
                "240Z", "260Z", "280Z", "280ZX", "510", "620"
            )
            
            // SUZUKI
            "suzuki" -> listOf(
                "Alto", "Jimny", "Swift", "SX4", "Vitara", "GSX-R600", "GSX-R750", "GSX-R1000", "Hayabusa"
            )
            
            // KAWASAKI
            "kawasaki" -> listOf(
                "Ninja 250", "Ninja 300", "Ninja 400", "Ninja 600", "Ninja 650", "Ninja 1000", 
                "Ninja ZX-6R", "Ninja ZX-10R", "Z900", "Vulcan"
            )
            
            // YAMAHA
            "yamaha" -> listOf(
                "R1", "R3", "R6", "YZF-R1", "YZF-R6", "MT-07", "MT-09", "FZ-09", "Virago"
            )
            
            // DUCATI
            "ducati" -> listOf(
                "Monster", "Panigale", "Panigale V4", "Streetfighter", "Supersport", "Diavel", 
                "Multistrada", "Scrambler"
            )
            
            // HARLEY DAVIDSON
            "harley davidson" -> listOf(
                "Fat Boy", "Heritage Softail", "Iron 883", "Road King", "Sportster", "Street Glide", 
                "Ultra Limited", "V-Rod"
            )
            
            // INDIAN
            "indian" -> listOf(
                "Chief", "Scout", "Springfield", "Roadmaster"
            )
            
            // TRIUMPH
            "triumph" -> listOf(
                "Bonneville", "Daytona", "Speed Triple", "Street Triple", "Tiger"
            )
            
            // HUMMER
            "hummer" -> listOf(
                "H1", "H2", "H3", "EV"
            )
            
            // GMC
            "gmc" -> listOf(
                "Acadia", "Canyon", "Denali", "Envoy", "Jimmy", "Safari", "Sierra", "Suburban", 
                "Terrain", "Yukon"
            )
            
            // RAM
            "ram" -> listOf(
                "1500", "2500", "3500", "ProMaster"
            )
            
            // LINCOLN
            "lincoln" -> listOf(
                "Aviator", "Continental", "Corsair", "MKC", "MKS", "MKX", "MKZ", "Navigator", "Town Car"
            )
            
            // MERCURY
            "mercury" -> listOf(
                "Cougar", "Grand Marquis", "Mariner", "Marquis", "Milan", "Montego", "Mountaineer", 
                "Sable"
            )
            
            // PLYMOUTH
            "plymouth" -> listOf(
                "Barracuda", "Cuda", "Duster", "Fury", "GTX", "Road Runner", "Satellite", 
                "Superbird", "Valiant", "Voyager"
            )
            
            // OLDSMOBILE
            "oldsmobile" -> listOf(
                "442", "Alero", "Aurora", "Cutlass", "Delta 88", "Intrigue", "Toronado"
            )
            
            // LEXUS
            "lexus" -> listOf(
                "ES", "GS", "IS", "LC", "LS", "LX", "NX", "RC", "RX", "SC", "UX"
            )
            
            // INFINITI
            "infiniti" -> listOf(
                "EX", "FX", "G35", "G37", "M", "Q50", "Q60", "Q70", "QX50", "QX60", "QX70", "QX80"
            )
            
            // ACURA
            "acura" -> listOf(
                "ILX", "Integra", "Legend", "MDX", "NSX", "RDX", "RL", "RSX", "TL", "TLX", "TSX", "ZDX"
            )
            
            // DAIHATSU
            "daihatsu" -> listOf(
                "Charade", "Copen", "Cuore", "Move", "Sirion", "Terios", "YRV"
            )
            
            // HOT WHEELS ORIGINALS
            "hot wheels" -> listOf(
                "16 Angels", "24 Ours", "32 Ford Roadster", "40 Ford Coupe", "55 Chevy Bel Air", 
                "57 Chevy", "Bone Shaker", "Carbonator", "Deora", "Deora II", "Evil Weevil", 
                "Fast 4WD", "RocketFire", "Split Vision", "Twin Mill", "Twin Mill II", "Twin Mill III"
            )
            
            // MATCHBOX
            "matchbox" -> listOf(
                "Ambulance", "Fire Truck", "Police Car", "School Bus", "Taxi", "Tow Truck"
            )
            
            // OPEL
            "opel" -> listOf(
                "Ascona", "Astra", "Calibra", "Corsa", "GT", "Kadett", "Manta", "Speedster", "Vectra"
            )
            
            // RIMAC
            "rimac" -> listOf(
                "Concept One", "Concept Two", "C_Two", "Nevera"
            )
            
            // LUCID AIR
            "lucid_air", "lucid air" -> listOf(
                "Air Dream", "Air Touring", "Air Pure", "Air Grand Touring"
            )
            
            // FORD GT
            "ford_gt", "ford gt" -> listOf(
                "GT40", "GT 2005", "GT 2017", "GT 2022", "Le Mans"
            )
            
            // MAZDA 787B
            "mazda_787b", "mazda 787b" -> listOf(
                "787B Le Mans", "787B Racing", "787B Prototype"
            )
            
            // AUTOMOBILI PININFARINA
            "automobili_pininfarina", "automobili pininfarina" -> listOf(
                "Battista", "B95 Gotham", "Farina"
            )
            
            // CHRYSLER
            "chrysler" -> listOf(
                "300", "300C", "300M", "Crossfire", "Imperial", "LeBaron", "New Yorker", 
                "Pacifica", "PT Cruiser", "Sebring", "Town & Country", "Voyager"
            )
            
            // OLDSMOBILE
            "oldsmobile" -> listOf(
                "442", "Cutlass", "Delta 88", "Hurst", "Toronado", "Vista Cruiser"
            )
            
            // LINCOLN
            "lincoln" -> listOf(
                "Continental", "Mark IV", "Mark V", "Navigator", "Town Car"
            )
            
            // MERCURY
            "mercury" -> listOf(
                "Cougar", "Grand Marquis", "Marauder", "Montego", "Sable"
            )
            
            // PLYMOUTH
            "plymouth" -> listOf(
                "Barracuda", "Duster", "Fury", "GTX", "Road Runner", "Satellite", "Superbird"
            )
            
            // CAMARO
            "camaro" -> listOf(
                "SS", "Z28", "ZL1", "1LE", "RS", "LT", "2SS", "1SS"
            )
            
            // CHEVY
            "chevy" -> listOf(
                "Bel Air", "Chevelle", "El Camino", "Impala", "Nova", "Silverado"
            )
            
            // CHEVELLE
            "chevelle" -> listOf(
                "SS", "Malibu", "Laguna", "Concours"
            )
            
            // EL CAMINO
            "el_camino", "el camino" -> listOf(
                "SS", "Classic", "Conquista", "Royal Knight"
            )
            
            // IMPALA
            "impala" -> listOf(
                "SS", "LT", "LS", "Premier", "Caprice"
            )
            
            // NOVA
            "nova" -> listOf(
                "SS", "Yenko", "Rally", "Custom"
            )
            
            // CHALLENGER
            "challenger" -> listOf(
                "SRT Hellcat", "SRT Demon", "R/T", "SXT", "T/A"
            )
            
            // CHARGER
            "charger" -> listOf(
                "SRT Hellcat", "SRT Demon", "R/T", "SXT", "Daytona"
            )
            
            // SUPER BEE
            "super_bee", "super bee" -> listOf(
                "A12", "Six Pack", "Coronet"
            )
            
            // MUSTANG
            "mustang" -> listOf(
                "GT", "Shelby", "Boss 302", "Mach 1", "Bullitt", "Eleanor", "Cobra"
            )
            
            // THUNDERBIRD
            "thunderbird" -> listOf(
                "Super Coupe", "Turbo Coupe", "Sport", "LX"
            )
            
            // COUGAR
            "cougar" -> listOf(
                "XR-7", "Eliminator", "Boss"
            )
            
            // BARRACUDA
            "barracuda" -> listOf(
                "Formula S", "Gran Coupe", "Cuda"
            )
            
            // FIREBIRD
            "firebird" -> listOf(
                "Trans Am", "Formula", "Esprit", "WS6"
            )
            
            // GTO
            "gto" -> listOf(
                "Judge", "Ram Air", "Tri-Power"
            )
            
            // MERCEDES
            "mercedes_benz", "mercedes" -> listOf(
                "A-Class", "C-Class", "E-Class", "S-Class", "AMG GT", "SL", "SLK", "SLS", "G-Class"
            )
            
            // GMC
            "gmc" -> listOf(
                "Sierra", "Yukon", "Acadia", "Terrain", "Canyon", "Savana"
            )
            
            // RAM
            "ram" -> listOf(
                "1500", "2500", "3500", "TRX", "Rebel", "Laramie"
            )
            
            // LEXUS
            "lexus" -> listOf(
                "LFA", "LC", "LS", "ES", "IS", "GS", "RX", "GX", "LX"
            )
            
            // INFINITI
            "infiniti" -> listOf(
                "G35", "G37", "Q50", "Q60", "QX70", "FX35", "FX45"
            )
            
            // ACURA
            "acura" -> listOf(
                "NSX", "Integra", "RSX", "TSX", "TLX", "MDX", "RDX"
            )
            
            // DAIHATSU
            "daihatsu" -> listOf(
                "Copen", "Charade", "Terios", "Sirion", "Move"
            )
            
            // FIAT
            "fiat" -> listOf(
                "500", "500 Abarth", "Panda", "Punto", "Bravo", "Tipo"
            )
            
            // ALFA ROMEO
            "alfa_romeo", "alfa romeo" -> listOf(
                "Giulia", "Stelvio", "4C", "Spider", "GTV", "156", "159"
            )
            
            // ABARTH
            "abarth" -> listOf(
                "595", "695", "124 Spider", "500 Abarth"
            )
            
            // RENAULT
            "renault" -> listOf(
                "Clio", "Megane", "Laguna", "Scenic", "Twingo", "Alpine A110"
            )
            
            // JAGUAR
            "jaguar" -> listOf(
                "F-Type", "XF", "XE", "XJ", "E-Pace", "F-Pace", "I-Pace"
            )
            
            // LAND ROVER
            "land_rover", "land rover" -> listOf(
                "Defender", "Discovery", "Range Rover", "Evoque", "Velar"
            )
            
            // MINI
            "mini" -> listOf(
                "Cooper", "Cooper S", "John Cooper Works", "Countryman", "Clubman"
            )
            
            // LOTUS
            "lotus" -> listOf(
                "Elise", "Exige", "Evora", "Emira", "Esprit"
            )
            
            // INDIAN
            "indian" -> listOf(
                "Scout", "Chief", "Roadmaster", "Springfield", "FTR"
            )
            
            // TRIUMPH
            "triumph" -> listOf(
                "Bonneville", "Street Triple", "Speed Triple", "Tiger", "Rocket"
            )
            
            // DEFAULT/OTHER BRANDS
            else -> listOf(
                "Model A", "Model T", "Roadster", "Coupe", "Sedan", "Wagon", "Convertible", 
                "Sports Car", "Race Car", "Concept Car", "Custom"
            )
        }.sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Model for $brand",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(models) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelected(model) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = model == currentModel,
                            onClick = { onModelSelected(model) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Custom input option
                item {
                    var customModel by remember { mutableStateOf("") }
                    var showCustomInput by remember { mutableStateOf(false) }
                    
                    if (showCustomInput) {
                        Column {
                            OutlinedTextField(
                                value = customModel,
                                onValueChange = { customModel = it },
                                label = { Text("Custom Model") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCustomInput = false }) {
                                    Text("Cancel")
                                }
                                TextButton(
                                    onClick = {
                                        if (customModel.isNotBlank()) {
                                            onModelSelected(customModel)
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomInput = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Custom Model",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Year Dropdown Dialog
@Composable
private fun YearDropdownDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember {
        (1968..2024).toList().reversed() // Die-cast cars started being mass produced in 1968
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Year",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(years) { year ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onYearSelected(year) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = year == currentYear,
                            onClick = { onYearSelected(year) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Color Dropdown Dialog
@Composable
private fun ColorDropdownDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = remember {
        listOf(
            "Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "Black", "White", "Gray",
            "Silver", "Gold", "Bronze", "Chrome", "Metallic Blue", "Metallic Red", "Metallic Green",
            "Dark Blue", "Light Blue", "Dark Red", "Light Green", "Dark Green", "Lime Green",
            "Hot Pink", "Magenta", "Cyan", "Turquoise", "Brown", "Tan", "Beige", "Cream",
            "Pearl White", "Matte Black", "Gloss Black", "Flat Black", "Satin Silver"
        ).sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Color",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(colors) { color ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelected(color) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = color == currentColor,
                            onClick = { onColorSelected(color) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = color,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Custom input option
                item {
                    var customColor by remember { mutableStateOf("") }
                    var showCustomInput by remember { mutableStateOf(false) }
                    
                    if (showCustomInput) {
                        Column {
                            OutlinedTextField(
                                value = customColor,
                                onValueChange = { customColor = it },
                                label = { Text("Custom Color") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCustomInput = false }) {
                                    Text("Cancel")
                                }
                                TextButton(
                                    onClick = {
                                        if (customColor.isNotBlank()) {
                                            onColorSelected(customColor)
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomInput = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Custom Color",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}