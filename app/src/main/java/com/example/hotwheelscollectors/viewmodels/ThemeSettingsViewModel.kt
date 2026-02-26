package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Sync UI state with persisted theme preferences
        viewModelScope.launch {
            combine(
                userPreferences.themeMode.distinctUntilChanged(),
                userPreferences.colorScheme.distinctUntilChanged(),
                userPreferences.useDynamicColor.distinctUntilChanged(),
                userPreferences.fontScale.distinctUntilChanged(),
                userPreferences.mainScreenFontFamily.distinctUntilChanged(),
            ) { mode, scheme, dynamic, scale, font ->
                _uiState.value.copy(
                    themeMode = mode,
                    colorScheme = scheme,
                    useDynamicColor = dynamic,
                    useCustomFontSize = scale != 1.0f,
                    fontScale = scale,
                    mainScreenFontFamily = font
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }

        // Sync custom global color scheme (3 colors)
        viewModelScope.launch {
            combine(
                userPreferences.customSchemeColor1.distinctUntilChanged(),
                userPreferences.customSchemeColor2.distinctUntilChanged(),
                userPreferences.customSchemeColor3.distinctUntilChanged(),
            ) { c1, c2, c3 ->
                _uiState.value.copy(
                    customSchemeColor1 = c1,
                    customSchemeColor2 = c2,
                    customSchemeColor3 = c3,
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }

        // Sync main screen button colors
        viewModelScope.launch {
            // combine first 5 flows, then combine result with the 6th
            combine(
                userPreferences.mainButtonMainlineColor.distinctUntilChanged(),
                userPreferences.mainButtonPremiumColor.distinctUntilChanged(),
                userPreferences.mainButtonSilverColor.distinctUntilChanged(),
                userPreferences.mainButtonTreasureHuntColor.distinctUntilChanged(),
                userPreferences.mainButtonSuperTreasureHuntColor.distinctUntilChanged(),
            ) { mainline, premium, silver, th, sth ->
                _uiState.value.copy(
                    mainlineButtonColor = mainline,
                    premiumButtonColor = premium,
                    silverButtonColor = silver,
                    treasureHuntButtonColor = th,
                    superTreasureHuntButtonColor = sth,
                    othersButtonColor = _uiState.value.othersButtonColor,
                )
            }.combine(userPreferences.mainButtonOthersColor.distinctUntilChanged()) { state, others ->
                state.copy(othersButtonColor = others)
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateThemeMode(mode: String) {
        // Optimistic update: update UI immediately
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch {
            userPreferences.updateThemeMode(mode)
        }
    }

    fun updateColorScheme(scheme: String) {
        // Optimistic update: update UI immediately
        _uiState.value = _uiState.value.copy(colorScheme = scheme)
        viewModelScope.launch {
            userPreferences.updateColorScheme(scheme)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        // Optimistic update: update UI immediately
        _uiState.value = _uiState.value.copy(useDynamicColor = enabled)
        viewModelScope.launch {
            userPreferences.updateUseDynamicColor(enabled)
        }
    }

    fun setCustomFontSizeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Turning ON custom size → if currently exactly 1.0f, move a bit up so effect is visible
                val currentScale = _uiState.value.fontScale
                val newScale = if (currentScale == 1.0f) 1.1f else currentScale
                val finalScale = newScale.coerceIn(0.8f, 1.2f)
                // Optimistic update: update UI immediately
                _uiState.value = _uiState.value.copy(
                    useCustomFontSize = true,
                    fontScale = finalScale
                )
                userPreferences.updateFontScale(finalScale)
            } else {
                // Optimistic update: update UI immediately
                _uiState.value = _uiState.value.copy(
                    useCustomFontSize = false,
                    fontScale = 1.0f
                )
                userPreferences.updateFontScale(1.0f)
            }
        }
    }

    fun updateFontScale(scale: Float) {
        val finalScale = scale.coerceIn(0.8f, 1.2f)
        // Optimistic update: update UI immediately
        _uiState.value = _uiState.value.copy(fontScale = finalScale)
        viewModelScope.launch {
            userPreferences.updateFontScale(finalScale)
        }
    }

    fun updateCustomSchemeColor(slot: Int, colorInt: Int) {
        viewModelScope.launch {
            userPreferences.updateCustomSchemeColor(slot, colorInt)
        }
    }

    fun updateMainScreenFontFamily(font: String) {
        viewModelScope.launch {
            userPreferences.updateMainScreenFontFamily(font)
        }
    }

    fun updateMainButtonColor(category: String, colorInt: Int) {
        viewModelScope.launch {
            userPreferences.updateMainButtonColor(category, colorInt)
        }
    }

    data class UiState(
        val themeMode: String = "system",
        val colorScheme: String = "default",
        val useDynamicColor: Boolean = true,
        val useCustomFontSize: Boolean = false,
        val fontScale: Float = 1.0f,
        val mainScreenFontFamily: String = "default",
        // Global custom scheme colors (ARGB Int; 0 = not set)
        val customSchemeColor1: Int = 0,
        val customSchemeColor2: Int = 0,
        val customSchemeColor3: Int = 0,
        // Main screen button colors (ARGB Int; 0 = default)
        val mainlineButtonColor: Int = 0,
        val premiumButtonColor: Int = 0,
        val silverButtonColor: Int = 0,
        val treasureHuntButtonColor: Int = 0,
        val superTreasureHuntButtonColor: Int = 0,
        val othersButtonColor: Int = 0,
    )
}