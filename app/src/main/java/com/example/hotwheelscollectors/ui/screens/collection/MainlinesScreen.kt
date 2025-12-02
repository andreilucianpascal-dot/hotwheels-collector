package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.viewmodels.BrandSeriesViewModel

// Data class for category items
data class MainlineCategory(
    val id: String,
    val title: String,
    val brands: List<String>,
    val fontFamily: FontFamily,
    val backgroundColor: Color,
    val textColor: Color,
    val route: String,
)

@Composable
fun MainlinesScreen(
    navController: NavController,
    viewModel: BrandSeriesViewModel = hiltViewModel(),
) {
    // Create categories with display-only titles; avoid invalid mapNotNull chains at init time
    val categories = remember {
        listOf(
            MainlineCategory(
                id = "rally",
                title = "Rally",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.racingsansone_regular)),
                backgroundColor = Color.Black,
                textColor = Color.Red,
                route = "mainline_brands/rally"
            ),
            MainlineCategory(
                id = "hot_roads",
                title = "Hot Rods",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.lobster)),
                backgroundColor = Color(0xFFFF9800),
                textColor = Color.Black,
                route = "mainline_brands/hot_roads"
            ),
            MainlineCategory(
                id = "convertibles",
                title = "Convertibles",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.greatvibes_regular)),
                backgroundColor = Color.White,
                textColor = Color.Red,
                route = "mainline_brands/convertibles"
            ),
            MainlineCategory(
                id = "vans",
                title = "Vans",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.permanentmarker)),
                backgroundColor = Color.Blue,
                textColor = Color.White,
                route = "mainline_brands/vans"
            ),
            MainlineCategory(
                id = "supercars",
                title = "Supercars",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.special_speed_agent)),
                backgroundColor = Color.White,
                textColor = Color.Black,
                route = "mainline_brands/supercars"
            ),
            MainlineCategory(
                id = "american_muscle",
                title = "American Muscle",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.retrofunk_script_personal_use)),
                backgroundColor = Color(0xFFD2691E),
                textColor = Color(0xFFFFFDD0),
                route = "mainline_brands/american_muscle"
            ),
            MainlineCategory(
                id = "motorcycle",
                title = "Motorcycle",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.motor_personal_use_only)),
                backgroundColor = Color(0xFFFF9800),
                textColor = Color.Red,
                route = "mainline_brands/motorcycle"
            ),
            MainlineCategory(
                id = "suv_trucks",
                title = "SUV & Pickups",
                brands = emptyList(),
                fontFamily = FontFamily(Font(R.font.clayborn)),
                backgroundColor = Color(0xFF8B4513),
                textColor = Color.Black,
                route = "mainline_brands/suv_trucks"
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Mainlines",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Using LazyColumn instead of scrollable Column for better scroll observation handling
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                MainlineCategoryButton(
                    category = category,
                    onClick = { navController.navigate(category.route) }
                )
            }
        }
    }
}

@Composable
private fun MainlineCategoryButton(
    category: MainlineCategory,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = category.backgroundColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        // Only show the category title - NO brand names
        Text(
            text = category.title,
            fontFamily = category.fontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = category.textColor,
            textAlign = TextAlign.Center
        )
    }
}