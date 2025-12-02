package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.SearchKeywordDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrandSeriesViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val searchKeywordDao: SearchKeywordDao,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _localCars: Flow<List<CarEntity>> = carDao.getAllCars()
    val localCars: Flow<List<CarEntity>> = _localCars

    fun getCarsForBrandAndSeries(brandId: String, seriesId: String): Flow<List<CarEntity>> {
        return carDao.getAllCars()
    }

    fun getCarsForMainlineCategory(category: String): Flow<List<CarEntity>> {
        return carDao.getAllCars()
    }
    
    /**
     * Gets a dynamic and always up-to-date list of unique brand IDs
     * for a specific category by querying the local database.
     */
    fun getBrandsForCategory(categoryId: String): Flow<List<String>> {
        // For now, get current user ID from auth and use it
        val userId = authRepository.getCurrentUser()?.uid ?: ""
        return carDao.getUniqueBrandsForSeries(userId, categoryId)
    }

    fun deleteCar(carId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("BrandSeriesViewModel", "Deleting car: $carId")

                // Delete photos for this car
                val photos = photoDao.getPhotosForCar(carId).first()
                for (photo in photos) {
                    photoDao.hardDeletePhoto(photo.id)
                }

                // Delete search keywords for this car
                searchKeywordDao.deleteKeywordsForCar(carId)

                // Delete the car
                carDao.deleteCarById(carId)

                android.util.Log.i("BrandSeriesViewModel", "✅ Car deleted successfully: $carId")

            } catch (e: Exception) {
                android.util.Log.e("BrandSeriesViewModel", "Error deleting car: ${e.message}", e)
            }
        }
    }

    fun getBrandName(brandId: String): String {
        // This can be improved with a database table for brands later
        return brandId.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }

    fun getSeriesName(seriesId: String): String {
        // This can be improved with a database table for series later
        return seriesId.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }

    fun getBrandDisplayNames(categoryId: String): Flow<List<Pair<String, String>>> {
        return getBrandsForCategory(categoryId).combine(localCars) { brands, cars ->
            brands.map { brandId ->
                brandId to getBrandName(brandId)
            }
        }
    }

    fun getBrandWithCarCounts(categoryId: String, cars: List<HotWheelsCar>): Flow<List<BrandWithCount>> {
        return getBrandsForCategory(categoryId).combine(localCars) { brands, localCarsList ->
            brands.map { brandId ->
                val carCount = localCarsList.count { car ->
                    car.brand.equals(brandId, ignoreCase = true) && car.series.equals(categoryId, ignoreCase = true)
                }

                val displayName = getBrandName(brandId)

            BrandWithCount(
                id = brandId,
                name = displayName,
                carCount = carCount
            )
            }
        }
    }

    fun categoryHasBrands(categoryId: String): Boolean {
        return categoryId.lowercase() != "hot_roads"
    }

    data class UiState(
        val cars: List<HotWheelsCar> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    data class BrandWithCount(val id: String, val name: String, val carCount: Int)
}

// Extension function to convert CarEntity to HotWheelsCar
fun CarEntity.toHotWheelsCar(): HotWheelsCar {
    return HotWheelsCar(
        id = id,
        name = model, // Use model as name since HotWheelsCar has name field
        model = model,
        brand = brand,
        series = series,
        subseries = subseries,
        year = year,
        number = number,
        color = color,
        tampos = "", // Not available in CarEntity
        barcode = barcode,
        baseType = "", // Not available in CarEntity
        wheelType = "", // Not available in CarEntity
        photoUrl = photoUrl,
        folderPath = folderPath,
        isPremium = isPremium,
        isSTH = isSTH,
        isTH = isTH,
        isFirstEdition = isFirstEdition,
        timestamp = timestamp,
        frontPhotoPath = frontPhotoPath,
        backPhotoPath = backPhotoPath,
        combinedPhotoPath = combinedPhotoPath,
        notes = notes,
        purchaseDate = "", // Not available in CarEntity
        purchasePrice = purchasePrice,
        purchaseLocation = location,
        condition = condition,
        packageCondition = "", // Not available in CarEntity
        estimatedValue = currentValue,
        lastPriceCheck = "", // Not available in CarEntity
        searchKeywords = searchKeywords
    )
}
