// app/src/main/java/com/example/hotwheelscollectors/ui/screens/camera/BarcodeScannerScreen.kt

package com.example.hotwheelscollectors.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.viewmodels.BarcodeScannerViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    navController: androidx.navigation.NavController,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: BarcodeScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val foundCar by viewModel.foundCar.collectAsState()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val camera = remember { mutableStateOf<Camera?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    // Add permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    // Check and request permission on startup
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isProcessingBarcode by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val searchResult by viewModel.searchResult.collectAsState()
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }

    // Handle search result navigation
    LaunchedEffect(searchResult) {
        when (val result = searchResult) {
            is BarcodeScannerViewModel.BarcodeSearchResult.LocalCar -> {
                // ✅ Found in My Collection - navigate to CarDetailsScreen
                Log.d("BarcodeScannerScreen", "Navigating to My Collection car: ${result.car.id}")
                navController.navigate("car_details/${result.car.id}") {
                    popUpTo("barcode_scanner") { inclusive = true }
                }
            }
            is BarcodeScannerViewModel.BarcodeSearchResult.GlobalCar -> {
                // ✅ Found in Browse - navigate to Browse screen
                val globalCar = result.car
                val browseRoute = when {
                    globalCar.category.lowercase().contains("premium") -> "browse_premium"
                    globalCar.category.lowercase().contains("treasure") && globalCar.category.lowercase().contains("super") -> "browse_super_treasure_hunt"
                    globalCar.category.lowercase().contains("treasure") -> "browse_treasure_hunt"
                    globalCar.series.lowercase().contains("silver series") || globalCar.category.lowercase().contains("silver series") -> "browse_silver_series"
                    globalCar.category.lowercase().contains("other") || globalCar.series.lowercase() == "others" -> "browse_others"
                    else -> "browse_mainlines"
                }
                // Set barcode in savedStateHandle for filtering
                navController.currentBackStackEntry?.savedStateHandle?.set("search_barcode", globalCar.barcode)
                navController.navigate(browseRoute) {
                    popUpTo("barcode_scanner") { inclusive = true }
                }
            }
            is BarcodeScannerViewModel.BarcodeSearchResult.NotFound -> {
                // ❌ Not found - show dialog
                scannedBarcode = null // Will be set when barcode is scanned
                showNotFoundDialog = true
            }
            null -> {
                // No result yet - do nothing
            }
            else -> {
                // Exhaustive when - no other cases
            }
        }
    }

    // Legacy support - handle foundCar for backward compatibility
    LaunchedEffect(foundCar) {
        foundCar?.let { globalCar ->
            // Only navigate if searchResult is null (backward compatibility)
            if (searchResult == null) {
                val browseRoute = when {
                    globalCar.category.lowercase().contains("premium") -> "browse_premium"
                    globalCar.category.lowercase().contains("treasure") && globalCar.category.lowercase().contains("super") -> "browse_super_treasure_hunt"
                    globalCar.category.lowercase().contains("treasure") -> "browse_treasure_hunt"
                    globalCar.series.lowercase().contains("silver series") || globalCar.category.lowercase().contains("silver series") -> "browse_silver_series"
                    globalCar.category.lowercase().contains("other") || globalCar.series.lowercase() == "others" -> "browse_others"
                    else -> "browse_mainlines"
                }
                navController.currentBackStackEntry?.savedStateHandle?.set("search_barcode", globalCar.barcode)
                navController.navigate(browseRoute) {
                    popUpTo("barcode_scanner") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isProcessingBarcode) {
                            isProcessingBarcode = true
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty()) {
                                            barcodes[0].rawValue?.let { barcode ->
                                                scannedBarcode = barcode
                                                // Search in both local collection and global database
                                                viewModel.searchBarcode(barcode)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        scanError = e.localizedMessage
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                        isProcessingBarcode = false
                                    }
                            } else {
                                imageProxy.close()
                                isProcessingBarcode = false
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            // UNBIND ALL FIRST, then bind to lifecycle
            cameraProvider.unbindAll()
            
            // Give a small delay to ensure previous bindings are cleared
            delay(100)
            
            camera.value = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            
            // Set surface provider AFTER successful binding
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Set up camera controls
            camera.value?.let { cameraInstance ->
                cameraInstance.cameraControl.enableTorch(isFlashEnabled)
                cameraInstance.cameraControl.setLinearZoom(0f)

                // Set up auto focus on center
                val focusMeteringAction = FocusMeteringAction.Builder(
                    previewView.meteringPointFactory.createPoint(
                        previewView.width / 2f,
                        previewView.height / 2f
                    )
                ).build()

                cameraInstance.cameraControl.startFocusAndMetering(focusMeteringAction)
            }
        } catch (e: Exception) {
            scanError = "Camera initialization failed: ${e.localizedMessage}"
        }
    }

    LaunchedEffect(scanError) {
        scanError?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            scanError = null
        }
    }

    LaunchedEffect(isFlashEnabled) {
        camera.value?.cameraControl?.enableTorch(isFlashEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Force update when camera changes
                    camera.value?.let { cam ->
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                ScannerOverlay(
                    isProcessing = isProcessingBarcode
                )
            }

            // Camera controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { isFlashEnabled = !isFlashEnabled }
                ) {
                    Icon(
                        imageVector = if (isFlashEnabled)
                            Icons.Default.FlashOn
                        else
                            Icons.Default.FlashOff,
                        contentDescription = "Toggle flash",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            CameraPermissionRequest(
                onPermissionGranted = { hasCameraPermission = true }
            )
        }

        // Top bar with close button (Custom implementation)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = androidx.compose.ui.graphics.Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close scanner",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.scan_barcode),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Dialog for "Car Not Found"
        if (showNotFoundDialog) {
            CarNotFoundDialog(
                barcode = scannedBarcode ?: "",
                onAddManually = {
                    showNotFoundDialog = false
                    // Navigate to category selection for adding new car
                    navController.navigate("category_selection") {
                        popUpTo("barcode_scanner") { inclusive = true }
                    }
                },
                onDismiss = {
                    showNotFoundDialog = false
                    scannedBarcode = null
                }
            )
        }
    }
}

@Composable
private fun CarNotFoundDialog(
    barcode: String,
    onAddManually: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Mașină necunoscută")
        },
        text = {
            Column {
                Text("Barcode: $barcode")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Nu am găsit această mașină în baza de date.")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vrei să o adaugi manual?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAddManually) {
                Text("Adaugă manual")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează")
            }
        }
    )
}

@Composable
private fun ScannerOverlay(
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(250.dp)
            .border(
                width = 2.dp,
                color = if (isProcessing)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CameraPermissionRequest(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.camera_permission_required),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.camera_permission_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { launcher.launch(Manifest.permission.CAMERA) }
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.grant_camera_permission))
        }
    }
}