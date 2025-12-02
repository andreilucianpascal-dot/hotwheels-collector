package com.example.hotwheelscollectors.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.hotwheelscollectors.viewmodels.AddMainlineViewModel
import android.net.Uri

@Composable
fun MainScreen(
    navController: NavController,
    addMainlineViewModel: AddMainlineViewModel = hiltViewModel()
) {
    // Monitor savedStateHandle for data from TakePhotosScreen
    val savedStateHandle = navController.getBackStackEntry("main").savedStateHandle
    val frontPhotoUri = savedStateHandle?.get<String>("front_photo_uri")
    val backPhotoUri = savedStateHandle?.get<String>("back_photo_uri")
    val barcodeResult = savedStateHandle?.get<String>("barcode_result")
    val folderPath = savedStateHandle?.get<String>("folder_path")
    val brandName = savedStateHandle?.get<String>("brand_name")
    
    // Debug logging
    android.util.Log.d("MainScreen", "=== MONITORING SAVEDSTATEHANDLE ===")
    android.util.Log.d("MainScreen", "savedStateHandle: $savedStateHandle")
    android.util.Log.d("MainScreen", "frontPhotoUri: $frontPhotoUri")
    android.util.Log.d("MainScreen", "backPhotoUri: $backPhotoUri")
    android.util.Log.d("MainScreen", "barcodeResult: $barcodeResult")
    android.util.Log.d("MainScreen", "folderPath: $folderPath")
    android.util.Log.d("MainScreen", "brandName: $brandName")
    
    // Process data from TakePhotosScreen when available
    LaunchedEffect(frontPhotoUri, backPhotoUri) {
        if (frontPhotoUri != null && backPhotoUri != null) {
            android.util.Log.d("MainScreen", "=== PROCESSING DATA FROM TAKEPHOTOSSCREEN ===")
            android.util.Log.d("MainScreen", "frontPhotoUri: $frontPhotoUri")
            android.util.Log.d("MainScreen", "backPhotoUri: $backPhotoUri")
            android.util.Log.d("MainScreen", "barcodeResult: $barcodeResult")
            android.util.Log.d("MainScreen", "folderPath: $folderPath")
            android.util.Log.d("MainScreen", "brandName: $brandName")
            
            try {
                // Process photos into ViewModel (wait for completion)
                addMainlineViewModel.processSelectedPhotoSync(Uri.parse(frontPhotoUri))
                addMainlineViewModel.processSelectedPhotoSync(Uri.parse(backPhotoUri))
                
                // Set barcode if detected
                if (barcodeResult != null && barcodeResult.isNotEmpty()) {
                    addMainlineViewModel.updateBarcode(barcodeResult)
                }
                
                // Set category from folder path as subseries for Mainline cars
                if (folderPath != null && folderPath.isNotEmpty()) {
                    // Extract subcategory from folderPath (e.g., "Convertibles/Porsche" -> "Convertibles")
                    val subcategory = if (folderPath.contains("/")) {
                        folderPath.split("/").firstOrNull() ?: folderPath
                    } else {
                        folderPath
                    }
                    addMainlineViewModel.updateSubseries(subcategory)
                    android.util.Log.d("MainScreen", "Setting subseries to: '$subcategory' (from folderPath: '$folderPath')")
                }

                // Set brand name
                if (brandName != null && brandName.isNotEmpty()) {
                    addMainlineViewModel.updateBrand(brandName)
                }

                // Set car name (brand + category)
                if (brandName != null && brandName.isNotEmpty() && folderPath != null && folderPath.isNotEmpty()) {
                    val subcategory = if (folderPath.contains("/")) {
                        folderPath.split("/").firstOrNull() ?: folderPath
                    } else {
                        folderPath
                    }
                    val carName = "$brandName $subcategory"
                    addMainlineViewModel.updateName(carName)
                    android.util.Log.d("MainScreen", "Setting car name to: '$carName'")
                }

                // Auto-save the car with all collected data
                android.util.Log.d("MainScreen", "About to call saveCar()")
                addMainlineViewModel.saveCar()
                
                // Clear the saved state to prevent reprocessing
                savedStateHandle?.remove<String>("front_photo_uri")
                savedStateHandle?.remove<String>("back_photo_uri")
                savedStateHandle?.remove<String>("barcode_result")
                savedStateHandle?.remove<String>("folder_path")
                savedStateHandle?.remove<String>("brand_name")
                
                android.util.Log.d("MainScreen", "Car saved successfully!")
                
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "Error saving car: ${e.message}")
            }
        }
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
                Text(
                    text = "Hot Wheels Collector",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { navController.navigate("search") }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Main content using LazyColumn for better performance
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Welcome Section with user info (if available)
                val user = FirebaseAuth.getInstance().currentUser
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (user != null) {
                            val name = user.displayName
                            val email = user.email
                            if (!name.isNullOrEmpty()) {
                                Text(
                                    text = "Welcome, $name!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                            } else if (!email.isNullOrEmpty()) {
                                Text(
                                    text = "Welcome, $email!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Welcome to Hot Wheels Collector",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = "Welcome to Hot Wheels Collector",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "Manage your die-cast collection with ease",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                // Add Car Actions Section
                Text(
                    text = "Add Cars",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // First row - Add Mainline and Add Premium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Mainline
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("take_photos/add_mainline") },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF87CEEB) // Sky Blue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Mainline",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF87CEEB) // Sky Blue
                            )
                        }
                    }

                    // Add Premium
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("add_premium") },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFFFD700) // Gold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Premium",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFFFFD700) // Gold
                            )
                        }
                    }
                }
            }

            item {
                // Second row - Add Treasure Hunt and Add Super Treasure Hunt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Treasure Hunt (White with Gray text)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("add_treasure_hunt") },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }

                    // Add Super Treasure Hunt (White with Gold text)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("add_super_treasure_hunt") },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFFFD700) // Gold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Super Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFFFFD700) // Gold
                            )
                        }
                    }
                }
            }

            item {
                // Add Others button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("add_others") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Others",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Miscellaneous cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Collection Management Section  
                Text(
                    text = "Collection Management",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // View Collection (single button)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("collection") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "View Collection",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Browse your organized collection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Browse Categories Section
                Text(
                    text = "Browse Categories",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Scan Barcode - Quick tool for discovery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("barcode_scanner") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Barcode",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Check if car exists in global database",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Mainlines
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_mainlines") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Mainlines",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Global database - Standard Hot Wheels cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Premium Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_premium") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Premium",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Global database - Premium cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Treasure Hunt Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_treasure_hunt") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Global database - TH cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Super Treasure Hunt Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_super_treasure_hunt") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFFFD700) // Gold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Super Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Global database - STH cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                // Others Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_others") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Others",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Global database - Miscellaneous cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }


            item {
                // About Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("about") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About & Help",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "App information and user guide",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }
        }
    }
}