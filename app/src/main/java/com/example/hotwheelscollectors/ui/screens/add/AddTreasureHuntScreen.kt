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
import com.example.hotwheelscollectors.viewmodels.AddTreasureHuntViewModel
import com.example.hotwheelscollectors.viewmodels.AddCarUiState

@Composable
fun AddTreasureHuntScreen(
    navController: NavController,
    viewModel: AddTreasureHuntViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasProcessedPhotos by rememberSaveable { mutableStateOf(false) }

    val navigateHome: () -> Unit = remember(navController) {
        {
            navController.navigate("main") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    // Retrieve photos from navigation (returned from TakePhotosScreen)
    val previousEntry = navController.previousBackStackEntry
    val savedStateHandle = previousEntry?.savedStateHandle ?: navController.currentBackStackEntry?.savedStateHandle
    val frontPhotoUri = savedStateHandle?.get<String>("front_photo_uri")
    val backPhotoUri = savedStateHandle?.get<String>("back_photo_uri")
    val barcodeResult = savedStateHandle?.get<String>("barcode_result")

    // ✅ PROCESARE + SALVARE + NAVIGARE INSTANT (ca la Mainline)
    LaunchedEffect(frontPhotoUri, backPhotoUri) {
        if (frontPhotoUri != null && !hasProcessedPhotos) {
            
            hasProcessedPhotos = true
            
            android.util.Log.d("AddTreasureHuntScreen", "=== PROCESSING DATA FROM TAKEPHOTOSSCREEN ===")
            android.util.Log.d("AddTreasureHuntScreen", "frontPhotoUri: $frontPhotoUri")
            android.util.Log.d("AddTreasureHuntScreen", "backPhotoUri: $backPhotoUri")
            android.util.Log.d("AddTreasureHuntScreen", "barcodeResult: $barcodeResult")

            val frontUri = Uri.parse(frontPhotoUri)
            val backUri = if (backPhotoUri != null) Uri.parse(backPhotoUri) else null
            
            // Procesare + Salvare (o singură funcție)
            viewModel.processAndSaveCar(frontUri, backUri)
            
            android.util.Log.d("AddTreasureHuntScreen", "Save started, waiting for Success state...")
            
            // Curăță saved state din TOATE entry-urile
            navController.currentBackStack.value.forEach { entry ->
                entry.savedStateHandle.remove<String>("front_photo_uri")
                entry.savedStateHandle.remove<String>("back_photo_uri")
                entry.savedStateHandle.remove<String>("barcode_result")
                entry.savedStateHandle.remove<String>("folder_path")
                entry.savedStateHandle.remove<String>("brand_name")
                entry.savedStateHandle.remove<String>("car_type")
            }
        }
    }

    // ✅ FIX: Navighează DOAR după Success (ca la Mainline)
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

    // ✅ BackHandler: Navigare directă la Main (ca la Mainline)
    BackHandler(enabled = true) { navigateHome() }

    // ✅ PREVIEW POZĂ + SPINNER: Arată poza în timpul procesării
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Preview al pozei originale
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
                    text = "Procesare poză...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
