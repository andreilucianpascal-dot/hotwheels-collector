package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowsePremiumViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val _cars = MutableStateFlow<List<GlobalCarData>>(emptyList())
    val cars: StateFlow<List<GlobalCarData>> = _cars.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredCars = MutableStateFlow<List<GlobalCarData>>(emptyList())
    val filteredCars: StateFlow<List<GlobalCarData>> = _filteredCars.asStateFlow()

    init {
        loadGlobalCars()
    }

    fun loadGlobalCars() {
        viewModelScope.launch {
            try {
                _uiState.value = BrowseUiState.Loading
                val globalCars = firestoreRepository.getGlobalPremiumCars()
                _cars.value = globalCars
                _filteredCars.value = globalCars
                _uiState.value = BrowseUiState.Success
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to load global premium cars: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCars()
    }

    private fun filterCars() {
        val query = _searchQuery.value.lowercase()
        val allCars = _cars.value

        _filteredCars.value = if (query.isEmpty()) {
            allCars
        } else {
            allCars.filter { car ->
                // For barcode, use exact match to find all variants with same barcode
                // For other fields, use contains for flexible search
                car.barcode.lowercase() == query ||  // Exact match for barcode
                car.carName.lowercase().contains(query) ||
                car.brand.lowercase().contains(query) ||
                car.series.lowercase().contains(query) ||
                car.color.lowercase().contains(query) ||
                car.year.toString().contains(query)
            }
        }
    }

    fun refresh() {
        loadGlobalCars()
    }
}
