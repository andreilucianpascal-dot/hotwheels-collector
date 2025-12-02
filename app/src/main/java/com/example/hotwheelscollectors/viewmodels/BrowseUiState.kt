package com.example.hotwheelscollectors.viewmodels

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    object Success : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}

