package com.example.hotwheelscollectors.ui.screens.price

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.viewmodels.PriceCheckViewModel
import kotlinx.coroutines.launch

@Composable
fun PriceCheckScreen(
    navController: NavController,
    carId: String?,
    viewModel: PriceCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(carId) {
        if (carId != null) {
            viewModel.loadCarAndPrices(carId)
        } else {
            viewModel.setError("Car not found")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom TopAppBar using stable components
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
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Price Check",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (uiState is PriceCheckViewModel.UiState.Success) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.refreshPrices()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is PriceCheckViewModel.UiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Loading prices...")
                    }
                }

                is PriceCheckViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = {
                            if (carId != null) {
                                viewModel.loadCarAndPrices(carId)
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PriceCheckViewModel.UiState.Success -> {
                    if (state.prices.isEmpty()) {
                        NoPricesState(
                            car = state.car,
                            onSearchManually = {
                                val searchQuery =
                                    "${state.car.brand} ${state.car.model} ${state.car.year}"
                                val encodedQuery = Uri.encode(searchQuery)
                                val searchUrl = "https://www.google.com/search?q=$encodedQuery"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        PriceList(
                            car = state.car,
                            prices = state.prices,
                            onPriceClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun NoPricesState(
    car: CarEntity,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MoneyOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No Prices Found",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "We couldn't find any current prices for this car",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Car details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${car.brand} ${car.model}",
                    style = MaterialTheme.typography.titleMedium
                )

                if (car.series.isNotEmpty()) {
                    Text(
                        text = car.series,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${car.year}${if (car.color.isNotEmpty()) " - ${car.color}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (car.number.isNotEmpty()) {
                    Text(
                        text = "Number: ${car.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSearchManually,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Search Manually")
        }
    }
}

@Composable
private fun PriceList(
    car: CarEntity,
    prices: List<PriceResult>,
    onPriceClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Car details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${car.brand} ${car.model}",
                    style = MaterialTheme.typography.titleMedium
                )

                if (car.series.isNotEmpty()) {
                    Text(
                        text = car.series,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${car.year}${if (car.color.isNotEmpty()) " - ${car.color}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (car.number.isNotEmpty()) {
                    Text(
                        text = "Number: ${car.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Found ${prices.size} listings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // Prices
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(prices) { price ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPriceClick(price.url) }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(price.title)
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    price.seller,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (price.condition.isNotEmpty()) {
                                    Text(
                                        "Condition: ${price.condition}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    price.price,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (price.shipping.isNotEmpty()) {
                                    Text(
                                        price.shipping,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

data class PriceResult(
    val title: String,
    val seller: String,
    val price: String,
    val url: String,
    val condition: String = "",
    val shipping: String = "",
)