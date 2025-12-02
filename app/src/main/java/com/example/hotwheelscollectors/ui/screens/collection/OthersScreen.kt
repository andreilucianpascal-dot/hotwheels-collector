package com.example.hotwheelscollectors.ui.screens.collection

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.model.CarFilterState
import com.example.hotwheelscollectors.model.SortState
import com.example.hotwheelscollectors.model.ViewType
import com.example.hotwheelscollectors.model.ExportResult
import com.example.hotwheelscollectors.ui.components.*
import com.example.hotwheelscollectors.viewmodels.OthersViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun OthersScreen(
    navController: NavController,
    viewModel: OthersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCars by viewModel.selectedCars.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val sortState by viewModel.sortState.collectAsStateWithLifecycle()
    val viewType by viewModel.viewType.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OthersTopAppBar(
            selectedCount = selectedCars.size,
            onClearSelection = { viewModel.clearSelection() },
            onDeleteSelected = { showDeleteDialog = true },
            onExportSelected = { showExportDialog = true },
            onShareSelected = { showShareDialog = true },
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onFilterClick = { showFilterSheet = true },
            onSortClick = { showSortSheet = true },
            onViewTypeChange = { viewModel.updateViewType(it) },
            isSelectionMode = selectedCars.isNotEmpty(),
            onBackClick = { navController.navigateUp() }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState) {
                is OthersViewModel.UiState.Loading -> {
                    LoadingState()
                }

                is OthersViewModel.UiState.Success -> {
                    val cars = (uiState as OthersViewModel.UiState.Success).cars
                    if (cars.isEmpty() && searchQuery.isEmpty() && !filterState.isActive) {
                        EmptyCollectionState(
                            onAddClick = { navController.navigate("add_others") }
                        )
                    } else if (cars.isEmpty()) {
                        NoResultsState(
                            searchQuery = searchQuery,
                            filterState = filterState,
                            onClearFilters = {
                                viewModel.updateFilterState(CarFilterState())
                                viewModel.updateSearchQuery("")
                            }
                        )
                    } else {
                        when (viewType) {
                            ViewType.GRID -> {
                                OthersGrid(
                                    cars = cars,
                                    selectedCars = selectedCars,
                                    onCarClick = { carWithPhotos ->
                                        if (selectedCars.isEmpty()) {
                                            navController.navigate("car_details/${carWithPhotos.car.id}")
                                        } else {
                                            viewModel.toggleCarSelection(carWithPhotos.car.id)
                                        }
                                    },
                                    onCarLongClick = { carWithPhotos ->
                                        viewModel.toggleCarSelection(carWithPhotos.car.id)
                                    },
                                    contentPadding = PaddingValues(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            ViewType.LIST -> {
                                OthersList(
                                    cars = cars,
                                    selectedCars = selectedCars,
                                    onCarClick = { carWithPhotos ->
                                        if (selectedCars.isEmpty()) {
                                            navController.navigate("car_details/${carWithPhotos.car.id}")
                                        } else {
                                            viewModel.toggleCarSelection(carWithPhotos.car.id)
                                        }
                                    },
                                    onCarLongClick = { carWithPhotos ->
                                        viewModel.toggleCarSelection(carWithPhotos.car.id)
                                    },
                                    contentPadding = PaddingValues(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                is OthersViewModel.UiState.Error -> {
                    ErrorState(
                        message = (uiState as OthersViewModel.UiState.Error).message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }

            // Floating Action Button
            if (selectedCars.isEmpty()) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_others") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add other car")
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            onFilterStateChange = { viewModel.updateFilterState(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            sortState = sortState,
            onSortStateChange = { viewModel.updateSortState(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                scope.launch {
                    viewModel.deleteSelectedCars()
                    snackbarHostState.showSnackbar(
                        message = "${selectedCars.size} cars deleted"
                    )
                    viewModel.clearSelection()
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onExport = { format ->
                scope.launch {
                    val result = viewModel.exportSelectedCars()
                    when (result) {
                        is ExportResult.Success -> {
                            snackbarHostState.showSnackbar(
                                message = "Export successful"
                            )
                        }
                        is ExportResult.Error -> {
                            snackbarHostState.showSnackbar(
                                message = "Export failed"
                            )
                        }
                    }
                    viewModel.clearSelection()
                }
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showShareDialog) {
        ShareDialog(
            onShare = { uri ->
                scope.launch {
                    val firstCarId = selectedCars.firstOrNull()
                    if (firstCarId != null) {
                        navController.navigate("share/$firstCarId")
                    }
                    viewModel.clearSelection()
                }
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun OthersTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onShareSelected: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onViewTypeChange: (ViewType) -> Unit,
    isSelectionMode: Boolean,
    onBackClick: () -> Unit,
) {
    var showSearch by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else if (!showSearch) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (isSelectionMode) {
                Text(
                    text = "$selectedCount items selected",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            } else if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            } else {
                Text(
                    text = "Others",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isSelectionMode) {
                IconButton(onClick = onShareSelected) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share selected",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onExportSelected) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export selected",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Toggle search",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                ViewTypeMenu(
                    onViewTypeChange = onViewTypeChange
                )
            }
        }
    }
}

@Composable
private fun ViewTypeMenu(
    onViewTypeChange: (ViewType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.ViewModule, "Change view")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Grid View") },
                leadingIcon = {
                    Icon(Icons.Default.GridView, null)
                },
                onClick = {
                    onViewTypeChange(ViewType.GRID)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("List View") },
                leadingIcon = {
                    Icon(Icons.Default.ViewList, null)
                },
                onClick = {
                    onViewTypeChange(ViewType.LIST)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun OthersGrid(
    cars: List<CarWithPhotos>,
    selectedCars: Set<String>,
    onCarClick: (CarWithPhotos) -> Unit,
    onCarLongClick: (CarWithPhotos) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier
    ) {
        items(
            items = cars,
            key = { carWithPhotos -> carWithPhotos.car.id }
        ) { carWithPhotos ->
            OtherCarCard(
                car = carWithPhotos,
                isSelected = carWithPhotos.car.id in selectedCars,
                onClick = { onCarClick(carWithPhotos) },
                onLongClick = { onCarLongClick(carWithPhotos) },
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun OthersList(
    cars: List<CarWithPhotos>,
    selectedCars: Set<String>,
    onCarClick: (CarWithPhotos) -> Unit,
    onCarLongClick: (CarWithPhotos) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        items(
            items = cars,
            key = { carWithPhotos -> carWithPhotos.car.id }
        ) { carWithPhotos ->
            OtherCarListItem(
                car = carWithPhotos,
                isSelected = carWithPhotos.car.id in selectedCars,
                onClick = { onCarClick(carWithPhotos) },
                onLongClick = { onCarLongClick(carWithPhotos) },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OtherCarCard(
    car: CarWithPhotos,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Column {
                if (car.photos.isNotEmpty()) {
                    val photo = car.photos.first()
                    val photoPath = if (!photo.localPath.isNullOrEmpty() && File(photo.localPath).exists()) {
                        photo.localPath
                    } else if (!photo.cloudPath.isNullOrEmpty()) {
                        photo.cloudPath
                    } else {
                        photo.localPath ?: "" // Fallback to empty string if null
                    }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoPath)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .crossfade(true)
                            .build(),
                        contentDescription = "${car.car.brand} ${car.car.model}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (car.car.isPremium) {
                            // For Premium: Show series and model (brand is empty)
                            "${car.car.series} - ${car.car.model.ifEmpty { "Edit Model" }}"
                        } else {
                            // For Others: Show brand and model
                            "${car.car.brand} ${car.car.model}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = car.car.series ?: "Unknown Series",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "#${car.car.number ?: "N/A"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = car.car.year?.toString() ?: "N/A",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherCarListItem(
    car: CarWithPhotos,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable { onClick() },
        leadingContent = {
            Box {
                if (car.photos.isNotEmpty()) {
                    val photo = car.photos.first()
                    val photoPath = if (!photo.localPath.isNullOrEmpty() && File(photo.localPath).exists()) {
                        photo.localPath
                    } else if (!photo.cloudPath.isNullOrEmpty()) {
                        photo.cloudPath
                    } else {
                        photo.localPath ?: "" // Fallback to empty string if null
                    }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoPath)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ CACHE ACTIVAT
                            .crossfade(true)
                            .build(),
                        contentDescription = "${car.car.brand} ${car.car.model}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp)
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = "${car.car.brand} ${car.car.model}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = car.car.series ?: "Unknown Series",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "#${car.car.number ?: "N/A"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = car.car.year?.toString() ?: "N/A",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun EmptyCollectionState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Other Cars Yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start building your collection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddClick
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Your Car")
        }
    }
}

@Composable
private fun NoResultsState(
    searchQuery: String,
    filterState: CarFilterState,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
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
            text = if (searchQuery.isNotEmpty()) {
                "No results for \"$searchQuery\""
            } else {
                "No cars match your filters"
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        if (filterState.isActive) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Active filters applied",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClearFilters
            ) {
                Icon(
                    Icons.Default.FilterAltOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Clear Filters")
            }
        }
    }
}