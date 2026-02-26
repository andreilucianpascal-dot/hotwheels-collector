package com.example.hotwheelscollectors.ui.screens.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hotwheelscollectors.R
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import com.example.hotwheelscollectors.viewmodels.SearchDatabaseViewModel
import com.example.hotwheelscollectors.ui.components.LoadingState
import com.example.hotwheelscollectors.ui.components.cards.CarSearchCard
import com.example.hotwheelscollectors.ui.components.cards.BrowseCarSearchCard

@Composable
fun SearchDatabaseScreen(
    navController: NavController,
    viewModel: SearchDatabaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text(stringResource(R.string.search_database)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearSearchQuery) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            is SearchDatabaseViewModel.UiState.Loading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SearchDatabaseViewModel.UiState.Success -> {
                val results = (uiState as SearchDatabaseViewModel.UiState.Success).results
                // ✅ FIX: Show empty state when query is empty (not just when no results)
                if (searchQuery.isBlank()) {
                    EmptySearchState(
                        modifier = Modifier.padding(paddingValues)
                    )
                } else if (results.isEmpty()) {
                    EmptyResultsState(
                        query = searchQuery,
                        onClearSearch = viewModel::clearSearchQuery,
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    BrowseSearchResults(
                        results = results,
                        onResultClick = { car ->
                            // Navigate to appropriate Browse screen based on category
                            val browseRoute = when {
                                car.category.lowercase().contains("premium") -> "browse_premium"
                                car.category.lowercase().contains("treasure") && car.category.lowercase().contains("super") -> "browse_super_treasure_hunt"
                                car.category.lowercase().contains("treasure") -> "browse_treasure_hunt"
                                car.series.lowercase().contains("silver series") || car.category.lowercase().contains("silver series") -> "browse_silver_series"
                                car.category.lowercase().contains("other") || car.series.lowercase() == "others" -> "browse_others"
                                else -> "browse_mainlines"
                            }
                            // Pass barcode to filter
                            navController.currentBackStackEntry?.savedStateHandle?.set("search_barcode", car.barcode)
                            navController.navigate(browseRoute)
                        },
                        contentPadding = paddingValues
                    )
                }
            }
            is SearchDatabaseViewModel.UiState.Error -> {
                ErrorState(
                    message = (uiState as SearchDatabaseViewModel.UiState.Error).message,
                    onRetry = viewModel::search,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<CarEntity>,
    onResultClick: (CarEntity) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = results,
            key = { it.id }
        ) { car ->
            CarSearchCard(
                car = car,
                onClick = { onResultClick(car) }
            )
        }
    }
}

@Composable
private fun BrowseSearchResults(
    results: List<GlobalCarData>,
    onResultClick: (GlobalCarData) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = results,
            key = { "${it.barcode}-${it.carName}" }
        ) { car ->
            BrowseCarSearchCard(
                car = car,
                onClick = { onResultClick(car) }
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search Global Database",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Type to search for cars by name, brand, year, barcode, or series",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyResultsState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_results_found, query),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.try_different_search),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClearSearch
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.clear_search))
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.retry))
        }
    }
}