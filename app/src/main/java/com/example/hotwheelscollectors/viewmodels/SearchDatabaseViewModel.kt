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
class SearchDatabaseViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allCars = MutableStateFlow<List<GlobalCarData>>(emptyList())

    init {
        loadAllCars()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCars()
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        filterCars()
    }

    private fun loadAllCars() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                // Load all cars from all categories in Browse
                val mainlineCars = repository.getGlobalMainlineCars()
                val premiumCars = repository.getGlobalPremiumCars()
                val treasureHuntCars = repository.getGlobalTreasureHuntCars()
                val superTreasureHuntCars = repository.getGlobalSuperTreasureHuntCars()
                val silverSeriesCars = repository.getGlobalSilverSeriesCars()
                val othersCars = repository.getGlobalOtherCars()
                
                val allCars = mainlineCars + premiumCars + treasureHuntCars + 
                              superTreasureHuntCars + silverSeriesCars + othersCars
                
                _allCars.value = allCars
                filterCars()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load cars")
            }
        }
    }

    private fun filterCars() {
        val query = _searchQuery.value.trim().lowercase()
        val allCars = _allCars.value

        // ✅ FIX: Don't show all cars when query is empty - show empty state instead
        val filtered = if (query.isEmpty()) {
            emptyList() // Show empty state until user types something
        } else {
            allCars.filter { car ->
                car.carName.lowercase().contains(query) ||
                car.brand.lowercase().contains(query) ||
                car.series.lowercase().contains(query) ||
                car.color.lowercase().contains(query) ||
                car.year.toString().contains(query) ||
                car.barcode.lowercase().contains(query) ||
                car.category.lowercase().contains(query) ||
                car.subcategory.lowercase().contains(query)
            }
        }

        _uiState.value = UiState.Success(filtered)
    }

    fun search() {
        filterCars()
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val results: List<GlobalCarData>) : UiState()
        data class Error(val message: String) : UiState()
    }
}