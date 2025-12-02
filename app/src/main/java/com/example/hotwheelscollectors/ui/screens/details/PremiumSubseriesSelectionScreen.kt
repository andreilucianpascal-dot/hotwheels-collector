package com.example.hotwheelscollectors.ui.screens.selection

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hotwheelscollectors.ui.components.LoadingState
import com.example.hotwheelscollectors.ui.components.error.ErrorScreen
import com.example.hotwheelscollectors.viewmodels.CollectionViewModel

@Composable
fun PremiumSubseriesSelectionScreen(
    navController: NavController,
    viewModel: CollectionViewModel = viewModel()
) {
    // 🔑 FIX: Explicitly specify the type
    val cars: List<com.example.hotwheelscollectors.data.local.entities.CarEntity> by viewModel.cars.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🔑 FIX: Extract unique subseries from existing cars with proper variable names
    val subseries = cars
        .filter { car -> car.isPremium && car.subseries.isNotEmpty() }
        .map { car -> car.subseries }
        .distinct()
        .sorted()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Simulate loading
            kotlinx.coroutines.delay(500)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Premium Subseries") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar as content instead of BottomAppBar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 3.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search subseries...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
            }

            // Content
            when {
                isLoading -> {
                    LoadingState()
                }

                errorMessage != null -> {
                    ErrorScreen(
                        message = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                            isLoading = true
                            // Retry logic here
                        }
                    )
                }
                subseries.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    val filteredSubseries = if (searchQuery.isNotEmpty()) {
                        subseries.filter { series ->
                            series.contains(
                                searchQuery,
                                ignoreCase = true
                            )
                        }
                    } else {
                        subseries
                    }

                    if (filteredSubseries.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSubseries) { series ->
                                SubseriesItem(
                                    subseries = series,
                                    onClick = { navController.navigate("premium_cars/$series") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubseriesItem(
    subseries: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(subseries) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No premium subseries found",
            style = MaterialTheme.typography.titleMedium
        )
    }
}