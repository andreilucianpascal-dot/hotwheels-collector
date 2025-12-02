package com.example.hotwheelscollectors.ui.screens.camera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.viewmodels.CameraCaptureViewModel
import java.io.File
import java.util.*

@Composable
fun CameraCaptureScreen(
    navController: NavController,
    viewModel: CameraCaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentStep by remember { mutableStateOf(CameraStep.FRONT) }
    var frontPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var backPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
                when (currentStep) {
                    CameraStep.FRONT -> {
                    frontPhotoUri = viewModel.getCurrentPhotoUri()
                        currentStep = CameraStep.BACK
                    }
                    CameraStep.BACK -> {
                    backPhotoUri = viewModel.getCurrentPhotoUri()
                    // Process photos and return to AddMainlineScreen
                    viewModel.processPhotosAndReturn(
                        frontPhotoUri = frontPhotoUri,
                        backPhotoUri = backPhotoUri,
                        navController = navController
                    )
                }
            }
        }
    }

    fun takePhoto() {
        val photoFile = File(
            context.cacheDir,
            "temp_${currentStep.name.lowercase()}_${UUID.randomUUID()}.jpg"
        )
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
        viewModel.setCurrentPhotoUri(uri)
        photoLauncher.launch(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (currentStep) {
                            CameraStep.FRONT -> "Take Front Photo"
                            CameraStep.BACK -> "Take Back Photo"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                        Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentStep) {
                            CameraStep.FRONT -> "Take a clear photo of the FRONT of the car"
                            CameraStep.BACK -> "Take a clear photo of the BACK of the car (barcode side)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentStep) {
                            CameraStep.FRONT -> "Make sure the car is centered and well-lit"
                            CameraStep.BACK -> "Make sure the barcode is visible and readable"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Front photo step
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (currentStep == CameraStep.FRONT) 
                                    MaterialTheme.colorScheme.primary
                                else if (frontPhotoUri != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (frontPhotoUri != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Front photo taken",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "1",
                                color = if (currentStep == CameraStep.FRONT) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Front",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentStep == CameraStep.FRONT) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Arrow
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.outline
                )

                // Back photo step
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (currentStep == CameraStep.BACK) 
                                    MaterialTheme.colorScheme.primary
                                else if (backPhotoUri != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (backPhotoUri != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Back photo taken",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "2",
                                color = if (currentStep == CameraStep.BACK) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentStep == CameraStep.BACK) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Capture button
            Button(
                onClick = { takePhoto() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (currentStep) {
                        CameraStep.FRONT -> "Capture Front Photo"
                        CameraStep.BACK -> "Capture Back Photo"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status message
            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

enum class CameraStep {
    FRONT,
    BACK
}
