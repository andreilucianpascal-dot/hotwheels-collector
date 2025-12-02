package com.example.hotwheelscollectors.data.repository

import android.content.Context
import com.example.hotwheelscollectors.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val settingsFlow = MutableStateFlow(loadSettings())

    fun getSettings(): Flow<AppSettings> = settingsFlow

    // Add this method to get current value
    fun getCurrentSettings(): AppSettings = settingsFlow.value

    fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            putBoolean(KEY_DARK_THEME, settings.isDarkTheme)
            putString(KEY_THEME_NAME, settings.themeName)
            putString(KEY_STORAGE_LOCATION, settings.storageLocation)
        }.apply()

        settingsFlow.value = settings
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false),
            themeName = prefs.getString(KEY_THEME_NAME, "Classic") ?: "Classic",
            storageLocation = prefs.getString(KEY_STORAGE_LOCATION, "Device") ?: "Device"
        )
    }

    companion object {
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_STORAGE_LOCATION = "storage_location"
    }
}