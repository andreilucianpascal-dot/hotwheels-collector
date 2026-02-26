package com.example.hotwheelscollectors.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.hotwheelscollectors.ui.screens.auth.ForgotPasswordScreen
import com.example.hotwheelscollectors.ui.screens.auth.LoginScreen
import com.example.hotwheelscollectors.ui.screens.auth.RegisterScreen
import com.example.hotwheelscollectors.ui.screens.auth.WelcomeScreen
import com.example.hotwheelscollectors.ui.screens.add.AddMainlineScreen
import com.example.hotwheelscollectors.ui.screens.add.AddOthersScreen
import com.example.hotwheelscollectors.ui.screens.add.AddPremiumScreen
import com.example.hotwheelscollectors.ui.screens.add.AddSilverSeriesScreen
import com.example.hotwheelscollectors.ui.screens.add.AddTreasureHuntScreen
import com.example.hotwheelscollectors.ui.screens.add.AddSuperTreasureHuntScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowseMainlinesScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowsePremiumScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowseSilverSeriesScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowseTreasureHuntScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowseSuperTreasureHuntScreen
import com.example.hotwheelscollectors.ui.screens.browse.BrowseOthersScreen
import com.example.hotwheelscollectors.ui.screens.camera.BarcodeScannerScreen
import com.example.hotwheelscollectors.ui.screens.camera.CameraCaptureScreen
import com.example.hotwheelscollectors.ui.screens.camera.TakePhotosScreen
import com.example.hotwheelscollectors.ui.screens.camera.UploadPhotosScreen
// 🔑 FIX: Correct import paths
import com.example.hotwheelscollectors.ui.screens.collection.CollectionScreen
import com.example.hotwheelscollectors.ui.screens.collection.MainlinesScreen
import com.example.hotwheelscollectors.ui.screens.collection.MainlineBrandsScreen
import com.example.hotwheelscollectors.ui.screens.collection.OthersScreen
import com.example.hotwheelscollectors.ui.screens.collection.PremiumScreen
import com.example.hotwheelscollectors.ui.screens.collection.SilverSeriesScreen
import com.example.hotwheelscollectors.ui.screens.collection.SilverSeriesCarsScreen
import com.example.hotwheelscollectors.ui.screens.collection.PremiumCategoriesScreen
import com.example.hotwheelscollectors.ui.screens.collection.PremiumSubcategoriesScreen
import com.example.hotwheelscollectors.ui.screens.collection.PremiumCarsScreen
import com.example.hotwheelscollectors.ui.screens.collection.BrandSeriesScreen
import com.example.hotwheelscollectors.ui.screens.edit.EditCarDetailsScreen
import com.example.hotwheelscollectors.ui.screens.selection.CategorySelectionScreen
import com.example.hotwheelscollectors.ui.screens.selection.BrandSelectionScreen
import com.example.hotwheelscollectors.ui.screens.details.CarDetailsScreen
import com.example.hotwheelscollectors.ui.screens.main.MainScreen
import com.example.hotwheelscollectors.ui.screens.search.SearchDatabaseScreen
import com.example.hotwheelscollectors.ui.screens.selection.CarSelectionScreen
import com.example.hotwheelscollectors.ui.screens.details.CarNotFoundOptionScreen
import com.example.hotwheelscollectors.ui.screens.selection.PremiumSubseriesSelectionScreen
import com.example.hotwheelscollectors.ui.screens.settings.SettingsScreen
import com.example.hotwheelscollectors.ui.screens.settings.StorageSettingsScreen
import com.example.hotwheelscollectors.ui.screens.settings.ThemeSettingsScreen
import com.example.hotwheelscollectors.ui.screens.settings.LanguageSettingsScreen
import com.example.hotwheelscollectors.ui.screens.about.WhatsNewScreen
import com.example.hotwheelscollectors.ui.screens.about.AboutScreen
import com.example.hotwheelscollectors.ui.screens.privacy.PrivacyScreen
import com.example.hotwheelscollectors.ui.screens.terms.TermsScreen
import com.example.hotwheelscollectors.ui.screens.debug.DatabaseCleanupScreen
import androidx.compose.material3.Text

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // Auth
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }

        // Main
        composable("main") { MainScreen(navController) }
        composable("collection") { CollectionScreen(navController) }
        composable("mainline") { MainlinesScreen(navController) }
        composable("premium") { PremiumScreen(navController) }
        composable("silver_series") { SilverSeriesScreen(navController) }
        composable(
            route = "silver_series_cars/{subseriesId}",
            arguments = listOf(navArgument("subseriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val subseriesId = backStackEntry.arguments?.getString("subseriesId") ?: ""
            SilverSeriesCarsScreen(navController = navController, subseriesId = subseriesId)
        }
        composable("others") { OthersScreen(navController) }
        
        // Premium navigation
        composable(
            route = "premium_subcategories/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            PremiumSubcategoriesScreen(navController = navController, categoryId = categoryId)
        }
        
        composable(
            route = "premium_cars/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            PremiumCarsScreen(navController = navController, categoryId = categoryId, subcategoryId = null)
        }
        
        composable(
            route = "premium_cars/{categoryId}/{subcategoryId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("subcategoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subcategoryId = backStackEntry.arguments?.getString("subcategoryId")
            PremiumCarsScreen(navController = navController, categoryId = categoryId, subcategoryId = subcategoryId)
        }

        // Add
        composable("add_mainline") { AddMainlineScreen(navController) }
        composable("add_premium") { AddPremiumScreen(navController) }
        composable(
            route = "add_premium/{series}",
            arguments = listOf(navArgument("series") { type = NavType.StringType })
        ) { backStackEntry ->
            AddPremiumScreen(navController)
        }
        composable("add_silver_series") { AddSilverSeriesScreen(navController) }
        composable("add_others") { AddOthersScreen(navController) }
        composable("add_treasure_hunt") { AddTreasureHuntScreen(navController) }
        composable("add_super_treasure_hunt") { AddSuperTreasureHuntScreen(navController) }

        // Edit car details
        composable(
            route = "edit_car/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.StringType })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: ""
            EditCarDetailsScreen(
                carId = carId,
                navController = navController
            )
        }

        // Category selection (after photos)
        composable(
            route = "category_selection/{carType}",
            arguments = listOf(navArgument("carType") { type = NavType.StringType })
        ) { backStackEntry ->
            val carType = backStackEntry.arguments?.getString("carType") ?: "mainline"
            CategorySelectionScreen(
                navController = navController,
                onCategorySelected = { categoryId ->
                    navController.navigate("brand_selection/$categoryId/$carType")
                },
                carType = carType
            )
        }

        // Brand selection (after category)
        composable(
            route = "brand_selection/{categoryId}/{carType}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("carType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val carType = backStackEntry.arguments?.getString("carType") ?: "mainline"
            BrandSelectionScreen(
                categoryId = categoryId,
                navController = navController,
                onBrandSelected = { }, // Not used anymore
                carType = carType
            )
        }

        // Browse Global Database
        composable("browse_mainlines") { BrowseMainlinesScreen(navController) }
        composable("browse_premium") { BrowsePremiumScreen(navController) }
        composable("browse_silver_series") { BrowseSilverSeriesScreen(navController) }
        composable("browse_treasure_hunt") { BrowseTreasureHuntScreen(navController) }
        composable("browse_super_treasure_hunt") { BrowseSuperTreasureHuntScreen(navController) }
        composable("browse_others") { BrowseOthersScreen(navController) }

        // Camera
        composable("camera_capture") { CameraCaptureScreen(navController) }
        composable(
            route = "take_photos/{returnRoute}",
            arguments = listOf(navArgument("returnRoute") { type = NavType.StringType })
        ) { backStackEntry ->
            val returnRoute = backStackEntry.arguments?.getString("returnRoute") ?: ""
            val brandId = backStackEntry.arguments?.getString("brandId")
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            
            TakePhotosScreen(
                returnRoute = returnRoute,
                brandId = brandId,
                categoryId = categoryId,
                onPhotosComplete = { frontUri, backUri, barcode, croppedBarcodeUri, folderPath ->
                    // Data handling is now managed by TakePhotosScreen itself
                    // NavGraph only provides routing
                },
                onDismiss = { navController.navigateUp() },
                navController = navController
            )
        }

        composable(
            route = "upload_photos/{returnRoute}",
            arguments = listOf(navArgument("returnRoute") { type = NavType.StringType })
        ) {
            UploadPhotosScreen(
                onPhotosUploaded = { uris ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("photo_uris", uris.map { it.toString() })
                    navController.navigateUp()
                },
                onDismiss = { navController.navigateUp() }
            )
        }

        composable("barcode_scanner") {
            BarcodeScannerScreen(
                navController = navController,
                onBarcodeScanned = { barcode ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("barcode_result", barcode)
                    navController.navigateUp()
                },
                onDismiss = { navController.navigateUp() }
            )
        }

        // Selection
        composable("car_selection") { CarSelectionScreen(navController) }
        composable("car_not_found_options") { CarNotFoundOptionScreen(navController) }
        composable("premium_subseries_selection") {
            PremiumSubseriesSelectionScreen(navController = navController)
        }

        // Details
        composable(
            route = "car_details/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.StringType })
        ) { backStackEntry ->
            CarDetailsScreen(
                carId = backStackEntry.arguments?.getString("carId") ?: "",
                navController = navController
            )
        }
        
        // Full Photo View (full immersive)
        composable(
            route = "full_photo_view/{carId}/{photoUri}",
            arguments = listOf(
                navArgument("carId") { type = NavType.StringType },
                navArgument("photoUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            com.example.hotwheelscollectors.ui.screens.details.FullPhotoViewScreen(
                photoUri = backStackEntry.arguments?.getString("photoUri") ?: "",
                carId = backStackEntry.arguments?.getString("carId") ?: "",
                navController = navController
            )
        }

        // Search
        composable("search") { SearchDatabaseScreen(navController) }

        // Settings
        composable("settings") { SettingsScreen(onNavigateBack = { navController.navigateUp() }, navController = navController) }
        composable("storage_settings") { StorageSettingsScreen(navController, onGoogleSignIn = {}) }
        composable("theme_settings") { ThemeSettingsScreen(navController) }
        composable("language_settings") { 
            LanguageSettingsScreen(navController)
        }
        composable("whats_new") { 
            WhatsNewScreen(navController)
        }

        // Info
        composable("about") { AboutScreen(navController) }
        composable("privacy") { PrivacyScreen(navController) }
        composable("terms") { TermsScreen(navController) }

        // Duplicate route removed - using only "take_photos/{returnRoute}" below

        // Additional routes for navigation
        composable(
            route = "mainline_category/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            BrandSeriesScreen(
                navController = navController,
                brandId = "",
                seriesId = category
            )
        }

        composable(
            route = "mainline_brands/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            MainlineBrandsScreen(
                navController = navController,
                categoryId = categoryId
            )
        }

        composable(
            route = "collection_mainline_brands/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            MainlineBrandsScreen(
                navController = navController,
                categoryId = categoryId
            )
        }

        composable(
            route = "brand_cars/{categoryId}/{brandName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("brandName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val brandName = backStackEntry.arguments?.getString("brandName") ?: ""
            com.example.hotwheelscollectors.ui.screens.collection.BrandCarsScreen(
                categoryId = categoryId,
                brandName = brandName,
                navController = navController
            )
        }

        composable(
            route = "collection_premium_series/{seriesId}",
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            BrandSeriesScreen(
                navController = navController,
                brandId = "",
                seriesId = seriesId
            )
        }

        composable(
            route = "mainline_brand/{brand}",
            arguments = listOf(navArgument("brand") { type = NavType.StringType })
        ) { backStackEntry ->
            val brand = backStackEntry.arguments?.getString("brand") ?: ""
            BrandSeriesScreen(
                navController = navController,
                brandId = brand
            )
        }

        composable(
            route = "premium_series/{series}",
            arguments = listOf(navArgument("series") { type = NavType.StringType })
        ) { backStackEntry ->
            val series = backStackEntry.arguments?.getString("series") ?: ""
            BrandSeriesScreen(
                navController = navController,
                brandId = "",
                seriesId = series
            )
        }

        composable(
            route = "mainline_cars/{categoryId}/{brandId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("brandId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val brandId = backStackEntry.arguments?.getString("brandId") ?: ""
            BrandSeriesScreen(
                navController = navController,
                brandId = brandId,
                seriesId = categoryId
            )
        }

        // Debug screens
        composable("database_cleanup") {
            DatabaseCleanupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}