package com.example.hotwheelscollectors.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val carDao: CarDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Result of barcode search - can be:
     * - LocalCar: Found in user's personal collection
     * - GlobalCar: Found in global Browse database
     * - NotFound: Not found anywhere
     */
    sealed class BarcodeSearchResult {
        data class LocalCar(val car: CarEntity) : BarcodeSearchResult()
        data class GlobalCar(val car: GlobalCarData) : BarcodeSearchResult()
        object NotFound : BarcodeSearchResult()
    }

    private val _searchResult = MutableStateFlow<BarcodeSearchResult?>(null)
    val searchResult: StateFlow<BarcodeSearchResult?> = _searchResult.asStateFlow()

    // Legacy support - keep for compatibility
    private val _foundCar = MutableStateFlow<GlobalCarData?>(null)
    val foundCar: StateFlow<GlobalCarData?> = _foundCar.asStateFlow()

    /**
     * Searches for barcode in both local collection and global database.
     * Priority:
     * 1. Local collection (My Collection) - if found, return LocalCar
     * 2. Global database (Browse) - if found, return GlobalCar
     * 3. NotFound - if not found in either
     */
    fun searchBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                Log.d("BarcodeScannerViewModel", "Searching barcode: $barcode")
                
                // Step 1: Check in LOCAL collection (My Collection)
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val localCar = carDao.getCarByBarcode(barcode, currentUser.uid)
                    if (localCar != null) {
                        Log.d("BarcodeScannerViewModel", "✅ Found in My Collection: ${localCar.model}")
                        _searchResult.value = BarcodeSearchResult.LocalCar(localCar)
                        // Legacy support - set to null for local cars
                        _foundCar.value = null
                        return@launch
                    }
                }
                
                Log.d("BarcodeScannerViewModel", "Not found in My Collection, checking Browse...")
                
                // Step 2: Check in GLOBAL database (Browse)
                val globalCar = firestoreRepository.findGlobalCarByBarcode(barcode)
                if (globalCar != null) {
                    Log.d("BarcodeScannerViewModel", "✅ Found in Browse: ${globalCar.carName}")
                    _searchResult.value = BarcodeSearchResult.GlobalCar(globalCar)
                    // Legacy support
                    _foundCar.value = globalCar
                } else {
                    Log.d("BarcodeScannerViewModel", "❌ Not found anywhere")
                    _searchResult.value = BarcodeSearchResult.NotFound
                    // Legacy support
                    _foundCar.value = null
                }
            } catch (e: Exception) {
                Log.e("BarcodeScannerViewModel", "Error searching barcode: ${e.message}", e)
                _searchResult.value = BarcodeSearchResult.NotFound
                _foundCar.value = null
            }
        }
    }
}
