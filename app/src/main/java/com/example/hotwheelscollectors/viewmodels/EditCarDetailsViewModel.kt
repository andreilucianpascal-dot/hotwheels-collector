package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI State --- //
sealed class EditCarUiState {
    object Idle : EditCarUiState()
    object Loading : EditCarUiState()
    object Success : EditCarUiState()
    data class Error(val message: String) : EditCarUiState()
}

@HiltViewModel
class EditCarDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EditCarUiState>(EditCarUiState.Idle)
    val uiState: StateFlow<EditCarUiState> = _uiState.asStateFlow()

    // --- Form State --- //
    var name by mutableStateOf("")
        private set
    var brand by mutableStateOf("")
        private set
    var series by mutableStateOf("")
        private set
    var category by mutableStateOf("")
        private set
    var color by mutableStateOf("")
        private set
    var year by mutableStateOf<Int?>(null)
        private set
    var barcode by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set
    var isTH by mutableStateOf(false)
        private set
    var isSTH by mutableStateOf(false)
        private set
    var isPremium by mutableStateOf(false)
        private set
    
    // --- Internal State --- //
    private var currentCar: CarEntity? = null
    private var originalValues = mutableMapOf<String, Any>()
    
    val hasUnsavedChanges: Boolean
        get() = name != (originalValues["name"] as? String ?: "") ||
                brand != (originalValues["brand"] as? String ?: "") ||
                color != (originalValues["color"] as? String ?: "") ||
                year != (originalValues["year"] as? Int?) ||
                notes != (originalValues["notes"] as? String ?: "")

    /**
     * Detectează tipul de mașină pentru a determina dacă brandul ar trebui să fie editable
     */
    val carType: String
        get() = when {
            isPremium -> "premium"
            isTH -> "treasure_hunt"
            isSTH -> "super_treasure_hunt"
            series == "Mainline" -> "mainline"
            else -> "others"
        }

    /**
     * Determină dacă brandul ar trebui să fie editable
     * READ-ONLY doar pentru Mainline, EDITABLE pentru toate celelalte tipuri
     */
    val isBrandEditable: Boolean
        get() = carType != "mainline"

    // --- Form Update Functions --- //
    fun updateName(value: String) { name = value }
    fun updateBrand(value: String) { brand = value }
    fun updateColor(value: String) { color = value }
    fun updateYear(value: Int) { year = value }
    fun updateNotes(value: String) { notes = value }
    fun updateIsTH(value: Boolean) { isTH = value }
    fun updateIsSTH(value: Boolean) { isSTH = value }

    /**
     * Încarcă datele mașinii din baza de date
     */
    fun loadCar(carId: String) {
        viewModelScope.launch {
            _uiState.value = EditCarUiState.Loading
            try {
                val car = carDao.getCarById(carId)
                if (car != null) {
                    currentCar = car
                    
                    // Populează formularul cu datele existente
                    name = car.model ?: ""
                    brand = car.brand ?: ""
                    series = car.series ?: ""
                    category = car.subseries ?: ""
                    color = car.color ?: ""
                    year = if (car.year != 0) car.year else null
                    barcode = car.barcode ?: ""
                    notes = car.notes ?: ""
                    isTH = car.isTH
                    isSTH = car.isSTH
                    isPremium = car.isPremium
                    
                    // Salvează valorile originale pentru detectarea modificărilor
                    originalValues.clear()
                    originalValues["name"] = name
                    originalValues["brand"] = brand
                    originalValues["color"] = color
                    originalValues["year"] = year ?: 0
                    originalValues["notes"] = notes
                    
                    Log.d("EditCarDetailsViewModel", "Car loaded successfully:")
                    Log.d("EditCarDetailsViewModel", "  - Model: $name")
                    Log.d("EditCarDetailsViewModel", "  - Brand: $brand")
                    Log.d("EditCarDetailsViewModel", "  - Series: $series")
                    Log.d("EditCarDetailsViewModel", "  - Category: $category")
                    Log.d("EditCarDetailsViewModel", "  - Car Type: $carType")
                    Log.d("EditCarDetailsViewModel", "  - Is Brand Editable: $isBrandEditable")
                    
                    _uiState.value = EditCarUiState.Idle
                } else {
                    Log.e("EditCarDetailsViewModel", "Car not found with ID: $carId")
                    _uiState.value = EditCarUiState.Error("Car not found")
                }
            } catch (e: Exception) {
                Log.e("EditCarDetailsViewModel", "Error loading car: ${e.message}", e)
                _uiState.value = EditCarUiState.Error("Failed to load car: ${e.message}")
            }
        }
    }

    /**
     * Salvează modificările în baza de date
     */
    fun saveChanges() {
        viewModelScope.launch {
            try {
                val car = currentCar ?: throw IllegalStateException("No car loaded")
                
                // Creează o copie actualizată a mașinii
                val updatedCar = car.copy(
                    model = name.trim(),
                    brand = brand.trim(),
                    color = color.trim(),
                    year = year ?: 0,
                    notes = notes.trim(),
                    isTH = isTH,
                    isSTH = isSTH,
                    lastModified = java.util.Date()
                )
                
                // Salvează în baza de date
                carDao.updateCar(updatedCar)
                
                // Actualizează valorile originale
                originalValues.clear()
                originalValues["name"] = name
                originalValues["brand"] = brand
                originalValues["color"] = color
                originalValues["year"] = year ?: 0
                originalValues["notes"] = notes
                
                Log.d("EditCarDetailsViewModel", "Car updated successfully")
                _uiState.value = EditCarUiState.Success
                
            } catch (e: Exception) {
                Log.e("EditCarDetailsViewModel", "Error saving changes: ${e.message}", e)
                _uiState.value = EditCarUiState.Error("Failed to save changes: ${e.message}")
            }
        }
    }

    /**
     * Resetează modificările la valorile originale
     */
    fun resetChanges() {
        name = originalValues["name"] as? String ?: ""
        brand = originalValues["brand"] as? String ?: ""
        color = originalValues["color"] as? String ?: ""
        year = originalValues["year"] as? Int?
        notes = originalValues["notes"] as? String ?: ""
    }

    fun updateBarcode(value: String) { barcode = value }
}
