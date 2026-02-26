package com.example.hotwheelscollectors.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager
import com.example.hotwheelscollectors.viewmodels.AppThemeViewModel
import com.example.hotwheelscollectors.viewmodels.ThemeSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.hotwheelscollectors.viewmodels.MainViewModel
import com.example.hotwheelscollectors.viewmodels.MainlineViewModel
import android.net.Uri

@Composable
fun MainScreen(
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()
    // ✅ OPTIMIZATION: Remove MainlineViewModel from MainScreen - lazy load only when user navigates to Mainline
    // mainlineViewModel: MainlineViewModel = hiltViewModel() // Removed - causes slow startup
) {
    // Monitor savedStateHandle for data from TakePhotosScreen
    val savedStateHandle = navController.getBackStackEntry("main").savedStateHandle
    val frontPhotoUri = savedStateHandle?.get<String>("front_photo_uri")
    val backPhotoUri = savedStateHandle?.get<String>("back_photo_uri")
    val barcodeResult = savedStateHandle?.get<String>("barcode_result")
    val folderPath = savedStateHandle?.get<String>("folder_path")
    val brandName = savedStateHandle?.get<String>("brand_name")
    val carType = savedStateHandle?.get<String>("car_type") // To determine if it's Mainline or Premium
    
    // Debug logging
    android.util.Log.d("MainScreen", "=== MONITORING SAVEDSTATEHANDLE ===")
    android.util.Log.d("MainScreen", "savedStateHandle: $savedStateHandle")
    android.util.Log.d("MainScreen", "frontPhotoUri: $frontPhotoUri")
    android.util.Log.d("MainScreen", "backPhotoUri: $backPhotoUri")
    android.util.Log.d("MainScreen", "barcodeResult: $barcodeResult")
    android.util.Log.d("MainScreen", "folderPath: $folderPath")
    android.util.Log.d("MainScreen", "brandName: $brandName")
    android.util.Log.d("MainScreen", "carType: $carType")
    
    // ✅ Monitor UI events from MainViewModel (e.g., Drive login required)
    LaunchedEffect(Unit) {
        mainViewModel.uiEvents.collect { event ->
            when (event) {
                is MainViewModel.UiEvent.RequireDriveLogin -> {
                    android.util.Log.d("MainScreen", "Drive login required - navigating to Settings")
                    navController.navigate("settings") {
                        // Don't pop main screen - user can come back after login
                        popUpTo("main") { inclusive = false }
                    }
                }
            }
        }
    }
    
    // ✅ FIX: Use rememberSaveable flag to prevent re-execution when returning from other screens
    var hasProcessedData by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    
    // Process data from TakePhotosScreen when available
    LaunchedEffect(frontPhotoUri, backPhotoUri, carType) {
        android.util.Log.d("MainScreen", "=== LAUNCHEDEFFECT TRIGGERED ===")
        android.util.Log.d("MainScreen", "frontPhotoUri: $frontPhotoUri")
        android.util.Log.d("MainScreen", "backPhotoUri: $backPhotoUri")
        android.util.Log.d("MainScreen", "carType: $carType")
        android.util.Log.d("MainScreen", "hasProcessedData: $hasProcessedData")
        android.util.Log.d("MainScreen", "Condition check: frontPhotoUri != null && carType != null && !hasProcessedData")
        android.util.Log.d("MainScreen", "frontPhotoUri != null: ${frontPhotoUri != null}")
        android.util.Log.d("MainScreen", "carType != null: ${carType != null}")
        android.util.Log.d("MainScreen", "!hasProcessedData: ${!hasProcessedData}")
        
        if (frontPhotoUri != null && carType != null && !hasProcessedData) {
            hasProcessedData = true // ✅ Mark as processed to prevent re-execution
            android.util.Log.d("MainScreen", "=== PROCESSING DATA FROM TAKEPHOTOSSCREEN ===")
            android.util.Log.d("MainScreen", "frontPhotoUri: $frontPhotoUri")
            android.util.Log.d("MainScreen", "backPhotoUri: $backPhotoUri")
            android.util.Log.d("MainScreen", "barcodeResult: $barcodeResult")
            android.util.Log.d("MainScreen", "folderPath: $folderPath")
            android.util.Log.d("MainScreen", "brandName: $brandName")
            android.util.Log.d("MainScreen", "carType: $carType")
            
            try {
                when (carType) {
                    "mainline" -> {
                        // Navigate to AddMainlineScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Mainline flow - navigating to AddMainlineScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        android.util.Log.d("MainScreen", "Passing folderPath: $folderPath")
                        android.util.Log.d("MainScreen", "Passing brandName: $brandName")
                        
                        // Pass data to AddMainlineScreen by NOT clearing SavedStateHandle
                        // Data will remain in SavedStateHandle for AddMainlineScreen to read
                        navController.navigate("add_mainline")
                    }
                    
                    "premium" -> {
                        // Navigate to AddPremiumScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Premium flow - navigating to AddPremiumScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        android.util.Log.d("MainScreen", "Passing folderPath: $folderPath")
                        android.util.Log.d("MainScreen", "Passing subcategoryName: $brandName")
                        
                        // Pass data to AddPremiumScreen through SavedStateHandle
                        navController.navigate("add_premium") {
                            // Data will be available in AddPremiumScreen's SavedStateHandle
                        }
                    }
                    
                    "silver_series" -> {
                        // Navigate to AddSilverSeriesScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Silver Series flow - navigating to AddSilverSeriesScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        android.util.Log.d("MainScreen", "Passing folderPath: $folderPath")
                        android.util.Log.d("MainScreen", "Passing subcategoryName: $brandName")
                        
                        // Pass data to AddSilverSeriesScreen through SavedStateHandle
                        navController.navigate("add_silver_series") {
                            // Data will be available in AddSilverSeriesScreen's SavedStateHandle
                        }
                    }
                    
                    "treasure_hunt" -> {
                        // Navigate to AddTreasureHuntScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Treasure Hunt flow - navigating to AddTreasureHuntScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        
                        // Pass data to AddTreasureHuntScreen through SavedStateHandle
                        navController.navigate("add_treasure_hunt") {
                            // Data will be available in AddTreasureHuntScreen's SavedStateHandle
                        }
                    }
                    
                    "super_treasure_hunt" -> {
                        // Navigate to AddSuperTreasureHuntScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Super Treasure Hunt flow - navigating to AddSuperTreasureHuntScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        
                        // Pass data to AddSuperTreasureHuntScreen through SavedStateHandle
                        navController.navigate("add_super_treasure_hunt") {
                            // Data will be available in AddSuperTreasureHuntScreen's SavedStateHandle
                        }
                    }
                    
                    "others" -> {
                        // Navigate to AddOthersScreen and pass data through SavedStateHandle
                        android.util.Log.d("MainScreen", "Others flow - navigating to AddOthersScreen with data")
                        android.util.Log.d("MainScreen", "Passing frontPhotoUri: $frontPhotoUri")
                        android.util.Log.d("MainScreen", "Passing backPhotoUri: $backPhotoUri")
                        android.util.Log.d("MainScreen", "Passing barcodeResult: $barcodeResult")
                        
                        // Pass data to AddOthersScreen through SavedStateHandle
                        navController.navigate("add_others") {
                            // Data will be available in AddOthersScreen's SavedStateHandle
                        }
                    }
                }
                
                // Don't clear saved state here - AddMainlineScreen needs to read it
                // Saved state will be cleared when AddMainlineScreen navigates back after saving
                android.util.Log.d("MainScreen", "Navigation completed - AddScreen will handle saving and clearing")
                
                // ✅ Reset flag after navigation so it's ready for next car
                hasProcessedData = false
                
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "Error saving car: ${e.message}")
                hasProcessedData = false // ✅ Reset on error too
            }
        }
    }
    
    // Background themed using current color scheme
    val appThemeViewModel: AppThemeViewModel = hiltViewModel()
    val themeState by appThemeViewModel.uiState.collectAsState()
    val bgTheme = HotWheelsThemeManager.getBackgroundTheme(themeState.colorScheme)

    // Main screen button colors + font from ThemeSettings
    val themeSettingsViewModel: ThemeSettingsViewModel = hiltViewModel()
    val themeSettingsState by themeSettingsViewModel.uiState.collectAsState()

    val fontFamily = when (themeSettingsState.mainScreenFontFamily) {
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "clayborn" -> FontFamily(Font(R.font.clayborn))
        "lobster" -> FontFamily(Font(R.font.lobster))
        "greatvibes" -> FontFamily(Font(R.font.greatvibes_regular))
        "permanentmarker" -> FontFamily(Font(R.font.permanentmarker))
        "racingsansone" -> FontFamily(Font(R.font.racingsansone_regular))
        "specialspeed" -> FontFamily(Font(R.font.special_speed_agent))
        "motor" -> FontFamily(Font(R.font.motor_personal_use_only))
        "retrofunk" -> FontFamily(Font(R.font.retrofunk_script_personal_use))
        else -> FontFamily.Default
    }

    // Override typography only for MainScreen, keeping global colors
    val baseTypography = MaterialTheme.typography
    val mainTypography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = fontFamily),
    )

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = mainTypography
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = bgTheme?.primaryGradient
                        ?: androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                )
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
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
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
                // Welcome Section with Hot Wheels colors (Red, White, Blue)
                val user = FirebaseAuth.getInstance().currentUser
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE60012) // Hot Wheels Red
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Hot Wheels logo colors gradient effect
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF0066CC) // Hot Wheels Blue
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (user != null) {
                            val name = user.displayName
                            val email = user.email
                            if (!name.isNullOrEmpty()) {
                                Text(
                                    text = "Welcome, $name!",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (!email.isNullOrEmpty()) {
                                Text(
                                    text = "Welcome, $email!",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Welcome to Hot Wheels Collector",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Welcome to Hot Wheels Collector",
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage your die-cast collection with ease",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            item {
                // Add Car Actions Section
                Text(
                    text = "Add Cars",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                // First row - Add Mainline and Add Premium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val mainlineBg = if (themeSettingsState.mainlineButtonColor != 0)
                        Color(themeSettingsState.mainlineButtonColor)
                    else
                        Color(0xFF87CEEB)

                    val premiumBg = if (themeSettingsState.premiumButtonColor != 0)
                        Color(themeSettingsState.premiumButtonColor)
                    else
                        Color.Black

                    // Add Mainline - customizable background, White text
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("take_photos/add_mainline") },
                        colors = CardDefaults.cardColors(
                            containerColor = mainlineBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add Mainline",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Add Premium - customizable background, Gold text
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("take_photos/add_premium") },
                        colors = CardDefaults.cardColors(
                            containerColor = premiumBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFFFD700) // Gold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add Premium",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                textAlign = TextAlign.Center,
                                color = Color(0xFFFFD700), // Gold - matching My Collection
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                // Add Silver Series - Silver background, Black text
                val silverBg = if (themeSettingsState.silverButtonColor != 0)
                    Color(themeSettingsState.silverButtonColor)
                else
                    Color(0xFFC0C0C0)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("take_photos/add_silver_series") },
                    colors = CardDefaults.cardColors(
                        containerColor = silverBg
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Silver Series",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                color = Color.Black, // Black text - matching My Collection
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                // Second row - Add Treasure Hunt and Add Super Treasure Hunt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val thBg = if (themeSettingsState.treasureHuntButtonColor != 0)
                        Color(themeSettingsState.treasureHuntButtonColor)
                    else
                        Color.White

                    val sthBg = if (themeSettingsState.superTreasureHuntButtonColor != 0)
                        Color(themeSettingsState.superTreasureHuntButtonColor)
                    else
                        Color.White

                    // Add Treasure Hunt - White background, Gray text
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("take_photos/add_treasure_hunt") },
                        colors = CardDefaults.cardColors(
                            containerColor = thBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Add Super Treasure Hunt - White background, Gold text
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate("take_photos/add_super_treasure_hunt") },
                        colors = CardDefaults.cardColors(
                            containerColor = sthBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFFFD700) // Gold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Add Super Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                textAlign = TextAlign.Center,
                                color = Color(0xFFFFD700), // Gold
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                // Add Others button
                val othersBg = if (themeSettingsState.othersButtonColor != 0)
                    Color(themeSettingsState.othersButtonColor)
                else
                    Color.White

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("take_photos/add_others") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = othersBg
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Others",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                fontWeight = FontWeight.Bold
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Collection Management Section  
                Text(
                    text = "Collection Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                // View Collection (single button)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("collection") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "View Collection",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Browse your organized collection",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Browse Categories Section
                Text(
                    text = "Browse Categories",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                // Scan Barcode - Quick tool for discovery
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("barcode_scanner") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Barcode",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Check if car exists in global database",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Mainlines
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_mainlines") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF87CEEB) // Light blue
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Mainlines",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Global database - Standard Hot Wheels cars",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Premium Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_premium") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFFFFD700) // Gold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Premium",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Silver Series Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_silver_series") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF808080) // Silver
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Silver Series",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Global database - Silver Series cars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Treasure Hunt Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_treasure_hunt") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Super Treasure Hunt Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_super_treasure_hunt") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFFFFD700) // Gold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Super Treasure Hunt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Others Cars
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("browse_others") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Browse Others",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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

        // End of Column
        }
    }
}