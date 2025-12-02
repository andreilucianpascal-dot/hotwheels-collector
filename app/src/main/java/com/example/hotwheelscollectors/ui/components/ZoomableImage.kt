package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 🔍 Zoomable Image Dialog with Photo Actions
 * 
 * Features:
 * - Pinch to zoom
 * - Pan/drag
 * - Share photo
 * - Download photo
 * - Copy photo
 */
@Composable
fun ZoomableImageDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit = {},
    onDownload: (Uri) -> Unit = {},
    onCopy: (Uri) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ZoomableImageContent(
            imageUri = imageUri,
            onDismiss = onDismiss,
            onShare = onShare,
            onDownload = onDownload,
            onCopy = onCopy
        )
    }
}

@Composable
private fun ZoomableImageContent(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit,
    onDownload: (Uri) -> Unit,
    onCopy: (Uri) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += offsetChange.x
        offsetY += offsetChange.y
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Zoomable Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                .crossfade(true)
                .build(),
            contentDescription = "Zoomable car photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .transformable(transformableState)
        )
        
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Button
                IconButton(
                    onClick = { onShare(imageUri) },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                
                // Download Button
                IconButton(
                    onClick = { onDownload(imageUri) },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Reset zoom hint
        if (scale != 1f || offsetX != 0f || offsetY != 0f) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "Tap to reset zoom",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
