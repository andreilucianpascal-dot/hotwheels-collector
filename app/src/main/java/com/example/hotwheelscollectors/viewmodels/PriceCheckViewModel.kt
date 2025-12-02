package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.ui.screens.price.PriceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class PriceCheckViewModel @Inject constructor(
    private val carDao: CarDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadCarAndPrices(carId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val car = carDao.getCarById(carId)
                if (car != null) {
                    val prices = fetchPrices(car)
                    _uiState.value = UiState.Success(car, prices)
                } else {
                    _uiState.value = UiState.Error("Car not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load car: ${e.message}")
            }
        }
    }

    fun refreshPrices() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                try {
                    // Keep the current car but reload prices
                    _uiState.value = UiState.Loading
                    val newPrices = fetchPrices(currentState.car)
                    _uiState.value = UiState.Success(currentState.car, newPrices)
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Failed to refresh prices: ${e.message}")
                }
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    private suspend fun fetchPrices(car: CarEntity): List<PriceResult> =
        withContext(Dispatchers.IO) {
            val prices = mutableListOf<PriceResult>()

            try {
                // Fetch from multiple sources
                prices.addAll(fetchEbayPrices(car))
                prices.addAll(fetchAmazonPrices(car))
                prices.addAll(fetchMercariPrices(car))
                prices.addAll(fetchWhatNotPrices(car))

                // Sort by price (ascending)
                prices.sortedBy { parsePrice(it.price) }
            } catch (e: Exception) {
                // Return empty list if all fetching fails
                emptyList()
            }
        }

    private suspend fun fetchEbayPrices(car: CarEntity): List<PriceResult> {
        // In a real implementation, you would:
        // 1. Use eBay's Finding API
        // 2. Search for the car using brand, model, year, series
        // 3. Parse the JSON response

        // For now, simulate API call with realistic data
        kotlinx.coroutines.delay(1000)

        val searchTerms = buildSearchTerms(car)

        return listOf(
            PriceResult(
                title = "${car.brand} ${car.model} (${car.year}) - ${car.series}",
                seller = "eBay - collectibles_pro",
                price = "$${(15..45).random()}.${(10..99).random()}",
                url = "https://www.ebay.com/sch/i.html?_nkw=${
                    URLEncoder.encode(
                        searchTerms,
                        StandardCharsets.UTF_8.toString()
                    )
                }",
                condition = "Used",
                shipping = "+ $3.99 shipping"
            ),
            PriceResult(
                title = "${car.brand} ${car.model} ${car.year}",
                seller = "eBay - diecast_hunter",
                price = "$${(20..60).random()}.${(10..99).random()}",
                url = "https://www.ebay.com/sch/i.html?_nkw=${
                    URLEncoder.encode(
                        searchTerms,
                        StandardCharsets.UTF_8.toString()
                    )
                }",
                condition = "New",
                shipping = "Free shipping"
            )
        )
    }

    private suspend fun fetchAmazonPrices(car: CarEntity): List<PriceResult> {
        // Simulate Amazon search
        kotlinx.coroutines.delay(800)

        val searchTerms = buildSearchTerms(car)

        return listOf(
            PriceResult(
                title = "${car.brand} ${car.model} ${car.year}",
                seller = "Amazon - Mattel Store",
                price = "$${(25..55).random()}.${(10..99).random()}",
                url = "https://www.amazon.com/s?k=${
                    URLEncoder.encode(
                        searchTerms,
                        StandardCharsets.UTF_8.toString()
                    )
                }",
                condition = "New",
                shipping = "Prime delivery"
            )
        )
    }

    private suspend fun fetchMercariPrices(car: CarEntity): List<PriceResult> {
        // Simulate Mercari search
        kotlinx.coroutines.delay(600)

        return listOf(
            PriceResult(
                title = "${car.brand} ${car.model}",
                seller = "Mercari - collector123",
                price = "$${(12..35).random()}.${(10..99).random()}",
                url = "https://www.mercari.com/search/?keyword=${
                    URLEncoder.encode(
                        buildSearchTerms(
                            car
                        ), StandardCharsets.UTF_8.toString()
                    )
                }",
                condition = "Like New",
                shipping = "+ $4.99 shipping"
            )
        )
    }

    private suspend fun fetchWhatNotPrices(car: CarEntity): List<PriceResult> {
        // Simulate WhatNot (auction platform) search
        kotlinx.coroutines.delay(400)

        return listOf(
            PriceResult(
                title = "${car.series} ${car.brand} ${car.model}",
                seller = "WhatNot - hotwheels_auction",
                price = "$${(18..42).random()}.${(10..99).random()}",
                url = "https://www.whatnot.com/search?query=${
                    URLEncoder.encode(
                        buildSearchTerms(car),
                        StandardCharsets.UTF_8.toString()
                    )
                }",
                condition = "Mint",
                shipping = "Calculated"
            )
        )
    }

    private fun buildSearchTerms(car: CarEntity): String {
        val terms = mutableListOf<String>()

        // Don't add any specific brand names to search terms
        if (car.brand.isNotEmpty()) terms.add(car.brand)
        if (car.model.isNotEmpty()) terms.add(car.model)
        if (car.year > 0) terms.add(car.year.toString())
        if (car.series.isNotEmpty() && !car.series.equals("mainline", ignoreCase = true)) {
            terms.add(car.series)
        }
        if (car.number.isNotEmpty()) terms.add(car.number)

        return terms.joinToString(" ")
    }

    private fun parsePrice(priceString: String): Double {
        return try {
            priceString.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val car: CarEntity, val prices: List<PriceResult>) : UiState()
        data class Error(val message: String) : UiState()
    }
}