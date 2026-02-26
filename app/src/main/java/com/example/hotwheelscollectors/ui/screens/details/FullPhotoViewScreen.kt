package com.example.hotwheelscollectors.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Full immersive photo viewer with zoom and "View Details" button
 * - Full screen (100% ecran, hide system bars)
 * - Zoomable photo
 * - "View Details" button centered at bottom
 */
@Composable
fun FullPhotoViewScreen(
    photoUri: String,
    carId: String,
    navController: NavController
) {
    // Decode photoUri from URL encoding and convert to proper format
    val decodedPhotoUri = try {
        java.net.URLDecoder.decode(photoUri, "UTF-8")
    } catch (e: Exception) {
        photoUri // Fallback to original if decoding fails
    }
    
    // ✅ FIX: Convert to File if it's a local path, or keep as String for URL
    val imageData = try {
        val file = java.io.File(decodedPhotoUri)
        if (file.exists()) {
            file // Use File for local paths
        } else {
            decodedPhotoUri // Use String for URLs or non-existent paths
        }
    } catch (e: Exception) {
        decodedPhotoUri // Fallback to String if File conversion fails
    }
    // Full immersive mode - hide system bars
    val view = LocalView.current
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        // Hide system bars for full immersive experience
        val activity = context as? android.app.Activity
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowInsetsControllerCompat(window, view)
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Restore system bars when leaving screen
            val activity = context as? android.app.Activity
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    var scale by remember { mutableFloatStateOf(1f) }
    
    // ✅ FIX: Only allow zoom (pinch to zoom), no pan/drag - image stays fixed/centered
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        // Ignore offsetChange - image stays fixed, only zoom works
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Zoomable Photo (100% screen)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageData) // ✅ FIX: Use imageData (File or String) instead of decodedPhotoUri
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = "Full size car photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = 0f,  // ✅ Fixed - no movement
                    translationY = 0f   // ✅ Fixed - no movement
                )
                .transformable(transformableState)
        )
        
        // Back button (top left, minimal)
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(50)
                )
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // "View Details" button (centered, bottom)
        Button(
            onClick = {
                navController.navigate("car_details/$carId")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "View Details",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

