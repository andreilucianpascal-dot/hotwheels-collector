package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.model.CarFilterState
import com.example.hotwheelscollectors.model.SortState
import com.example.hotwheelscollectors.model.ViewType
import com.example.hotwheelscollectors.model.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainlineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val cars: List<CarWithPhotos>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedCars = MutableStateFlow<Set<String>>(emptySet())
    val selectedCars: StateFlow<Set<String>> = _selectedCars.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(CarFilterState())
    val filterState: StateFlow<CarFilterState> = _filterState.asStateFlow()

    private val _sortState = MutableStateFlow(SortState.NAME_ASC)
    val sortState: StateFlow<SortState> = _sortState.asStateFlow()

    private val _viewType = MutableStateFlow(ViewType.GRID)
    val viewType: StateFlow<ViewType> = _viewType.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Raw data from database
    private val _allMainlineCars = MutableStateFlow<List<CarWithPhotos>>(emptyList())

    init {
        loadCars()
        observeFilteredCars()
    }

    fun loadCars() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val userId = getCurrentUserId()
                android.util.Log.d("MainlineViewModel", "=== LOADING CARS ===")
                android.util.Log.d("MainlineViewModel", "User ID: $userId")

                // Load Mainline cars (isPremium = false)
                carDao.getCarsWithPhotos(userId).collect { allCars ->
                    android.util.Log.d("MainlineViewModel", "Received ${allCars.size} cars from DB")
                    android.util.Log.d("MainlineViewModel", "Cars details:")
                    allCars.forEachIndexed { index, carWithPhotos ->
                        android.util.Log.d("MainlineViewModel", "  [$index] ID: ${carWithPhotos.car.id}, Model: ${carWithPhotos.car.model}, Series: ${carWithPhotos.car.series}, isPremium: ${carWithPhotos.car.isPremium}")
                    }
                    
                    val mainlineCars = allCars.filter { carWithPhotos ->
                        !carWithPhotos.car.isPremium
                    }
                    android.util.Log.d("MainlineViewModel", "Filtered to ${mainlineCars.size} mainline cars (isPremium = false)")
                    
                    _allMainlineCars.value = mainlineCars
                }
            } catch (e: Exception) {
                android.util.Log.e("MainlineViewModel", "❌ Error loading cars: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Failed to load mainline cars")
            }
        }
    }

    private fun observeFilteredCars() {
        viewModelScope.launch {
            combine(
                _allMainlineCars,
                _searchQuery,
                _filterState,
                _sortState
            ) { cars, query, filters, sort ->
                var filteredCars = cars

                // Apply search filter
                if (query.isNotEmpty()) {
                    filteredCars = filteredCars.filter { carWithPhotos ->
                        val car = carWithPhotos.car
                        car.model.contains(query, ignoreCase = true) ||
                                car.brand.contains(query, ignoreCase = true) ||
                                car.series.contains(query, ignoreCase = true) ||
                                car.color.contains(query, ignoreCase = true) ||
                                car.notes.contains(query, ignoreCase = true) ||
                                car.subseries.contains(query, ignoreCase = true)
                    }
                }

                // Apply filters
                filteredCars = applyFilters(filteredCars, filters)

                // Apply sorting
                filteredCars = applySorting(filteredCars, sort)

                filteredCars
            }.collect { filteredCars ->
                _uiState.value = UiState.Success(filteredCars)
            }
        }
    }

    private fun applyFilters(
        cars: List<CarWithPhotos>,
        filters: CarFilterState,
    ): List<CarWithPhotos> {
        var filteredCars = cars

        // Filter by year range
        if (filters.minYear != null) {
            filteredCars = filteredCars.filter { it.car.year >= filters.minYear }
        }
        if (filters.maxYear != null) {
            filteredCars = filteredCars.filter { it.car.year <= filters.maxYear }
        }

        // Filter by series (mainline subseries like "Rally", "Convertibles", etc.)
        if (!filters.series.isNullOrEmpty()) {
            filteredCars = filteredCars.filter { carWithPhotos ->
                carWithPhotos.car.series.contains(filters.series, ignoreCase = true) ||
                        carWithPhotos.car.subseries.contains(filters.series, ignoreCase = true)
            }
        }

        // Filter by color
        if (!filters.color.isNullOrEmpty()) {
            filteredCars = filteredCars.filter { carWithPhotos ->
                carWithPhotos.car.color.contains(filters.color, ignoreCase = true)
            }
        }

        return filteredCars
    }

    private fun applySorting(cars: List<CarWithPhotos>, sortState: SortState): List<CarWithPhotos> {
        return when (sortState) {
            SortState.NAME_ASC -> cars.sortedBy { it.car.model.lowercase() }
            SortState.NAME_DESC -> cars.sortedByDescending { it.car.model.lowercase() }
            SortState.BRAND_ASC -> cars.sortedBy { it.car.brand.lowercase() }
            SortState.BRAND_DESC -> cars.sortedByDescending { it.car.brand.lowercase() }
            SortState.YEAR_ASC -> cars.sortedBy { it.car.year }
            SortState.YEAR_DESC -> cars.sortedByDescending { it.car.year }
            SortState.SERIES_ASC -> cars.sortedBy { it.car.series.lowercase() }
            SortState.SERIES_DESC -> cars.sortedByDescending { it.car.series.lowercase() }
            else -> cars.sortedByDescending { it.car.timestamp } // Default to newest first
        }
    }

    fun toggleCarSelection(carId: String) {
        val current = _selectedCars.value.toMutableSet()
        if (current.contains(carId)) {
            current.remove(carId)
        } else {
            current.add(carId)
        }
        _selectedCars.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun selectAllCars() {
        val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return
        _selectedCars.value = currentCars.map { it.car.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedCars.value = emptySet()
        _isSelectionMode.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilters(filters: CarFilterState) {
        _filterState.value = filters
    }

    fun updateSort(sort: SortState) {
        _sortState.value = sort
    }

    fun updateViewType(viewType: ViewType) {
        _viewType.value = viewType
    }

    fun clearFilters() {
        _filterState.value = CarFilterState()
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun deleteCars(carIds: Set<String>) {
        viewModelScope.launch {
            try {
                val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return@launch

                val carsToDeleteEntities = currentCars.filter { carWithPhotos ->
                    carIds.contains(carWithPhotos.car.id)
                }

                // Delete from local database
                for (carWithPhotos in carsToDeleteEntities) {
                    carDao.deleteCarById(carWithPhotos.car.id)

                    // Delete associated photos from database
                    for (photo in carWithPhotos.photos) {
                        photoDao.hardDeletePhoto(photo.id)

                        // Delete local photo files
                        try {
                            File(photo.localPath).delete()
                            if (!photo.thumbnailPath.isNullOrEmpty()) {
                                File(photo.thumbnailPath).delete()
                            }
                        } catch (e: Exception) {
                            // Log but don't fail if file deletion fails
                        }
                    }
                }

                clearSelection()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete cars: ${e.message}")
            }
        }
    }

    fun exportCars(carIds: Set<String>, format: String): ExportResult {
        return try {
            _isExporting.value = true
            val currentCars = (_uiState.value as? UiState.Success)?.cars
                ?: return ExportResult.Error("No cars to export")

            val selectedCarEntities = currentCars.filter { carWithPhotos ->
                carIds.contains(carWithPhotos.car.id)
            }.map { it.car }

            if (selectedCarEntities.isEmpty()) {
                return ExportResult.Error("No cars selected for export")
            }

            val exportFile = createExportFile()
            val success = exportCarsToCSV(selectedCarEntities, exportFile)

            _isExporting.value = false

            if (success) {
                ExportResult.Success
            } else {
                ExportResult.Error("Failed to create export file")
            }
        } catch (e: Exception) {
            _isExporting.value = false
            ExportResult.Error(e.message ?: "Export failed")
        }
    }

    fun exportAllMainlineCars(): ExportResult {
        return try {
            _isExporting.value = true
            val currentCars = (_uiState.value as? UiState.Success)?.cars
                ?: return ExportResult.Error("No cars to export")

            val carEntities = currentCars.map { it.car }

            if (carEntities.isEmpty()) {
                return ExportResult.Error("No mainline cars found")
            }

            val exportFile = createExportFile()
            val success = exportCarsToCSV(carEntities, exportFile)

            _isExporting.value = false

            if (success) {
                ExportResult.Success
            } else {
                ExportResult.Error("Failed to create export file")
            }
        } catch (e: Exception) {
            _isExporting.value = false
            ExportResult.Error(e.message ?: "Export failed")
        }
    }

    fun shareCars(uri: String) {
        viewModelScope.launch {
            try {
                // Share implementation would typically use Android's sharing intent
                // This is a placeholder for the sharing functionality
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to share cars: ${e.message}")
            }
        }
    }

    fun toggleFavorite(carId: String) {
        viewModelScope.launch {
            try {
                val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return@launch
                val carToUpdate = currentCars.find { it.car.id == carId } ?: return@launch

                val newFavoriteStatus = !carToUpdate.car.isFavorite
                carDao.updateFavoriteStatus(carId, newFavoriteStatus)

            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to update favorite: ${e.message}")
            }
        }
    }

    fun getCarStats(): CarStats {
        val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return CarStats()

        val totalCars = currentCars.size
        val uniqueBrands = currentCars.map { it.car.brand }.distinct().size
        val uniqueSeries = currentCars.map { it.car.series }.distinct().size
        val uniqueSubSeries = currentCars.map { it.car.subseries }.distinct().size
        val favorites = currentCars.count { it.car.isFavorite }
        val totalValue = currentCars.sumOf { it.car.currentValue }
        val avgValue = if (totalCars > 0) totalValue / totalCars else 0.0

        // Mainline-specific stats by subseries
        val rallyCount = currentCars.count { it.car.subseries.contains("rally", ignoreCase = true) }
        val convertiblesCount = currentCars.count { it.car.subseries.contains("convertibles", ignoreCase = true) }
        val muscleCarsCount = currentCars.count { it.car.subseries.contains("muscle", ignoreCase = true) }
        val trucksCount = currentCars.count { it.car.subseries.contains("trucks", ignoreCase = true) }

        return CarStats(
            totalCars = totalCars,
            uniqueBrands = uniqueBrands,
            uniqueSeries = uniqueSeries,
            uniqueSubSeries = uniqueSubSeries,
            favorites = favorites,
            totalValue = totalValue,
            avgValue = avgValue,
            rallyCount = rallyCount,
            convertiblesCount = convertiblesCount,
            muscleCarsCount = muscleCarsCount,
            trucksCount = trucksCount
        )
    }

    fun refresh() {
        loadCars()
    }

    fun retry() {
        loadCars()
    }

    private fun createExportFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.getExternalFilesDir("Exports"), "mainline")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "mainline_cars_export_${timeStamp}.csv")
    }

    private fun exportCarsToCSV(cars: List<CarEntity>, file: File): Boolean {
        return try {
            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("ID,Model,Brand,Series,Subseries,Year,Color,Notes,Barcode,Condition,Value,Timestamp\n")

                // Write car data
                for (car in cars) {
                    writer.append("${car.id},")
                    writer.append("\"${car.model.replace("\"", "\"\"")}\",$") // Escape quotes
                    writer.append("\"${car.brand.replace("\"", "\"\"")}\",$")
                    writer.append("\"${car.series.replace("\"", "\"\"")}\",$")
                    writer.append("\"${car.subseries.replace("\"", "\"\"")}\",$")
                    writer.append("${car.year},")
                    writer.append("\"${car.color.replace("\"", "\"\"")}\",$")
                    writer.append("\"${car.notes.replace("\"", "\"\"")}\",$")
                    writer.append("\"${car.barcode}\",$")
                    writer.append("\"${car.condition.replace("\"", "\"\"")}\",$")
                    writer.append("${car.currentValue},")
                    writer.append("${car.timestamp}\n")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getCurrentUserId(): String {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            // Log the authentication issue
            android.util.Log.e("MainlineViewModel", "User not authenticated - cannot load cars")
            throw IllegalStateException("User must be authenticated to load cars")
        }
        return userId
    }

    data class CarStats(
        val totalCars: Int = 0,
        val uniqueBrands: Int = 0,
        val uniqueSeries: Int = 0,
        val uniqueSubSeries: Int = 0,
        val favorites: Int = 0,
        val totalValue: Double = 0.0,
        val avgValue: Double = 0.0,
        val rallyCount: Int = 0,
        val convertiblesCount: Int = 0,
        val muscleCarsCount: Int = 0,
        val trucksCount: Int = 0,
    )
}