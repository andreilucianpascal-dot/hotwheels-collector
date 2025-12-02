package com.example.hotwheelscollectors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hotwheelscollectors.model.FilterState
import com.example.hotwheelscollectors.model.SortState
import com.example.hotwheelscollectors.ui.components.*

// MainlinesScreen-specific components that are NOT in CollectionComponents.kt

@Composable
fun MainlinesHeader(
    title: String,
    onBackClick: () -> Unit
) {
    androidx.compose.material.TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
fun MainlinesToolbar(
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onFilterClick) {
            Text("Filter")
        }

        Button(onClick = onSortClick) {
            Text("Sort")
        }

        Button(onClick = onSearchClick) {
            Text("Search")
        }
    }
}

@Composable
fun MainlinesCarCard(
    car: com.example.hotwheelscollectors.model.HotWheelsCar,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = car.name ?: "Unknown Car",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Year: ${car.year ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Number: ${car.number ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun MainlinesEmptyState(
    message: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}