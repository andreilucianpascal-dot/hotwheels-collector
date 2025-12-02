// LoadingAnimations.kt
package com.example.hotwheelscollectors.ui.components.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ) {
            LoadingScreen()
        }
    }
}

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = modifier
            .alpha(alpha),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {}
}

@Composable
fun LoadingItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerLoading(
            modifier = Modifier
                .size(60.dp)
                .padding(end = 8.dp)
        )
        Column {
            ShimmerLoading(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .padding(bottom = 4.dp)
            )
            ShimmerLoading(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(20.dp)
            )
        }
    }
}