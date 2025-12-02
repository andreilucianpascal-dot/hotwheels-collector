package com.example.hotwheelscollectors.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

enum class SimplePhotoStep {
    FRONT_PHOTO,
    BACK_PHOTO,
    CHOOSE_FOLDER,
    SAVE_COMPLETE
}

private enum class FolderSelectionStep {
    CATEGORY_SELECTION,
    BRAND_SELECTION,
    SUBCATEGORY_SELECTION,
    CONFIRMATION
}

@Composable
fun TakePhotosScreen(
    returnRoute: String,
    brandId: String? = null,
    categoryId: String? = null,
    onPhotosComplete: (frontUri: Uri, backUri: Uri, barcode: String, croppedBarcodeUri: Uri?, folderPath: String?) -> Unit,
    onDismiss: () -> Unit,
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentStep by rememberSaveable { mutableStateOf(SimplePhotoStep.FRONT_PHOTO) }
    var frontPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var backPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val frontPhotoUri = frontPhotoUriString?.let(Uri::parse)
    val backPhotoUri = backPhotoUriString?.let(Uri::parse)
    var detectedBarcode by rememberSaveable { mutableStateOf("") }
    
    // Handle photo completion - navigate to category selection for main screen flow
    LaunchedEffect(frontPhotoUri, backPhotoUri, detectedBarcode) {
        android.util.Log.d("TakePhotosScreen", "=== PHOTO COMPLETION TRIGGERED ===")
        android.util.Log.d("TakePhotosScreen", "frontPhotoUri: $frontPhotoUri")
        android.util.Log.d("TakePhotosScreen", "backPhotoUri: $backPhotoUri")
        android.util.Log.d("TakePhotosScreen", "detectedBarcode: $detectedBarcode")
        android.util.Log.d("TakePhotosScreen", "returnRoute: $returnRoute")
        
        // Require both photos for proper barcode extraction
        if (frontPhotoUri != null && backPhotoUri != null) {
            android.util.Log.d("TakePhotosScreen", "At least front photo captured!")
            
            // Check if this is TH/STH/Others flow (no category selection needed)
            val isSimpleFlow = returnRoute.contains("treasure_hunt") || 
                              returnRoute.contains("super_treasure_hunt") || 
                              returnRoute.contains("others")
            
            if (isSimpleFlow) {
                android.util.Log.d("TakePhotosScreen", "Simple flow (TH/STH/Others) - saving to previousBackStack and navigating back")
                val previousEntry = navController?.previousBackStackEntry
                if (previousEntry != null) {
                    previousEntry.savedStateHandle.set("front_photo_uri", frontPhotoUri.toString())
                    previousEntry.savedStateHandle.set("back_photo_uri", backPhotoUri.toString())
                    previousEntry.savedStateHandle.set("barcode_result", detectedBarcode)
                    val carType = when {
                        returnRoute.contains("treasure_hunt") -> "treasure_hunt"
                        returnRoute.contains("super_treasure_hunt") -> "super_treasure_hunt"
                        returnRoute.contains("others") -> "others"
                        else -> null
                    }
                    carType?.let { previousEntry.savedStateHandle.set("car_type", it) }
                    android.util.Log.d("TakePhotosScreen", "Saved to SavedStateHandle for simple flow. Navigating up...")
                    navController?.navigateUp()
                } else {
                    android.util.Log.w("TakePhotosScreen", "previousBackStackEntry is null in simple flow; falling back to onPhotosComplete")
                    frontPhotoUri?.let { front ->
                        backPhotoUri?.let { back ->
                            onPhotosComplete(front, back, detectedBarcode, null, null)
                        }
                    }
                }
            } else if (brandId == null && categoryId == null) {
                android.util.Log.d("TakePhotosScreen", "Main screen flow - navigating to CHOOSE_FOLDER")
                // Main screen flow - go to folder selection
                currentStep = SimplePhotoStep.CHOOSE_FOLDER
            } else {
                android.util.Log.d("TakePhotosScreen", "Collection flow - returning photos")
                // Collection flow - return photos to caller (they will handle saving)
                frontPhotoUri?.let { front ->
                    backPhotoUri?.let { back ->
                        onPhotosComplete(front, back, detectedBarcode, null, null)
                    }
                }
            }
        }
    }

    // Handle back navigation between steps
    BackHandler {
        when (currentStep) {
            SimplePhotoStep.FRONT_PHOTO -> {
                navController?.navigateUp()
            }
            SimplePhotoStep.BACK_PHOTO -> {
                currentStep = SimplePhotoStep.FRONT_PHOTO
            }
            SimplePhotoStep.CHOOSE_FOLDER -> {
                // ✅ Pentru Others, TH, STH: Direct exit (no need to go back to photos)
                if (returnRoute.contains("others") || returnRoute.contains("treasure_hunt") || returnRoute.contains("super_treasure_hunt")) {
                    navController?.navigateUp()
                } else {
                    currentStep = SimplePhotoStep.BACK_PHOTO
                }
            }
            SimplePhotoStep.SAVE_COMPLETE -> {
                navController?.navigateUp()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
            SimplePhotoStep.FRONT_PHOTO -> {
                SimpleCameraView(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    stepTitle = "Take Front Photo",
                    stepDescription = "Position the front of the carded car in the frame",
                    onPhotoTaken = { uri ->
                        frontPhotoUriString = uri.toString()
                        currentStep = SimplePhotoStep.BACK_PHOTO
                    },
                    onBack = onDismiss
                )
            }
            
            SimplePhotoStep.BACK_PHOTO -> {
                SimpleCameraView(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    stepTitle = "Take Back Photo",
                    stepDescription = "Position the back of the carded car (with barcode) in the frame",
                    onPhotoTaken = { uri ->
                        backPhotoUriString = uri.toString()
                        // Try to detect barcode from back photo
                        detectBarcodeFromUri(context, uri) { barcode ->
                            detectedBarcode = barcode ?: ""
                        }
                    },
                    onBack = { currentStep = SimplePhotoStep.FRONT_PHOTO }
                )
            }

            SimplePhotoStep.CHOOSE_FOLDER -> {
                ChooseFolderViewNew(
                    returnRoute = returnRoute,
                    frontPhotoUri = frontPhotoUri,
                    backPhotoUri = backPhotoUri,
                    detectedBarcode = detectedBarcode,
                    onPhotosComplete = onPhotosComplete,
                    onBack = { currentStep = SimplePhotoStep.BACK_PHOTO },
                    navController = navController
                )
            }
            
            SimplePhotoStep.SAVE_COMPLETE -> {
                // This step is handled by navigation back
            }
        }
        
        // Progress indicator
        LinearProgressIndicator(
            progress = when (currentStep) {
                SimplePhotoStep.FRONT_PHOTO -> 0.33f
                SimplePhotoStep.BACK_PHOTO -> 0.66f
                SimplePhotoStep.CHOOSE_FOLDER -> 1.0f
                SimplePhotoStep.SAVE_COMPLETE -> 1.0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
        
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SimpleCameraView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    stepTitle: String,
    stepDescription: String,
    onPhotoTaken: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val previewView = remember { PreviewView(context) }
    val camera = remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val capture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            cameraProvider.unbindAll()
            delay(100)
            
            camera.value = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            
            imageCapture.value = capture
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
        } catch (e: Exception) {
            cameraError = "Camera error: ${e.message}"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(32.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stepDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // Camera controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Back")
            }
            
            // Flash control
            IconButton(
                onClick = { 
                    isFlashEnabled = !isFlashEnabled
                    camera.value?.cameraControl?.enableTorch(isFlashEnabled)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle flash",
                    tint = Color.White
                )
            }

            // Shutter button
            Button(
                onClick = {
                    imageCapture.value?.let { capture ->
                        takeSimplePhoto(context, capture, onPhotoTaken) { error ->
                            cameraError = error
                        }
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Take photo",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

private fun takeSimplePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoTaken: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = createImageFile(context)
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Auto-crop the photo to center 80%
                val croppedUri = autoCropPhoto(context, photoUri)
                onPhotoTaken(croppedUri ?: photoUri)
            }

            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("TakePhotosScreen", "Photo capture failed", exception)
                onError(exception.message ?: "Failed to take photo")
            }
        }
    )
}

private fun autoCropPhoto(context: Context, originalUri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(originalUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            // Crop to center 80% of the image
            val cropMargin = (bitmap.width * 0.1f).toInt()
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropMargin,
                cropMargin,
                bitmap.width - (2 * cropMargin),
                bitmap.height - (2 * cropMargin)
            )

            // Save cropped bitmap
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val croppedFile = File(context.getExternalFilesDir("Photos"), "CROPPED_${timeStamp}.jpg")
            
            FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Properly recycle bitmaps to free memory
            bitmap.recycle()
            croppedBitmap.recycle()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", croppedFile)
        } else {
            android.util.Log.w("TakePhotosScreen", "Failed to load bitmap for cropping from URI: $originalUri")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("TakePhotosScreen", "Error during photo cropping", e)
        null
    }
}

private fun detectBarcodeFromUri(context: Context, uri: Uri, onResult: (String?) -> Unit) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()?.rawValue
                    
                    // ✅ DON'T DELETE - photos needed for processing and saving
                    onResult(barcode)
                    bitmap.recycle()
                }
                .addOnFailureListener {
                    // ✅ DON'T DELETE - photos needed for processing and saving
                    
                    onResult(null)
                    bitmap.recycle()
                }
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("Photos")
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}


@Composable
private fun CategorySelectionStep(
    returnRoute: String,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Category:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Debug info
        Text(
            text = "Return route: $returnRoute",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            when (returnRoute) {
                "take_photos/add_mainline", "add_mainline" -> {
                    MainlineCategoryGrid(onCategorySelected = onCategorySelected)
                }

                "take_photos/add_premium", "add_premium" -> {
                    PremiumCategoryGrid(onCategorySelected = onCategorySelected)
                }

                "take_photos/add_treasure_hunt", "add_treasure_hunt" -> {
                    // Show a simple button for treasure hunt instead of auto-selecting
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Treasure Hunt",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mixed cars from different brands - no specific folders needed",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onCategorySelected("treasure_hunt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text("Save to Treasure Hunt")
                        }
                    }
                }

                "take_photos/add_super_treasure_hunt", "add_super_treasure_hunt" -> {
                    // Show a simple button for super treasure hunt instead of auto-selecting
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Super Treasure Hunt",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFFD700) // Gold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mixed cars from different brands - no specific folders needed",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onCategorySelected("super_treasure_hunt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700), // Gold
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Save to Super Treasure Hunt")
                        }
                    }
                }

                "take_photos/add_others", "add_others" -> {
                    // Show a simple button for others instead of auto-selecting
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Others",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Miscellaneous cars",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onCategorySelected("others") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save to Others")
                        }
                    }
                }

                else -> {
                    // Unknown route - show error message
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Unknown Route",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Route: $returnRoute",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This route is not supported",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun MainlineCategoryGrid(onCategorySelected: (String) -> Unit) {
    val categories = listOf(
        CategoryOption("rally", "Rally", Color.Black, Color.Red),
        CategoryOption("hot_roads", "Hot Roads", Color(0xFFFF9800), Color.Black),
        CategoryOption("convertibles", "Convertibles", Color.White, Color.Red),
        CategoryOption("vans", "Vans", Color.Blue, Color.White),
        CategoryOption("supercars", "Supercars", Color.White, Color.Black),
        CategoryOption("american_muscle", "American Muscle", Color(0xFFD2691E), Color(0xFFFFFDD0)),
        CategoryOption("motorcycle", "Motorcycle", Color(0xFFFF9800), Color.Red),
        CategoryOption("suv_trucks", "SUV & Trucks", Color(0xFF8B4513), Color.Black)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "✨ Select a Mainline Category:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Button(
                    onClick = { onCategorySelected(category.id) },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = category.backgroundColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = category.title,
                        color = category.textColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCategoryGrid(onCategorySelected: (String) -> Unit) {
    val categories = listOf(
        CategoryOption("Car Culture", "Car Culture", Color(0xFF1976D2), Color.White),
        CategoryOption("Pop Culture", "Pop Culture", Color(0xFFE91E63), Color.White),
        CategoryOption("Boulevard", "Boulevard", Color(0xFF424242), Color.White),
        CategoryOption("F1", "F1", Color(0xFFD32F2F), Color.White),
        CategoryOption("RLC", "RLC", Color(0xFF7B1FA2), Color.White),
        CategoryOption("1:43 Scale", "1:43 Scale", Color(0xFF388E3C), Color.White),
        CategoryOption("Others Premium", "Others Premium", Color(0xFF616161), Color.White)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "🏆 Select Premium Category:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Button(
                    onClick = { onCategorySelected(category.id) },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = category.backgroundColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = category.title,
                        color = category.textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandSelectionStep(
    categoryId: String,
    onBrandSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val brands = com.example.hotwheelscollectors.domain.catalog.BrandCatalog
        .getBrandsForCategory(categoryId)
        .sortedBy { it.second }

    val categoryDisplayName = when (categoryId) {
        "rally" -> "Rally"
        "supercars" -> "Supercars"
        "american_muscle" -> "American Muscle"
        "vans" -> "Vans"
        "convertibles" -> "Convertibles"
        "suv_trucks" -> "SUV & Trucks"
        "motorcycle" -> "Motorcycle"
        "mainline" -> "Mainline"
        "premium" -> "Premium"
        "others" -> "Others"
        "treasure_hunt" -> "Treasure Hunt"
        "super_treasure_hunt" -> "Super Treasure Hunt"
        else -> categoryId.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "🏭 Select Brand for $categoryDisplayName:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(brands) { (brandId, brandName) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBrandSelected(brandId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = brandName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Categories")
        }
    }
}





@Composable
private fun ConfirmationView(
    categoryId: String,
    brandId: String?,
    subcategoryId: String? = null,
    frontPhotoUri: Uri?,
    backPhotoUri: Uri?,
    detectedBarcode: String,
    onConfirm: (Uri, Uri, String, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val categoryDisplayName = when (categoryId) {
        "rally" -> "Rally"
        "supercars" -> "Supercars"
        "american_muscle" -> "American Muscle"
        "vans" -> "Vans"
        "convertibles" -> "Convertibles"
        "suv_trucks" -> "SUV & Trucks"
        "motorcycle" -> "Motorcycle"
        "hot_roads" -> "Hot Rods"
        "Car Culture" -> "Car Culture"
        "Pop Culture" -> "Pop Culture"
        "Boulevard" -> "Boulevard"
        "F1" -> "F1"
        "RLC" -> "RLC"
        "1:43 Scale" -> "1:43 Scale"
        "Others Premium" -> "Others Premium"
        "mainline" -> "Mainline"
        "premium" -> "Premium"
        "others" -> "Others"
        "treasure_hunt" -> "Treasure Hunt"
        "super_treasure_hunt" -> "Super Treasure Hunt"
        else -> categoryId.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }
    
    val subcategoryDisplayName = subcategoryId?.let {
        when (it) {
            "modern_classics" -> "Modern Classics"
            "race_day" -> "Race Day"
            "circuit_legends" -> "Circuit Legends"
        "team_transport" -> "Team Transport"
            "silhouettes" -> "Silhouettes"
            "jay_lenos_garage" -> "Jay Leno's Garage"
            "rtr_vehicles" -> "RTR Vehicles"
            "real_riders" -> "Real Riders"
            "fast_wagons" -> "Fast Wagons"
            "speed_machine" -> "Speed Machine"
            "japan_historics" -> "Japan Historics"
            "hammer_drop" -> "Hammer Drop"
            "slide_street" -> "Slide Street"
            "terra_trek" -> "Terra Trek"
            "exotic_envy" -> "Exotic Envy"
            "cargo_containers" -> "Cargo Containers"
            "fast_and_furious" -> "Fast & Furious"
            "mario_kart" -> "Mario Kart"
            "forza_motorsport" -> "Forza Motorsport"
            "gran_turismo" -> "Gran Turismo"
            "top_gun" -> "Top Gun"
            "batman" -> "Batman"
            "star_wars" -> "Star Wars"
            "marvel" -> "Marvel"
            "jurassic_world" -> "Jurassic World"
            "back_to_the_future" -> "Back to the Future"
            "looney_tunes" -> "Looney Tunes"
            else -> it.replace("_", " ").split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercaseChar() } }
        }
    }
    
    val brandDisplayName = brandId?.replace("_", " ")?.split(" ")
        ?.joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    
    val folderPath = when {
        subcategoryId != null -> "$categoryDisplayName/$subcategoryDisplayName"
        brandId != null -> "$categoryDisplayName/$brandDisplayName"
        else -> categoryDisplayName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Save Car To:",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when {
                        subcategoryId != null -> "$categoryDisplayName → $subcategoryDisplayName"
                        brandId != null -> "$categoryDisplayName → $brandDisplayName"
                        else -> categoryDisplayName
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Path: $folderPath",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (detectedBarcode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Barcode: $detectedBarcode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (frontPhotoUri != null && backPhotoUri != null) {
                    android.util.Log.d("TakePhotosScreen", "=== CONFIRMATION VIEW - SAVE BUTTON CLICKED ===")
                    android.util.Log.d("TakePhotosScreen", "Category: $categoryDisplayName")
                    android.util.Log.d("TakePhotosScreen", "Brand: $brandDisplayName")
                    android.util.Log.d("TakePhotosScreen", "Barcode: $detectedBarcode")
                    android.util.Log.d("TakePhotosScreen", "subcategoryId: $subcategoryId")
                    
                    val isPremiumFlow = categoryId in listOf("Car Culture", "Pop Culture", "Boulevard", "F1", "RLC", "1:43 Scale", "Others Premium")
                    android.util.Log.d("TakePhotosScreen", "isPremiumFlow: $isPremiumFlow")
                    
                    // ✅ TOATE TIPURILE: Doar returnează URI-urile la Add Screen-ul lor
                    // Fiecare Add Screen își gestionează propria salvare!
                    android.util.Log.d("TakePhotosScreen", "=== BEFORE onConfirm CALL ===")
                    android.util.Log.d("TakePhotosScreen", "frontPhotoUri: $frontPhotoUri")
                    android.util.Log.d("TakePhotosScreen", "backPhotoUri: $backPhotoUri")
                    android.util.Log.d("TakePhotosScreen", "categoryDisplayName: $categoryDisplayName")
                    
                    if (frontPhotoUri != null && backPhotoUri != null) {
                        if (isPremiumFlow) {
                            android.util.Log.d("TakePhotosScreen", "Premium flow - calling onConfirm")
                            onConfirm(frontPhotoUri, backPhotoUri, detectedBarcode, null, folderPath)
                            android.util.Log.d("TakePhotosScreen", "Premium flow - onConfirm returned")
                        } else {
                            android.util.Log.d("TakePhotosScreen", "Mainline flow - calling onConfirm")
                            onConfirm(frontPhotoUri, backPhotoUri, detectedBarcode, null, folderPath)
                            android.util.Log.d("TakePhotosScreen", "Mainline flow - onConfirm returned")
                        }
                    } else {
                        android.util.Log.e("TakePhotosScreen", "ERROR: frontPhotoUri or backPhotoUri is NULL!")
                        android.util.Log.e("TakePhotosScreen", "frontPhotoUri: $frontPhotoUri")
                        android.util.Log.e("TakePhotosScreen", "backPhotoUri: $backPhotoUri")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Save Car Now",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}



@Composable
private fun ChooseFolderViewNew(
    returnRoute: String,
    frontPhotoUri: Uri?,
    backPhotoUri: Uri?,
    detectedBarcode: String,
    onPhotosComplete: (frontUri: Uri, backUri: Uri, barcode: String, croppedBarcodeUri: Uri?, folderPath: String?) -> Unit,
    onBack: () -> Unit,
    navController: androidx.navigation.NavController? = null
) {
    android.util.Log.d("TakePhotosScreen", "=== CHOOSE_FOLDER_VIEW_NEW ===")
    android.util.Log.d("TakePhotosScreen", "returnRoute: $returnRoute")
    android.util.Log.d("TakePhotosScreen", "frontPhotoUri: $frontPhotoUri")
    android.util.Log.d("TakePhotosScreen", "backPhotoUri: $backPhotoUri")
    
    var folderSelectionStep by remember { mutableStateOf(FolderSelectionStep.CATEGORY_SELECTION) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedSubcategory by remember { mutableStateOf<String?>(null) }
    
    val isPremium = returnRoute.contains("premium", ignoreCase = true)
    android.util.Log.d("TakePhotosScreen", "isPremium: $isPremium")
    
    when (folderSelectionStep) {
        FolderSelectionStep.CATEGORY_SELECTION -> {
            CategorySelectionView(
                returnRoute = returnRoute,
                onCategorySelected = { categoryId ->
                    selectedCategory = categoryId
                    if (isPremium) {
                        // Check if this premium category has subcategories
                        val hasSubcategories = when (categoryId) {
                            "Car Culture", "Pop Culture" -> true
                            else -> false
                        }
                        if (hasSubcategories) {
                            folderSelectionStep = FolderSelectionStep.SUBCATEGORY_SELECTION
                        } else {
                            folderSelectionStep = FolderSelectionStep.CONFIRMATION
                        }
                    } else if (categoryId in listOf("others", "treasure_hunt", "super_treasure_hunt")) {
                        // ✅ Others, TH, STH: Direct to Save Car (no brand selection needed)
                        folderSelectionStep = FolderSelectionStep.CONFIRMATION
                    } else {
                        folderSelectionStep = FolderSelectionStep.BRAND_SELECTION
                    }
                },
                onBack = onBack
            )
        }
        
        FolderSelectionStep.BRAND_SELECTION -> {
            BrandSelectionView(
                categoryId = selectedCategory ?: "",
                onBrandSelected = { brandId ->
                    selectedBrand = brandId
                    folderSelectionStep = FolderSelectionStep.CONFIRMATION
                },
                onBack = { folderSelectionStep = FolderSelectionStep.CATEGORY_SELECTION }
            )
        }
        
        FolderSelectionStep.SUBCATEGORY_SELECTION -> {
            PremiumSubcategorySelectionView(
                categoryId = selectedCategory ?: "",
                onSubcategorySelected = { subcategoryId ->
                    selectedSubcategory = subcategoryId
                    folderSelectionStep = FolderSelectionStep.CONFIRMATION
                },
                onBack = { folderSelectionStep = FolderSelectionStep.CATEGORY_SELECTION }
            )
        }
        
        FolderSelectionStep.CONFIRMATION -> {
            ConfirmationView(
                categoryId = selectedCategory ?: "",
                brandId = selectedBrand,
                subcategoryId = selectedSubcategory,
                frontPhotoUri = frontPhotoUri,
                backPhotoUri = backPhotoUri,
                detectedBarcode = detectedBarcode,
                onConfirm = { front: Uri, back: Uri, barcode: String, croppedBarcode: String?, folderPath: String? ->
                    android.util.Log.d("TakePhotosScreen", "=== onConfirm LAMBDA CALLED ===")
                    android.util.Log.d("TakePhotosScreen", "front: $front")
                    android.util.Log.d("TakePhotosScreen", "back: $back")
                    android.util.Log.d("TakePhotosScreen", "barcode: $barcode")
                    android.util.Log.d("TakePhotosScreen", "folderPath: $folderPath")
                    android.util.Log.d("TakePhotosScreen", "navController: $navController")
                    
                    // Save data to previous screen's savedStateHandle and navigate back
                    if (navController != null) {
                        android.util.Log.d("TakePhotosScreen", "Using navController to save data")
                        android.util.Log.d("TakePhotosScreen", "Current backstack size: ${navController.currentBackStack.value.size}")
                        navController.currentBackStack.value.forEach { entry ->
                            android.util.Log.d("TakePhotosScreen", "  - ${entry.destination.route}")
                        }
                        android.util.Log.d("TakePhotosScreen", "previousBackStackEntry: ${navController.previousBackStackEntry?.destination?.route}")
                        
                        val previousEntry = navController.previousBackStackEntry
                        if (previousEntry != null) {
                            android.util.Log.d("TakePhotosScreen", "Setting savedStateHandle values...")
                            previousEntry.savedStateHandle.set("front_photo_uri", front.toString())
                            previousEntry.savedStateHandle.set("back_photo_uri", back.toString())
                            previousEntry.savedStateHandle.set("barcode_result", barcode)
                            folderPath?.let { previousEntry.savedStateHandle.set("folder_path", it) }
                            selectedBrand?.let { previousEntry.savedStateHandle.set("brand_name", it) }
                            selectedSubcategory?.let { previousEntry.savedStateHandle.set("subcategory_name", it) }
                            
                            // Determine car type based on return route
                            val carType = when {
                                returnRoute.contains("mainline") -> "mainline"
                                returnRoute.contains("premium") -> "premium"
                                returnRoute.contains("treasure_hunt") -> "treasure_hunt"
                                returnRoute.contains("super_treasure_hunt") -> "super_treasure_hunt"
                                returnRoute.contains("others") -> "others"
                                else -> "mainline" // fallback
                            }
                            android.util.Log.d("TakePhotosScreen", "Setting car_type to: '$carType' (from returnRoute: '$returnRoute')")
                            previousEntry.savedStateHandle.set("car_type", carType)
                            android.util.Log.d("TakePhotosScreen", "Data saved. Navigating up...")
                            navController.navigateUp()
                        } else {
                            android.util.Log.e("TakePhotosScreen", "ERROR: previousBackStackEntry is NULL!")
                            android.util.Log.e("TakePhotosScreen", "This means we cannot save data to the previous screen")
                            android.util.Log.e("TakePhotosScreen", "Falling back to onPhotosComplete...")
                            onPhotosComplete(front, back, barcode, croppedBarcode?.let { Uri.parse(it) }, folderPath)
                        }
                    } else {
                        android.util.Log.d("TakePhotosScreen", "Calling onPhotosComplete...")
                        onPhotosComplete(front, back, barcode, croppedBarcode?.let { Uri.parse(it) }, folderPath)
                        android.util.Log.d("TakePhotosScreen", "onPhotosComplete returned")
                    }
                },
                onBack = { 
                    if (isPremium) {
                        // Check if we have a subcategory selected, go back to subcategory selection
                        if (selectedSubcategory != null) {
                            folderSelectionStep = FolderSelectionStep.SUBCATEGORY_SELECTION
                        } else {
                            folderSelectionStep = FolderSelectionStep.CATEGORY_SELECTION
                        }
                    } else {
                        folderSelectionStep = FolderSelectionStep.BRAND_SELECTION
                    }
                }
            )
        }
    }
}

@Composable
private fun CategorySelectionView(
    returnRoute: String,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    android.util.Log.d("TakePhotosScreen", "=== CATEGORY_SELECTION_VIEW ===")
    android.util.Log.d("TakePhotosScreen", "returnRoute: $returnRoute")
    
    val categories = remember {
        val result = when (returnRoute) {
            "add_premium", "take_photos/add_premium" -> listOf(
                CategoryOption("Car Culture", "Car Culture", Color(0xFF1976D2), Color.White),
                CategoryOption("Pop Culture", "Pop Culture", Color(0xFFE91E63), Color.White),
                CategoryOption("Boulevard", "Boulevard", Color(0xFF424242), Color.White),
                CategoryOption("F1", "F1", Color(0xFFD32F2F), Color.White),
                CategoryOption("RLC", "RLC", Color(0xFF7B1FA2), Color.White),
                CategoryOption("1:43 Scale", "1:43 Scale", Color(0xFF388E3C), Color.White),
                CategoryOption("Others Premium", "Others Premium", Color(0xFF616161), Color.White)
            )
            "add_mainline", "take_photos/add_mainline" -> listOf(
                CategoryOption("rally", "Rally", Color.Black, Color.Red),
                CategoryOption("hot_roads", "Hot Rods", Color(0xFFFF9800), Color.Black),
                CategoryOption("convertibles", "Convertibles", Color.White, Color.Red),
                CategoryOption("vans", "Vans", Color.Blue, Color.White),
                CategoryOption("supercars", "Supercars", Color.White, Color.Black),
                CategoryOption("american_muscle", "American Muscle", Color(0xFFD2691E), Color(0xFFFFFDD0)),
                CategoryOption("motorcycle", "Motorcycle", Color(0xFFFF9800), Color.Red),
                CategoryOption("suv_trucks", "SUV & Trucks", Color(0xFF8B4513), Color.Black)
            )
            else -> emptyList() // TH/STH/Others should not show categories at all
        }
        android.util.Log.d("TakePhotosScreen", "Categories count: ${result.size}")
        result
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(category.id) },
                    colors = CardDefaults.cardColors(containerColor = category.backgroundColor)
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = category.textColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandSelectionView(
    categoryId: String,
    onBrandSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val brands = remember(categoryId) {
        when (categoryId) {
            "rally" -> listOf("subaru", "mitsubishi", "lancia", "peugeot", "citroen", "toyota", "ford", "audi", "volkswagen", "mazda", "bmw", "volvo", "datsun", "opel", "nissan").sortedBy { it }
            "supercars" -> listOf("ferrari", "lamborghini", "maserati", "pagani", "bugatti", "mclaren", "koenigsegg", "aston_martin", "rimac", "lucid_air", "ford_gt", "mazda_787b", "automobili_pininfarina", "bentley", "porsche", "corvette").sortedBy { it }
            "american_muscle" -> listOf("ford", "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac", "oldsmobile", "plymouth", "lincoln", "mercury", "camaro", "chevy", "corvette", "chevelle", "el_camino", "impala", "nova", "challenger", "charger", "super_bee", "mustang", "thunderbird", "cougar", "barracuda", "firebird", "gto").sortedBy { it }
            "vans" -> listOf("ford", "chevrolet", "dodge", "chrysler", "toyota", "honda", "nissan", "volkswagen", "mercedes", "mercedes_benz").sortedBy { it }
            "convertibles" -> listOf("ford", "chevrolet", "dodge", "chrysler", "pontiac", "buick", "cadillac", "oldsmobile", "plymouth", "lincoln", "mercury", "toyota", "honda", "nissan", "mazda", "subaru", "mitsubishi", "suzuki", "daihatsu", "lexus", "infiniti", "acura", "datsun", "bmw", "mercedes", "audi", "volkswagen", "porsche", "opel", "ferrari", "lamborghini", "maserati", "pagani", "bugatti", "fiat", "alfa_romeo", "lancia", "abarth", "peugeot", "renault", "citroen", "jaguar", "land_rover", "mini", "bentley", "aston_martin", "lotus", "mclaren", "volvo", "koenigsegg", "corvette").sortedBy { it }
            "suv_trucks" -> listOf("hummer", "jeep", "ram", "gmc", "land_rover", "toyota", "honda", "nissan", "ford", "chevrolet", "dodge", "bmw", "mercedes", "mercedes_benz", "audi", "volkswagen", "porsche").sortedBy { it }
            "motorcycle" -> listOf("honda", "yamaha", "kawasaki", "suzuki", "bmw", "ducati", "harley_davidson", "indian", "triumph").sortedBy { it }
            "hot_roads" -> emptyList() // Hot Rods has no specific brands
            "mainline" -> emptyList() // No fallback to toy brands
            "premium" -> emptyList() // No fallback to toy brands
            "others" -> emptyList() // No fallback to toy brands
            "treasure_hunt" -> emptyList() // No fallback to toy brands
            "super_treasure_hunt" -> emptyList() // No toy brands
            else -> emptyList() // No fallback toy brands
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Brand",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(brands) { brand ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBrandSelected(brand) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = brand.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSubcategorySelectionView(
    categoryId: String,
    onSubcategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val subcategories = remember(categoryId) {
        when (categoryId) {
            "Car Culture" -> listOf(
                CategoryOption("modern_classics", "Modern Classics", Color(0xFF1976D2), Color.White),
                CategoryOption("race_day", "Race Day", Color(0xFFD32F2F), Color.White),
                CategoryOption("circuit_legends", "Circuit Legends", Color(0xFF388E3C), Color.White),
                CategoryOption("team_transport", "Team Transport", Color(0xFFFF9800), Color.White),
                CategoryOption("silhouettes", "Silhouettes", Color(0xFF7B1FA2), Color.White),
                CategoryOption("jay_lenos_garage", "Jay Leno's Garage", Color(0xFF0097A7), Color.White),
                CategoryOption("rtr_vehicles", "RTR Vehicles", Color(0xFF5D4037), Color.White),
                CategoryOption("real_riders", "Real Riders", Color(0xFF455A64), Color.White),
                CategoryOption("fast_wagons", "Fast Wagons", Color(0xFFE64A19), Color.White),
                CategoryOption("speed_machine", "Speed Machine", Color(0xFFC2185B), Color.White),
                CategoryOption("japan_historics", "Japan Historics", Color(0xFFD81B60), Color.White),
                CategoryOption("hammer_drop", "Hammer Drop", Color(0xFF8E24AA), Color.White),
                CategoryOption("slide_street", "Slide Street", Color(0xFF5E35B1), Color.White),
                CategoryOption("terra_trek", "Terra Trek", Color(0xFF6A1B9A), Color.White),
                CategoryOption("exotic_envy", "Exotic Envy", Color(0xFFAD1457), Color.White),
                CategoryOption("cargo_containers", "Cargo Containers", Color(0xFF00796B), Color.White)
            )
            "Pop Culture" -> listOf(
                CategoryOption("fast_and_furious", "Fast & Furious", Color.Black, Color(0xFFFFD700)),
                CategoryOption("mario_kart", "Mario Kart", Color(0xFFD32F2F), Color.White),
                CategoryOption("forza_motorsport", "Forza Motorsport", Color(0xFF1976D2), Color.White),
                CategoryOption("gran_turismo", "Gran Turismo", Color(0xFFFF5722), Color.White),
                CategoryOption("top_gun", "Top Gun", Color(0xFF424242), Color.White),
                CategoryOption("batman", "Batman", Color.Black, Color(0xFFFFEB3B)),
                CategoryOption("star_wars", "Star Wars", Color.Black, Color.White),
                CategoryOption("marvel", "Marvel", Color(0xFFD32F2F), Color.White),
                CategoryOption("jurassic_world", "Jurassic World", Color(0xFF388E3C), Color.White),
                CategoryOption("back_to_the_future", "Back to the Future", Color(0xFF0288D1), Color.White),
                CategoryOption("looney_tunes", "Looney Tunes", Color(0xFFFF9800), Color.Black)
            )
            else -> emptyList()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Select Subcategory",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subcategories) { subcategory ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubcategorySelected(subcategory.id) },
                    colors = CardDefaults.cardColors(containerColor = subcategory.backgroundColor)
                ) {
                    Text(
                        text = subcategory.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = subcategory.textColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private data class CategoryOption(
    val id: String,
    val title: String,
    val backgroundColor: Color,
    val textColor: Color
)

private fun parseFolderPath(folderPath: String?): Pair<String, String> {
    if (folderPath.isNullOrEmpty()) return Pair("Other", "Hot Wheels")
    
    // Parse folder path like "convertibles/porsche" or "rally/subaru"
    val parts = folderPath.split("/")
    val category = parts.getOrNull(0)?.replace("_", " ")?.split(" ")?.joinToString(" ") { 
        it.replaceFirstChar { char -> char.uppercaseChar() } 
    } ?: "Other"
    val brand = parts.getOrNull(1)?.replace("_", " ")?.split(" ")?.joinToString(" ") { 
        it.replaceFirstChar { char -> char.uppercaseChar() } 
    } ?: "Hot Wheels"
    
    return Pair(category, brand)
}