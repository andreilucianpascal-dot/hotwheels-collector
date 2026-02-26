package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global theme state used by MainActivity to drive HotWheelsCollectorsTheme.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    userPreferences: UserPreferences,
) : ViewModel() {

    data class ThemeUiState(
        val themeMode: String = "system",      // "light" | "dark" | "system"
        val colorScheme: String = "default",   // key for HotWheelsThemeManager or "default"
        val useDynamicColor: Boolean = true,
        val fontScale: Float = 1.0f,
        val customSchemeColor1: Int = 0,
        val customSchemeColor2: Int = 0,
        val customSchemeColor3: Int = 0,
    )

    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.themeMode,
                userPreferences.colorScheme,
                userPreferences.useDynamicColor,
                userPreferences.fontScale,
            ) { mode, scheme, dynamic, scale ->
                ThemeUiState(
                    themeMode = mode,
                    colorScheme = scheme,
                    useDynamicColor = dynamic,
                    fontScale = scale,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Listen for custom scheme colors
        viewModelScope.launch {
            combine(
                userPreferences.customSchemeColor1,
                userPreferences.customSchemeColor2,
                userPreferences.customSchemeColor3,
            ) { c1, c2, c3 ->
                _uiState.value.copy(
                    customSchemeColor1 = c1,
                    customSchemeColor2 = c2,
                    customSchemeColor3 = c3,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}


