package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val carDao: CarDao,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow("name")
    private val _filterPremium = MutableStateFlow(false)
    private val _currentUserId = MutableStateFlow("")

    init {
        // Get actual user ID from authentication
        val currentUser = authRepository.getCurrentUser()
        _currentUserId.value = currentUser?.uid ?: ""
        android.util.Log.d("CollectionViewModel", "Initialized with user ID: ${_currentUserId.value}")
        
        // Observe authentication state changes
        viewModelScope.launch {
            authRepository.authStateChanges().collect { user ->
                val userId = user?.uid ?: ""
                android.util.Log.d("CollectionViewModel", "Auth state changed, user ID: $userId")
                _currentUserId.value = userId
            }
        }
    }

    val cars = combine(
        _sortOrder,
        _filterPremium,
        _currentUserId
    ) { sortOrder, filterPremium, userId ->
        Triple(sortOrder, filterPremium, userId)
    }.flatMapLatest { (_, filterPremium, userId) ->
        if (userId.isNotEmpty()) {
            carDao.getCarsByType(userId = userId, isPremium = filterPremium)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pagedCars = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true,
            maxSize = 100
        )
    ) {
        CarPagingSource(carDao, _currentUserId.value, _sortOrder.value)
    }.flow.cachedIn(viewModelScope)

    val localCars = _currentUserId.flatMapLatest { userId ->
        if (userId.isNotEmpty()) {
            android.util.Log.d("CollectionViewModel", "Loading cars for user: $userId")
            carDao.getCarsForUser(userId)
        } else {
            android.util.Log.d("CollectionViewModel", "No user ID, returning empty list")
            flowOf(emptyList())
        }
    }
        .distinctUntilChanged()
        .onEach { cars ->
            android.util.Log.d("CollectionViewModel", "localCars emitted: ${cars.size}")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val collectionStats = _currentUserId.flatMapLatest { userId ->
        if (userId.isNotEmpty()) {
            carDao.getCollectionStats(userId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun updatePremiumFilter(showPremium: Boolean) {
        _filterPremium.value = showPremium
    }

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
        android.util.Log.d("CollectionViewModel", "User ID updated to: $userId")
    }

    fun refreshUserData() {
        val currentUser = authRepository.getCurrentUser()
        _currentUserId.value = currentUser?.uid ?: ""
        android.util.Log.d("CollectionViewModel", "User data refreshed. Current user ID: ${_currentUserId.value}")
    }

    fun toggleGridView() {
        viewModelScope.launch {
            userPreferences.updateGridView(!userPreferences.gridViewEnabled.first())
        }
    }
}

// Custom PagingSource for cars
class CarPagingSource(
    private val carDao: CarDao,
    private val userId: String,
    private val sortOrder: String
) : PagingSource<Int, CarEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CarEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            // Get cars for the current page
            val cars = carDao.getCarsForUser(userId).first()

            // Apply sorting and pagination
            val sortedCars = when (sortOrder) {
                "name" -> cars.sortedBy { it.model }
                "brand" -> cars.sortedBy { it.brand }
                "year" -> cars.sortedBy { it.year }
                "date" -> cars.sortedByDescending { it.timestamp }
                else -> cars.sortedBy { it.model }
            }

            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, sortedCars.size)

            if (startIndex >= sortedCars.size) {
                LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            } else {
                val pageData = sortedCars.subList(startIndex, endIndex)
                LoadResult.Page(
                    data = pageData,
                    prevKey = if (page > 0) page - 1 else null,
                    nextKey = if (endIndex < sortedCars.size) page + 1 else null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CarEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
