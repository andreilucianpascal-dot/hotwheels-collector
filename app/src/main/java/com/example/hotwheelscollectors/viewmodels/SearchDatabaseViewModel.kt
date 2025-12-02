package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    init {
        search()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        search()
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        search()
    }

    fun search() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val query = _searchQuery.value.trim()
                val results = if (query.isEmpty()) {
                    // Get all cars for the current user
                    repository.getCarsForUser(repository.userId).first()
                } else {
                    // Use the repository's search method
                    repository.searchCars(repository.userId, query).first()
                }

                _uiState.value = UiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val results: List<CarEntity>) : UiState()
        data class Error(val message: String) : UiState()
    }
}