package com.example.hotwheelscollectors.ui.screens.browse

import androidx.compose.foundation.background
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
import android.net.Uri
import androidx.compose.foundation.clickable
import com.example.hotwheelscollectors.ui.theme.HotWheelsThemeManager
import com.example.hotwheelscollectors.viewmodels.BrowsePremiumViewModel
import com.example.hotwheelscollectors.viewmodels.AddFromBrowseViewModel
import com.example.hotwheelscollectors.viewmodels.AddFromBrowseUiState
import com.example.hotwheelscollectors.utils.PriceSearchHelper
import android.content.Intent

@Composable
fun BrowsePremiumScreen(
    navController: NavController,
    viewModel: BrowsePremiumViewModel = hiltViewModel(),
    addFromBrowseViewModel: AddFromBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredCars by viewModel.filteredCars.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val addUiState by addFromBrowseViewModel.uiState.collectAsState()
    
    val currentUiState = uiState
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Check for barcode from barcode scanner
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val searchBarcode = savedStateHandle?.get<String>("search_barcode")
    
    LaunchedEffect(searchBarcode) {
        searchBarcode?.let { barcode ->
            viewModel.updateSearchQuery(barcode)
            // Clear the barcode from savedStateHandle after using it
            savedStateHandle?.remove<String>("search_barcode")
        }
    }
    
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

    val themeState by com.example.hotwheelscollectors.viewmodels.AppThemeViewModel::class
        .let { hiltViewModel<com.example.hotwheelscollectors.viewmodels.AppThemeViewModel>() }
        .uiState
        .collectAsState()
    val bgTheme = HotWheelsThemeManager.getBackgroundTheme(themeState.colorScheme)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Premium - Global Database") },
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
                .background(
                    brush = bgTheme?.secondaryGradient
                        ?: androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                )
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search premium cars...") },
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
                                    text = if (searchQuery.isEmpty()) "No premium cars found in global database" else "No cars match your search",
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
                                com.example.hotwheelscollectors.ui.components.BrowseGlobalCarCard(
                                    car = car,
                                    addUiState = addUiState,
                                    onAddToCollection = { addFromBrowseViewModel.addCarToCollection(car) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}