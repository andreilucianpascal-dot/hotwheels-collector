package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilverSeriesScreen(
    navController: NavController,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    data class SilverCategoryStyle(val name: String, val bg: Color, val text: Color)

    // Paletă vie, saturată, cu contrast bun text/icon
    val silverSeriesCategories = listOf(
        SilverCategoryStyle("Hybrid Speed", Color(0xFF5C6BC0), Color.White),            // Steel blue
        SilverCategoryStyle("Compact Kings", Color(0xFF26A69A), Color.White),           // Teal
        SilverCategoryStyle("National Icons", Color(0xFFFFB300), Color(0xFF3E2723)),   // Amber + dark text
        SilverCategoryStyle("Pantone Assortment", Color(0xFF8E24AA), Color.White),      // Purple
        SilverCategoryStyle("Vintage Club", Color(0xFF607D8B), Color.White),            // Blue Grey
        SilverCategoryStyle("Salt Flat Racers", Color(0xFF29B6F6), Color(0xFF0D47A1)),  // Vivid blue
        SilverCategoryStyle("80th Anniversary Vehicle Set", Color(0xFFFF7043), Color.White), // Orange
        SilverCategoryStyle("Fast & Furious", Color(0xFFE53935), Color.White),          // Red
        SilverCategoryStyle("Mustang 60 Years", Color(0xFF1565C0), Color.White)         // Navy
    )

    // Get Silver Series cars from database
    val localCars by viewModel.localCars.collectAsState()
    val silverSeriesCars = remember(localCars) {
        localCars.filter { it.series.equals("Silver Series", ignoreCase = true) }
    }
    
    // Calculate count for each category
    val categoryCounts = remember(silverSeriesCars) {
        silverSeriesCategories.map { categoryStyle ->
            val count = silverSeriesCars.count { car ->
                val subseries = car.subseries ?: ""
                subseries.contains(categoryStyle.name, ignoreCase = true)
            }
            categoryStyle.name to count
        }.toMap()
    }

    val themeState by com.example.hotwheelscollectors.viewmodels.AppThemeViewModel::class
        .let { androidx.hilt.navigation.compose.hiltViewModel<com.example.hotwheelscollectors.viewmodels.AppThemeViewModel>() }
        .uiState
        .collectAsState()
    val bgTheme = HotWheelsThemeManager.getBackgroundTheme(themeState.colorScheme)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Silver Series") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_silver_series") }
            ) {
                Icon(Icons.Default.Add, "Add silver series car")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = bgTheme?.secondaryGradient
                        ?: androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(silverSeriesCategories) { style ->
                    val count = categoryCounts[style.name] ?: 0
                    SilverSeriesCategoryCard(
                        category = style.name,
                        carCount = count,
                        bgColor = style.bg,
                        textColor = style.text,
                        onClick = {
                            // Navigate to Silver Series subseries screen
                            val subseriesId = style.name.lowercase().replace(" ", "_").replace("&", "and").replace("'", "")
                            navController.navigate("silver_series_cars/$subseriesId")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SilverSeriesCategoryCard(
    category: String,
    carCount: Int,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$carCount cars",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}
