package com.example.hotwheelscollectors.viewmodels

import com.example.hotwheelscollectors.utils.SaveLocation

/**
 * Unified UI State for all "Add" car ViewModels.
 * 
 * This ensures consistent UI behavior across all car types:
 * - AddMainlineViewModel
 * - AddPremiumViewModel  
 * - AddTreasureHuntViewModel
 * - AddSuperTreasureHuntViewModel
 * - AddOthersViewModel
 */
sealed class AddCarUiState {
    object Idle : AddCarUiState()
    object Saving : AddCarUiState()
    object ProcessingPhoto : AddCarUiState()
    object PhotoProcessed : AddCarUiState()
    data class Success(val message: String) : AddCarUiState()
    data class Error(val message: String) : AddCarUiState()
    data class ContributedToGlobal(val carId: String, val message: String) : AddCarUiState()
    data class SmartCategorized(val saveLocation: SaveLocation) : AddCarUiState()
    object RequiresUserSelection : AddCarUiState()
    data class LowConfidenceCategory(val saveLocation: SaveLocation) : AddCarUiState()
    data class ExportReady(val exportData: String) : AddCarUiState()
    data class ShareReady(val shareText: String) : AddCarUiState()
}
