package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * SafeLazyColumn - A wrapper around LazyColumn that prevents Content Capture crashes
 * by adding proper semantics and error boundaries.
 */
@Composable
fun SafeLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical =
        if (!reverseLayout) androidx.compose.foundation.layout.Arrangement.Top else androidx.compose.foundation.layout.Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: androidx.compose.foundation.gestures.FlingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    contentDescription: String = "Scrollable list",
    content: LazyListScope.() -> Unit,
) {
    // Add error boundary and semantics to prevent Content Capture crashes
    LaunchedEffect(Unit) {
        // Pre-initialize scroll state to prevent scope issues
        try {
            state.scrollToItem(0)
        } catch (e: Exception) {
            // Ignore initialization errors
        }
    }

    LazyColumn(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}