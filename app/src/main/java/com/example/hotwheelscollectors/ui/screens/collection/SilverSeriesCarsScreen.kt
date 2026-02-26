package com.example.hotwheelscollectors.ui.screens.collection

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel
import java.io.File

@Composable
fun SilverSeriesCarsScreen(
    navController: NavController,
    subseriesId: String,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val localCars by viewModel.localCars.collectAsState()
    
    // Convert subseriesId back to display name
    val subseriesDisplayName = subseriesId.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char -> char.uppercaseChar() }
    }
    
    // Filter Silver Series cars by subseries
    val filteredCars = localCars.filter { car ->
        car.series.equals("Silver Series", ignoreCase = true) &&
        car.subseries?.contains(subseriesDisplayName, ignoreCase = true) == true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subseriesDisplayName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredCars.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No cars in this subseries yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCars) { car ->
                    SilverSeriesCarCard(
                        car = car,
                        navController = navController,
                        onClick = {
                            navController.navigate("car_details/${car.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SilverSeriesCarCard(
    car: com.example.hotwheelscollectors.data.local.entities.CarEntity,
    navController: NavController,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display thumbnail
            if (car.combinedPhotoPath.isNotBlank()) {
                val thumbnailFile = File(car.combinedPhotoPath)
                if (thumbnailFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailFile)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Car Thumbnail",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val photoUri = car.frontPhotoPath.ifEmpty { car.combinedPhotoPath }
                                navController.navigate("full_photo_view/${car.id}/${Uri.encode(photoUri)}")
                            }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = car.model.ifEmpty { "Unknown Model" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Brand: ${car.brand.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (car.subseries.isNotBlank()) {
                    Text(
                        text = "Category: ${car.subseries}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (car.year != 0) {
                    Text(
                        text = "Year: ${car.year}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (car.color.isNotBlank()) {
                    Text(
                        text = "Color: ${car.color}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

