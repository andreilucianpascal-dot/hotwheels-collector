package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel

@Composable
fun PremiumCategoriesScreen(
    navController: NavController,
    cars: List<CarEntity>
) {
    val premiumCategories = listOf(
        PremiumCategory("Car Culture", "car_culture", Color(0xFF1976D2), Color.White),
        PremiumCategory("Pop Culture", "pop_culture", Color(0xFFE91E63), Color.White),
        PremiumCategory("Boulevard", "boulevard", Color(0xFF424242), Color.White),
        PremiumCategory("F1", "f1", Color(0xFFD32F2F), Color.White),
        PremiumCategory("RLC", "rlc", Color(0xFF7B1FA2), Color.White),
        PremiumCategory("1:43 Scale", "large_scale", Color(0xFF388E3C), Color.White),
        PremiumCategory("Others Premium", "others_premium", Color(0xFF616161), Color.White)
    )

    // Group Premium cars by main category (Car Culture, Pop Culture, etc.)
    val carsByCategory = cars.filter { it.isPremium }.groupBy { car -> // ✅ Fixed: Use only isPremium field
        // Extract main category from subseries (e.g., "Pop Culture/Back to the Future" -> "Pop Culture")
        if (car.subseries.contains("/")) {
            car.subseries.split("/").firstOrNull() ?: car.subseries
        } else {
            car.subseries
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        premiumCategories.forEach { category ->
            val categoryCars = carsByCategory[category.displayName] ?: emptyList()
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable {
                            if (category.id == "boulevard" || category.id == "f1" || category.id == "rlc" || category.id == "large_scale") {
                                navController.navigate("premium_cars/${category.id}")
                            } else {
                                navController.navigate("premium_subcategories/${category.id}")
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = category.backgroundColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${categoryCars.size} cars",
                                fontSize = 14.sp,
                                color = category.textColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PremiumCategory(
    val displayName: String,
    val id: String,
    val backgroundColor: Color,
    val textColor: Color
)

