package com.example.hotwheelscollectors.ui.screens.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.viewmodels.AddMainlineViewModel

data class Brand(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class Subcategory(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun BrandSelectionScreen(
    categoryId: String,
    navController: NavController,
    onBrandSelected: (String) -> Unit,
    viewModel: AddMainlineViewModel = hiltViewModel(),
    carType: String = "mainline" // "mainline" or "premium"
) {
    val coroutineScope = rememberCoroutineScope()
    val categoryName = remember(categoryId) {
        when (categoryId) {
            "rally" -> "Rally"
            "hot_roads" -> "Hot Rods"
            "convertibles" -> "Convertibles"
            "vans" -> "Vans"
            "supercars" -> "Supercars"
            "american_muscle" -> "American Muscle"
            "motorcycle" -> "Motorcycle"
            "suv_trucks" -> "SUV & Pickups"
            else -> categoryId.replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        }
    }

    val brands = remember(categoryId, carType) {
        // Brand data based on category
        if (carType == "premium") {
            // Premium subcategories - ONLY correct Premium categories
            when (categoryId) {
                "car_culture" -> listOf(
                    Subcategory("modern_classic", "Modern Classic", Icons.Default.DirectionsCar),
                    Subcategory("race_day", "Race Day", Icons.Default.DirectionsCar),
                    Subcategory("circuit_legends", "Circuit Legends", Icons.Default.DirectionsCar),
                    Subcategory("team_transport", "Team Transport", Icons.Default.DirectionsCar),
                    Subcategory("silhouettes", "Silhouettes", Icons.Default.DirectionsCar),
                    Subcategory("jay_leno_garage", "Jay Leno Garage", Icons.Default.DirectionsCar),
                    Subcategory("rtr_vehicles", "RTR Vehicles", Icons.Default.DirectionsCar),
                    Subcategory("real_riders", "Real Riders", Icons.Default.DirectionsCar),
                    Subcategory("fast_wagons", "Fast Wagons", Icons.Default.DirectionsCar),
                    Subcategory("speed_machine", "Speed Machine", Icons.Default.DirectionsCar),
                    Subcategory("japan_historics", "Japan Historics", Icons.Default.DirectionsCar),
                    Subcategory("hammer_drop", "Hammer Drop", Icons.Default.DirectionsCar),
                    Subcategory("slide_street", "Slide Street", Icons.Default.DirectionsCar),
                    Subcategory("terra_trek", "Terra Trek", Icons.Default.DirectionsCar),
                    Subcategory("exotic_envy", "Exotic Envy", Icons.Default.DirectionsCar),
                    Subcategory("cargo_containers", "Cargo Containers", Icons.Default.DirectionsCar)
                )
                "pop_culture" -> listOf(
                    Subcategory("fast_and_furious", "Fast and Furious", Icons.Default.Speed),
                    Subcategory("mario_kart", "Mario Kart", Icons.Default.Games),
                    Subcategory("forza", "Forza", Icons.Default.Speed),
                    Subcategory("gran_turismo", "Gran Turismo", Icons.Default.Speed),
                    Subcategory("top_gun", "Top Gun", Icons.Default.Flight),
                    Subcategory("batman", "Batman", Icons.Default.Star),
                    Subcategory("star_wars", "Star Wars", Icons.Default.Star),
                    Subcategory("marvel", "Marvel", Icons.Default.Star),
                    Subcategory("jurassic_world", "Jurassic World", Icons.Default.Star),
                    Subcategory("back_to_the_future", "Back to the Future", Icons.Default.Star),
                    Subcategory("looney_tunes", "Looney Tunes", Icons.Default.Star)
                )
                // Other Premium categories have no subcategories
                "boulevard", "f1", "rlc", "1:43_scale", "others_premium" -> emptyList()
                else -> emptyList()
            }
        } else {
            // Mainline brands
            when (categoryId) {
                "convertibles" -> listOf(
                    Brand("porsche", "Porsche", Icons.Default.DirectionsCar),
                    Brand("ferrari", "Ferrari", Icons.Default.DirectionsCar),
                    Brand("lamborghini", "Lamborghini", Icons.Default.DirectionsCar),
                    Brand("mclaren", "McLaren", Icons.Default.DirectionsCar),
                    Brand("aston_martin", "Aston Martin", Icons.Default.DirectionsCar)
                )
                "rally" -> listOf(
                    Brand("subaru", "Subaru", Icons.Default.Speed),
                    Brand("mitsubishi", "Mitsubishi", Icons.Default.Speed),
                    Brand("ford", "Ford", Icons.Default.Speed),
                    Brand("toyota", "Toyota", Icons.Default.Speed)
                )
                "supercars" -> listOf(
                    Brand("bugatti", "Bugatti", Icons.Default.ElectricCar),
                    Brand("koenigsegg", "Koenigsegg", Icons.Default.ElectricCar),
                    Brand("pagani", "Pagani", Icons.Default.ElectricCar),
                    Brand("rimac", "Rimac", Icons.Default.ElectricCar)
                )
                "american_muscle" -> listOf(
                    Brand("ford", "Ford", Icons.Default.Speed),
                    Brand("chevrolet", "Chevrolet", Icons.Default.Speed),
                    Brand("dodge", "Dodge", Icons.Default.Speed),
                    Brand("pontiac", "Pontiac", Icons.Default.Speed)
                )
                else -> listOf(
                    Brand("generic", "Generic", Icons.Default.DirectionsCar)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    text = "Select Brand",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (carType == "premium") "Select the subcategory for your $categoryName car" else "Select the brand for your $categoryName car",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (carType == "premium") "Choose the Premium subcategory" else "Choose the manufacturer of the car",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Brands List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(brands) { item ->
                BrandCard(
                    brand = item,
                    onClick = { 
                        // Set category/brand and auto-save using new API
                        viewModel.updateSeries(categoryName)
                        val itemName = when (item) {
                            is Brand -> item.name
                            is Subcategory -> item.name
                            else -> "Unknown"
                        }
                        
                        if (carType == "premium") {
                            // For Premium: Series="Premium", Brand="", Category=main category, Name="", Subcategory=selected
                            viewModel.updateSeries("Premium")
                            viewModel.updateBrand("") // Brand is empty for Premium
                            viewModel.updateName("") // Name/Model is empty and editable
                            // Subcategory is stored separately (itemName = "Modern Classic", "Fast and Furious", etc.)
                            coroutineScope.launch {
                                viewModel.saveCar()
                            }
                        } else {
                            // For Mainline, use brand name
                            viewModel.updateBrand(itemName)
                            viewModel.updateName("$categoryName $itemName")
                            coroutineScope.launch {
                                viewModel.saveCar()
                            }
                        }
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BrandCard(
    brand: Any,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (brand) {
                    is Brand -> brand.icon
                    is Subcategory -> brand.icon
                    else -> Icons.Default.DirectionsCar
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = when (brand) {
                    is Brand -> brand.name
                    is Subcategory -> brand.name
                    else -> "Unknown"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
