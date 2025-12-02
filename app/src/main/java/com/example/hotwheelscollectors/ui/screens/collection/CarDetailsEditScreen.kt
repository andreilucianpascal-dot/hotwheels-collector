package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CarDetailsEditScreen(
    navController: NavController,
    carId: String
) {
    // Car details state
    var carModel by remember { mutableStateOf("") }
    var carYear by remember { mutableStateOf("") }
    var carColor by remember { mutableStateOf("") }
    var carDescription by remember { mutableStateOf("") }
    
    // Search suggestions state
    var modelSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var yearSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var colorSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // All available options
    val allModels = remember {
        listOf(
            // American Brands - Chevrolet
            "Camaro SS", "Camaro Z28", "Camaro IROC-Z", "Corvette Stingray", "Corvette Z06",
            "Chevelle SS", "Nova SS", "Impala", "Silverado", "Blazer", "El Camino",
            "Tahoe", "Suburban", "Malibu", "Monte Carlo",

            // Ford models  
            "Mustang GT", "Mustang Shelby", "Mustang Fastback", "F-150", "F-150 Lightning",
            "GT40", "Bronco", "Escort RS", "Focus RS", "Fiesta ST", "Explorer",
            "Thunderbird", "Torino", "Galaxie", "Falcon", "Ranger",

            // Dodge models
            "Challenger", "Charger", "Viper", "Dart", "Coronet", "Super Bee",
            "Durango", "Ram 1500", "Caravan", "Journey",

            // Plymouth models
            "Barracuda", "Road Runner", "Fury", "Duster", "Satellite",
            "GTX", "Valiant", "Belvedere",

            // Pontiac models
            "GTO", "Firebird", "Trans Am", "Grand Prix", "Bonneville",
            "Tempest", "LeMans", "Grand Am",

            // Buick models
            "Skylark", "Grand National", "Regal", "Riviera", "Wildcat",

            // Cadillac models
            "Eldorado", "DeVille", "CTS-V", "Escalade", "Fleetwood",

            // Japanese Brands - Toyota
            "Supra", "Celica", "AE86", "Corolla", "Camry", "4Runner",
            "Land Cruiser", "Tundra", "Tacoma", "Prius", "Yaris",

            // Honda models
            "Civic Type R", "Civic Si", "Accord", "S2000", "NSX",
            "CR-X", "Prelude", "Fit", "Pilot", "Ridgeline",

            // Nissan models  
            "Skyline GT-R", "350Z", "370Z", "240SX", "Silvia", "Maxima",
            "Altima", "Sentra", "Frontier", "Titan", "Pathfinder",

            // Mazda models
            "RX-7", "RX-8", "Miata", "Speed3", "CX-5", "CX-9",
            "Protege", "626", "787B", "Cosmo",

            // Subaru models
            "WRX STI", "Impreza", "Legacy", "Outback", "Forester",
            "BRZ", "Baja", "SVX",

            // Mitsubishi models
            "Lancer Evolution", "Eclipse", "3000GT", "Galant", "Montero",
            "Outlander", "Pajero",

            // German Brands - BMW
            "M3", "M4", "M5", "i8", "Z4", "X6", "330i", "528i",
            "M1", "2002", "E30", "E36", "E46",

            // Mercedes-Benz models
            "SLS AMG", "C63 AMG", "E-Class", "S-Class", "SL-Class",
            "G-Wagon", "ML-Class", "CLS", "A-Class",

            // Audi models
            "R8", "TT", "A4", "A6", "A8", "S4", "S6", "RS6",
            "Quattro", "Q7", "Q5",

            // Volkswagen models
            "Golf GTI", "Beetle", "Jetta", "Passat", "Touareg",
            "Scirocco", "Corrado", "Karmann Ghia",

            // Porsche models  
            "911", "911 Turbo", "Carrera GT", "Cayman", "Boxster",
            "Macan", "Cayenne", "Panamera", "918 Spyder",

            // Italian Brands - Ferrari
            "488 GTB", "F40", "Enzo", "LaFerrari", "Testarossa",
            "458 Italia", "F12 Berlinetta", "California", "Roma",
            "SF90 Stradale", "812 Superfast",

            // Lamborghini models
            "Aventador", "Huracán", "Gallardo", "Murciélago", "Countach",
            "Diablo", "Miura", "Espada", "Urus",

            // Maserati models
            "GranTurismo", "Quattroporte", "Ghibli", "Levante", "MC20",

            // Pagani models
            "Zonda", "Huayra", "Utopia",

            // McLaren models
            "720S", "P1", "650S", "570S", "F1", "Senna", "Artura",

            // British Brands - Jaguar
            "F-Type", "XK", "XJ", "XF", "E-Type", "XKE", "F-Pace",

            // Aston Martin models
            "DB11", "Vantage", "DBS", "Vanquish", "One-77", "Valkyrie",

            // Bentley models
            "Continental GT", "Bentayga", "Flying Spur", "Mulsanne",

            // Lotus models
            "Elise", "Exige", "Evora", "Esprit", "Elan",

            // Land Rover models
            "Range Rover", "Discovery", "Defender", "Freelander", "Evoque",

            // Mini models
            "Cooper S", "Countryman", "Clubman", "Convertible",

            // French Brands - Peugeot
            "205 GTI", "306", "406", "505", "206 WRC",

            // Renault models
            "Clio", "Megane", "Alpine A110", "5 Turbo", "R8",

            // Citroën models
            "2CV", "DS", "CX", "Xsara WRC", "C4 WRC",

            // Swedish Brands - Volvo
            "240", "850", "V70", "XC90", "P1800", "Amazon",

            // Koenigsegg models
            "Agera", "Regera", "Jesko", "One:1", "CCX",

            // Motorcycle Brands
            "Yamaha R1", "Kawasaki Ninja", "Honda CBR", "Suzuki GSXR",
            "Ducati Panigale", "Harley Sportster", "BMW R1250GS",
            "Indian Scout", "Triumph Bonneville",

            // Hot Wheels Fantasy Cars
            "Bone Shaker", "Twin Mill", "Deora II", "Sling Shot", "16 Angels",
            "What-4-2", "Twinduction", "Carbonic", "Synkro", "Night Shifter",
            "Rip Rod", "Speed Blaster", "Jet Threat", "Splittin' Image",
            "Power Pistons", "Altered State", "Barbaric", "Bedlam"
        )
    }
    
    val allYears = remember {
        (1968..2024).reversed().map { it.toString() }
    }
    
    val allColors = remember {
        listOf(
            "Red", "Blue", "Black", "White", "Silver", "Gold", "Yellow", "Green",
            "Orange", "Purple", "Pink", "Gray", "Brown", "Cyan", "Magenta", "Lime",
            "Chrome", "Pearl White", "Metallic Blue", "Metallic Red", "Metallic Green",
            "Metallic Silver", "Metallic Gold", "Flat Black", "Matte Blue", "Satin Silver"
        )
    }

    // Update suggestions based on user input
    LaunchedEffect(carModel) {
        modelSuggestions = if (carModel.length >= 2) {
            allModels.filter { 
                it.contains(carModel, ignoreCase = true) 
            }.take(5)
        } else {
            emptyList()
        }
    }
    
    LaunchedEffect(carYear) {
        yearSuggestions = if (carYear.isNotEmpty()) {
            allYears.filter { 
                it.startsWith(carYear) 
            }.take(10)
        } else {
            emptyList()
        }
    }
    
    LaunchedEffect(carColor) {
        colorSuggestions = if (carColor.length >= 2) {
            allColors.filter { 
                it.contains(carColor, ignoreCase = true) 
            }.take(5)
        } else {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Edit Car Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { 
                    // Save changes
                    navController.navigateUp()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Add details to your car",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Car Model with Search + Suggestions
            item {
                Column {
                    OutlinedTextField(
                        value = carModel,
                        onValueChange = { carModel = it },
                        label = { Text("Car Model (start typing...)") },
                        placeholder = { Text("e.g. Cam, Ferr, Must") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (carModel.isNotEmpty()) {
                                IconButton(onClick = { carModel = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        }
                    )
                    
                    // Model Suggestions
                    if (modelSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Suggestions:",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                                modelSuggestions.forEach { suggestion ->
                                    TextButton(
                                        onClick = { carModel = suggestion },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Text(suggestion)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Year with Search + Suggestions  
            item {
                Column {
                    OutlinedTextField(
                        value = carYear,
                        onValueChange = { carYear = it },
                        label = { Text("Year (start typing...)") },
                        placeholder = { Text("e.g. 20, 199, 1968") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (carYear.isNotEmpty()) {
                                IconButton(onClick = { carYear = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        }
                    )
                    
                    // Year Suggestions
                    if (yearSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Years:",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    yearSuggestions.take(5).forEach { suggestion ->
                                        FilterChip(
                                            onClick = { carYear = suggestion },
                                            label = { Text(suggestion) },
                                            selected = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Color with Search + Suggestions
            item {
                Column {
                    OutlinedTextField(
                        value = carColor,
                        onValueChange = { carColor = it },
                        label = { Text("Color (start typing...)") },
                        placeholder = { Text("e.g. Red, Meta, Chrome") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (carColor.isNotEmpty()) {
                                IconButton(onClick = { carColor = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        }
                    )
                    
                    // Color Suggestions
                    if (colorSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Colors:",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colorSuggestions.take(3).forEach { suggestion ->
                                        FilterChip(
                                            onClick = { carColor = suggestion },
                                            label = { Text(suggestion) },
                                            selected = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Description (Free text)
            item {
                OutlinedTextField(
                    value = carDescription,
                    onValueChange = { carDescription = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Yellow with black stripes, Limited edition") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        // Save the car details
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Details")
                }
            }
        }
    }
}