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
class OthersViewModel @Inject constructor(
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
    private val _allOthersCars = MutableStateFlow<List<CarWithPhotos>>(emptyList())

    init {
        loadOthersCars()
        observeFilteredCars()
    }

    private fun loadOthersCars() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val userId = getCurrentUserId()
                
                // Load Others cars (series = "Others")
                carDao.getCarsWithPhotos(userId).collect { allCars ->
                    val othersCars = allCars.filter { carWithPhotos ->
                        carWithPhotos.car.series == "Others"
                    }
                    _allOthersCars.value = othersCars
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load Others cars")
            }
        }
    }

    private fun observeFilteredCars() {
        viewModelScope.launch {
            combine(
                _allOthersCars,
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
                        car.notes.contains(query, ignoreCase = true)
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

    private fun isOtherVehicle(car: CarEntity): Boolean {
        // Check if this is a non-car vehicle (trucks, buses, motorcycles, etc.)
        val otherVehicleTypes = listOf(
            "truck", "bus", "motorcycle", "bike", "boat", "plane", "helicopter",
            "tank", "construction", "emergency", "fire", "police", "ambulance",
            "military", "farm", "working", "big rig", "transport"
        )
        
        val textToCheck = "${car.model} ${car.series} ${car.subseries} ${car.notes}".lowercase()
        
        return otherVehicleTypes.any { vehicleType ->
            textToCheck.contains(vehicleType)
        }
    }

    private fun applyFilters(cars: List<CarWithPhotos>, filters: CarFilterState): List<CarWithPhotos> {
        var filteredCars = cars

        // Filter by year range
        if (filters.minYear != null) {
            filteredCars = filteredCars.filter { it.car.year >= filters.minYear }
        }
        if (filters.maxYear != null) {
            filteredCars = filteredCars.filter { it.car.year <= filters.maxYear }
        }

        // Filter by series
        if (!filters.series.isNullOrEmpty()) {
            filteredCars = filteredCars.filter { carWithPhotos ->
                carWithPhotos.car.series.contains(filters.series, ignoreCase = true)
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun updateFilterState(filterState: CarFilterState) {
        _filterState.value = filterState
    }

    fun clearFilters() {
        _filterState.value = CarFilterState()
    }

    fun updateSortState(sortState: SortState) {
        _sortState.value = sortState
    }

    fun updateViewType(viewType: ViewType) {
        _viewType.value = viewType
    }

    fun toggleCarSelection(carId: String) {
        val currentSelection = _selectedCars.value.toMutableSet()
        if (currentSelection.contains(carId)) {
            currentSelection.remove(carId)
        } else {
            currentSelection.add(carId)
        }
        _selectedCars.value = currentSelection
        _isSelectionMode.value = currentSelection.isNotEmpty()
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

    fun deleteSelectedCars() {
        viewModelScope.launch {
            try {
                val carsToDelete = _selectedCars.value
                val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return@launch
                
                val carsToDeleteEntities = currentCars.filter { carWithPhotos ->
                    carsToDelete.contains(carWithPhotos.car.id)
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

    fun exportSelectedCars(): ExportResult {
        return try {
            _isExporting.value = true
            val carsToExport = _selectedCars.value
            val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return ExportResult.Error("No cars to export")
            
            val selectedCarEntities = currentCars.filter { carWithPhotos ->
                carsToExport.contains(carWithPhotos.car.id)
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

    fun exportAllOthersCars(): ExportResult {
        return try {
            _isExporting.value = true
            val currentCars = (_uiState.value as? UiState.Success)?.cars ?: return ExportResult.Error("No cars to export")
            
            val carEntities = currentCars.map { it.car }
            
            if (carEntities.isEmpty()) {
                return ExportResult.Error("No Others cars found")
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
        val favorites = currentCars.count { it.car.isFavorite }
        val totalValue = currentCars.sumOf { it.car.currentValue }
        val avgValue = if (totalCars > 0) totalValue / totalCars else 0.0
        
        return CarStats(
            totalCars = totalCars,
            uniqueBrands = uniqueBrands,
            uniqueSeries = uniqueSeries,
            favorites = favorites,
            totalValue = totalValue,
            avgValue = avgValue
        )
    }

    fun refresh() {
        loadOthersCars()
    }

    fun retry() {
        loadOthersCars()
    }

    private fun createExportFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.getExternalFilesDir("Exports"), "others")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, "others_cars_export_${timeStamp}.csv")
    }

    private fun exportCarsToCSV(cars: List<CarEntity>, file: File): Boolean {
        return try {
            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("ID,Model,Brand,Series,Year,Color,Notes,Barcode,Condition,Value,Timestamp\n")
                
                // Write car data
                for (car in cars) {
                    writer.append("${car.id},")
                    writer.append("\"${car.model.replace("\"", "\"\"")}\",$") // Escape quotes
                    writer.append("\"${car.brand.replace("\"", "\"\"")}\",$")
                    writer.append("\"${car.series.replace("\"", "\"\"")}\",$")
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
            android.util.Log.e("OthersViewModel", "User not authenticated - cannot load cars")
            throw IllegalStateException("User must be authenticated to load cars")
        }
        return userId
    }

    data class CarStats(
        val totalCars: Int = 0,
        val uniqueBrands: Int = 0,
        val uniqueSeries: Int = 0,
        val favorites: Int = 0,
        val totalValue: Double = 0.0,
        val avgValue: Double = 0.0
    )
}