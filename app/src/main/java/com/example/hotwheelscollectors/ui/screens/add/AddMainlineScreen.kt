package com.example.hotwheelscollectors.ui.screens.add

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.viewmodels.AddMainlineViewModel
import com.example.hotwheelscollectors.viewmodels.AddCarUiState

@Composable
fun AddMainlineScreen(
    navController: NavController,
    viewModel: AddMainlineViewModel = hiltViewModel()
) {
    var hasProcessedPhotos by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val navigateHome: () -> Unit = remember(navController) {
        {
            navController.navigate("main") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    val previousEntry = navController.previousBackStackEntry
    val savedStateHandle = previousEntry?.savedStateHandle ?: navController.currentBackStackEntry?.savedStateHandle
    val frontPhotoUri = savedStateHandle?.get<String>("front_photo_uri")
    val backPhotoUri = savedStateHandle?.get<String>("back_photo_uri")
    val barcodeResult = savedStateHandle?.get<String>("barcode_result")
    val folderPath = savedStateHandle?.get<String>("folder_path")
    val brandName = savedStateHandle?.get<String>("brand_name")

    LaunchedEffect(frontPhotoUri, backPhotoUri, folderPath, brandName) {
        if (frontPhotoUri != null && folderPath != null && !hasProcessedPhotos) {
            hasProcessedPhotos = true

            val frontUri = Uri.parse(frontPhotoUri)
            val backUri = backPhotoUri?.let(Uri::parse)

            val categoryDisplayName = folderPath.substringBefore("/").ifEmpty { folderPath }
            val resolvedBrand = when {
                !brandName.isNullOrBlank() -> brandName
                folderPath.contains("/") -> folderPath.substringAfter("/", "")
                else -> ""
            }

            viewModel.processAndSaveCar(
                frontPhotoUri = frontUri,
                backPhotoUri = backUri,
                category = categoryDisplayName,
                brand = resolvedBrand,
                preDetectedBarcode = barcodeResult?.takeIf { it.isNotBlank() }
            )

            previousEntry?.savedStateHandle?.apply {
                remove<String>("front_photo_uri")
                remove<String>("back_photo_uri")
                remove<String>("barcode_result")
                remove<String>("folder_path")
                remove<String>("brand_name")
                remove<String>("subcategory_name")
                remove<String>("car_type")
            }
            navController.currentBackStackEntry?.savedStateHandle?.apply {
                remove<String>("front_photo_uri")
                remove<String>("back_photo_uri")
                remove<String>("barcode_result")
                remove<String>("folder_path")
                remove<String>("brand_name")
                remove<String>("subcategory_name")
                remove<String>("car_type")
            }

        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AddCarUiState.Success -> {
                hasProcessedPhotos = false
                navigateHome()
            }
            is AddCarUiState.Error -> {
                hasProcessedPhotos = false
            }
            else -> Unit
        }
    }

    BackHandler(enabled = true) { navigateHome() }

    // ✅ PREVIEW POZĂ + SPINNER: Arată poza în timpul procesării
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Preview al pozei originale (blur sau normal)
        frontPhotoUri?.let { uri ->
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Overlay semi-transparent pentru a face spinner-ul mai vizibil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Spinner centrat cu text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.size(16.dp))
                
                Text(
                    text = "Processing photo...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}