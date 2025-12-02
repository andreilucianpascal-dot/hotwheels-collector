package com.example.hotwheelscollectors.domain.usecase.settings

import com.example.hotwheelscollectors.data.repository.PreferencesRepository
import com.example.hotwheelscollectors.domain.model.AppSettings

class UpdateSettingsUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(
        isDarkTheme: Boolean? = null,
        themeName: String? = null,
        storageLocation: String? = null
    ): Result<Unit> = runCatching {
        val currentSettings = preferencesRepository.getCurrentSettings()  // Use the new method
        val updatedSettings = currentSettings.copy(
            isDarkTheme = isDarkTheme ?: currentSettings.isDarkTheme,
            themeName = themeName ?: currentSettings.themeName,
            storageLocation = storageLocation ?: currentSettings.storageLocation
        )
        preferencesRepository.updateSettings(updatedSettings)
    }

    suspend fun updateAll(settings: AppSettings): Result<Unit> = runCatching {
        preferencesRepository.updateSettings(settings)
    }
}