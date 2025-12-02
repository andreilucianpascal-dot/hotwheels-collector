package com.example.hotwheelscollectors.ui.screens.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.viewmodels.BrowseOthersViewModel
import com.example.hotwheelscollectors.viewmodels.AddFromBrowseViewModel
import com.example.hotwheelscollectors.viewmodels.AddFromBrowseUiState

@Composable
fun BrowseOthersScreen(
    navController: NavController,
    viewModel: BrowseOthersViewModel = hiltViewModel(),
    addFromBrowseViewModel: AddFromBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredCars by viewModel.filteredCars.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val addUiState by addFromBrowseViewModel.uiState.collectAsState()
    
    val currentUiState = uiState
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(addUiState) {
        when (val state = addUiState) {
            is AddFromBrowseUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                addFromBrowseViewModel.resetState()
            }
            is AddFromBrowseUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                addFromBrowseViewModel.resetState()
            }
            else -> {}
        }
    }
    
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Others - Global Database") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search other cars...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            when (currentUiState) {
                is com.example.hotwheelscollectors.viewmodels.BrowseUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.example.hotwheelscollectors.viewmodels.BrowseUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentUiState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is com.example.hotwheelscollectors.viewmodels.BrowseUiState.Success -> {
                    if (filteredCars.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = "No cars found",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "No other cars found in global database" else "No cars match your search",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCars) { car ->
                                val context = LocalContext.current
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { 
                                        // Navigate to car details or add to collection
                                        // Implementation will be added in future update
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (car.frontPhotoUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(car.frontPhotoUrl)
                                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Car Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = car.carName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${car.brand} - ${car.series} (${car.year})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (car.color.isNotEmpty()) {
                                                Text(
                                                    text = "Color: ${car.color}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Verified by ${car.verificationCount} users",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { addFromBrowseViewModel.addCarToCollection(car) },
                                                enabled = addUiState !is AddFromBrowseUiState.Loading,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (addUiState is AddFromBrowseUiState.Loading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Add, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Add to Collection")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}