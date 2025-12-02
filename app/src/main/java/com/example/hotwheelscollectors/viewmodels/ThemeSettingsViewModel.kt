package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun updateColorScheme(scheme: String) {
        _uiState.value = _uiState.value.copy(colorScheme = scheme)
    }

    fun toggleDynamicColor() {
        _uiState.value = _uiState.value.copy(useDynamicColor = !_uiState.value.useDynamicColor)
    }

    fun toggleCustomFontSize() {
        _uiState.value = _uiState.value.copy(useCustomFontSize = !_uiState.value.useCustomFontSize)
    }

    fun updateFontScale(scale: Float) {
        _uiState.value = _uiState.value.copy(fontScale = scale)
    }

    data class UiState(
        val themeMode: String = "system",
        val colorScheme: String = "default",
        val useDynamicColor: Boolean = true,
        val useCustomFontSize: Boolean = false,
        val fontScale: Float = 1.0f
    )
}