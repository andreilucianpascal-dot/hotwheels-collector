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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class Category(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val backgroundColor: Color,
    val textColor: Color
)

@Composable
fun CategorySelectionScreen(
    navController: NavController,
    onCategorySelected: (String) -> Unit,
    carType: String = "mainline" // "mainline" or "premium"
) {
    val categories = remember(carType) {
        if (carType == "premium") {
            // Premium categories - CORRECTED
            listOf(
                Category(
                    id = "car_culture",
                    name = "Car Culture",
                    icon = Icons.Default.DirectionsCar,
                    backgroundColor = Color(0xFF1976D2),
                    textColor = Color.White
                ),
                Category(
                    id = "pop_culture",
                    name = "Pop Culture",
                    icon = Icons.Default.Star,
                    backgroundColor = Color(0xFFE91E63),
                    textColor = Color.White
                ),
                Category(
                    id = "boulevard",
                    name = "Boulevard",
                    icon = Icons.Default.Star,
                    backgroundColor = Color(0xFF424242),
                    textColor = Color.White
                ),
                Category(
                    id = "f1",
                    name = "F1",
                    icon = Icons.Default.Speed,
                    backgroundColor = Color(0xFFD32F2F),
                    textColor = Color.White
                ),
                Category(
                    id = "rlc",
                    name = "RLC",
                    icon = Icons.Default.Diamond,
                    backgroundColor = Color(0xFF7B1FA2),
                    textColor = Color.White
                ),
                Category(
                    id = "1:43_scale",
                    name = "1:43 Scale",
                    icon = Icons.Default.Scale,
                    backgroundColor = Color(0xFF388E3C),
                    textColor = Color.White
                ),
                Category(
                    id = "others_premium",
                    name = "Others Premium",
                    icon = Icons.Default.Category,
                    backgroundColor = Color(0xFF616161),
                    textColor = Color.White
                )
            )
        } else {
            // Mainline categories
            listOf(
                Category(
                    id = "rally",
                    name = "Rally",
                    icon = Icons.Default.Speed,
                    backgroundColor = Color.Black,
                    textColor = Color.Red
                ),
                Category(
                    id = "hot_roads",
                    name = "Hot Rods",
                    icon = Icons.Default.LocalFireDepartment,
                    backgroundColor = Color(0xFFFF9800),
                    textColor = Color.Black
                ),
                Category(
                    id = "convertibles",
                    name = "Convertibles",
                    icon = Icons.Default.DirectionsCar,
                    backgroundColor = Color.White,
                    textColor = Color.Red
                ),
                Category(
                    id = "vans",
                    name = "Vans",
                    icon = Icons.Default.LocalShipping,
                    backgroundColor = Color.Blue,
                    textColor = Color.White
                ),
                Category(
                    id = "supercars",
                    name = "Supercars",
                    icon = Icons.Default.ElectricCar,
                    backgroundColor = Color.White,
                    textColor = Color.Black
                ),
                Category(
                    id = "american_muscle",
                    name = "American Muscle",
                    icon = Icons.Default.Speed,
                    backgroundColor = Color(0xFFD2691E),
                    textColor = Color(0xFFFFFDD0)
                ),
                Category(
                    id = "motorcycle",
                    name = "Motorcycle",
                    icon = Icons.Default.TwoWheeler,
                    backgroundColor = Color(0xFFFF9800),
                    textColor = Color.Red
                ),
                Category(
                    id = "suv_trucks",
                    name = "SUV & Pickups",
                    icon = Icons.Default.LocalShipping,
                    backgroundColor = Color(0xFF8B4513),
                    textColor = Color.Black
                )
            )
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
                    text = "Select Category",
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
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select the category for your car",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Choose the type of car you just photographed",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // Categories Grid
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = category.backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = category.textColor
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = category.textColor,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = category.textColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
