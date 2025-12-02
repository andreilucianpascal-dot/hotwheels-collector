package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.SearchHistoryDao
import com.example.hotwheelscollectors.data.local.entities.SearchHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val carDao: CarDao,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) {
                carDao.searchCars(query)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentSearches = searchHistoryDao.getRecentSearches(userId = "", limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val popularSearches = searchHistoryDao.getPopularSearches(userId = "", limit = 5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.length >= 2) {
                searchHistoryDao.insertSearch(
                    SearchHistoryEntity(
                        id = UUID.randomUUID().toString(),
                        userId = "",
                        query = query,
                        resultCount = searchResults.value.size,
                        timestamp = Date()
                    )
                )
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearSearchHistory(userId = "")
        }
    }

    fun getSuggestions(prefix: String): Flow<List<String>> {
        return searchHistoryDao.getSuggestedSearches(userId = "", prefix = prefix)
            .map { searches -> searches.map { it.query } }
    }
}