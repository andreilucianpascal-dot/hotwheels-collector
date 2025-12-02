package com.example.hotwheelscollectors.ui.components.infinitescroll

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

/**
 * A professional infinite scroll list component that handles paging data with loading states,
 * error handling, and retry functionality.
 *
 * @param items The Flow of PagingData to display
 * @param modifier Modifier to be applied to the LazyColumn
 * @param key Optional key function for item identification and efficient recomposition
 * @param itemContent Composable function to render each item
 * @param T The type of items to display
 */
@Composable
fun <T : Any> InfiniteScrollList(
    items: Flow<PagingData<T>>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val lazyItems = items.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier
    ) {
        // Main content items
        items(
            count = lazyItems.itemCount,
            key = if (key != null) { index ->
                val item = lazyItems[index]
                if (item != null) key(item) else index
            } else null
        ) { index ->
            lazyItems[index]?.let { item ->
                itemContent(item)
            }
        }

        // Loading and error states
        lazyItems.apply {
            when {
                // Initial loading state
                loadState.refresh is LoadState.Loading -> {
                    item {
                        LoadingIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Pagination loading state
                loadState.append is LoadState.Loading -> {
                    item {
                        LoadingIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Initial error state
                loadState.refresh is LoadState.Error -> {
                    item {
                        ErrorItem(
                            message = (loadState.refresh as LoadState.Error).error.message,
                            onRetry = { retry() },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Pagination error state
                loadState.append is LoadState.Error -> {
                    item {
                        ErrorItem(
                            message = (loadState.append as LoadState.Error).error.message,
                            onRetry = { retry() },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading indicator component for infinite scroll states.
 *
 * @param modifier Modifier to be applied to the Box
 */
@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Error item component for displaying error states with retry functionality.
 *
 * @param message The error message to display
 * @param onRetry Callback function for retry action
 * @param modifier Modifier to be applied to the Column
 */
@Composable
private fun ErrorItem(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "An error occurred while loading content",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}