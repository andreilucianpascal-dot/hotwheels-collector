package com.example.hotwheelscollectors.domain.usecase.collection

import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCollectionUseCase(
    private val carRepository: FirestoreRepository
) {
    operator fun invoke(
        filterMainline: Boolean? = null,
        filterPremium: Boolean? = null,
        searchQuery: String = "",
        sortBy: SortOption = SortOption.RECENT
    ): Flow<List<CarEntity>> {
        return carRepository.getCarsForUser(carRepository.userId).map { cars ->
            cars.filter { car ->
                val matchesType = when {
                    filterMainline == true -> !car.isPremium
                    filterPremium == true -> car.isPremium
                    else -> true
                }

                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    car.brand.contains(searchQuery, ignoreCase = true) ||
                            car.model.contains(searchQuery, ignoreCase = true) ||
                            car.barcode.contains(searchQuery)
                }

                matchesType && matchesSearch
            }.let { filtered ->
                when (sortBy) {
                    SortOption.RECENT -> filtered.sortedByDescending { it.timestamp }
                    SortOption.BRAND -> filtered.sortedBy { it.brand }
                    SortOption.MODEL -> filtered.sortedBy { it.model }
                    SortOption.YEAR -> filtered.sortedByDescending { it.year }
                }
            }
        }
    }

    enum class SortOption {
        RECENT,
        BRAND,
        MODEL,
        YEAR
    }
}