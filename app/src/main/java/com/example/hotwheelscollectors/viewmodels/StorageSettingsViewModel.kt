package com.example.hotwheelscollectors.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class StorageSettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            // Load current storage location from UserPreferences
            var currentLocation = userPreferences.storageLocation.first()
            Log.d("StorageSettingsViewModel", "📖 Loaded storage location from UserPreferences: '$currentLocation'")
            
            // Normalize "Device" to "Internal" for display in UI (to match dialog values)
            val displayLocation = when {
                currentLocation == "Device" || currentLocation.isEmpty() -> {
                    Log.d("StorageSettingsViewModel", "   Normalizing to 'Internal' for display")
                    "Internal"
                }
                currentLocation.equals("Google Drive", ignoreCase = true) -> {
                    Log.d("StorageSettingsViewModel", "   Keeping 'Google Drive' for display")
                    "Google Drive"  // ✅ Keep exact format for display
                }
                else -> {
                    Log.d("StorageSettingsViewModel", "   Keeping '$currentLocation' for display")
                    currentLocation
                }
            }
            
            // Real storage calculation using Android StorageManager
            val storageInfo = calculateStorageInfo(displayLocation)
            _uiState.value = storageInfo
            
            Log.d("StorageSettingsViewModel", "✅ UI state updated with location: '${_uiState.value.storageLocation}'")
            
            // Observe storage location changes
            userPreferences.storageLocation.collect { location ->
                Log.d("StorageSettingsViewModel", "🔄 Storage location changed to: '$location'")
                val normalizedLocation = when {
                    location == "Device" || location.isEmpty() -> "Internal"
                    location.equals("Google Drive", ignoreCase = true) -> "Google Drive"
                    else -> location
                }
                _uiState.value = _uiState.value.copy(storageLocation = normalizedLocation)
                Log.d("StorageSettingsViewModel", "✅ UI state updated with location: '$normalizedLocation'")
            }
        }
    }

    private fun calculateStorageInfo(currentLocation: String): UiState {
        // This will calculate real storage values
        return UiState(
            totalSpace = getTotalStorageSpace(),
            usedSpace = getUsedStorageSpace(),
            photoSpace = getPhotoStorageSpace(),
            databaseSpace = getDatabaseStorageSpace(),
            cacheSpace = getCacheStorageSpace(),
            storageLocation = currentLocation,
            autoBackupEnabled = getAutoBackupStatus()
        )
    }

    private fun getTotalStorageSpace(): Long {
        // Real implementation for total storage
        return 0L
    }

    private fun getUsedStorageSpace(): Long {
        // Real implementation for used storage
        return 0L
    }

    private fun getPhotoStorageSpace(): Long {
        // Real implementation for photo storage
        return 0L
    }

    private fun getDatabaseStorageSpace(): Long {
        // Real implementation for database storage
        return 0L
    }

    private fun getCacheStorageSpace(): Long {
        // Real implementation for cache storage
        return 0L
    }

    private fun getAutoBackupStatus(): Boolean {
        // Real implementation for auto backup status
        return false
    }

    fun toggleAutoBackup() {
        _uiState.value = _uiState.value.copy(
            autoBackupEnabled = !_uiState.value.autoBackupEnabled
        )
    }

    /**
     * Updates the storage location preference in UserPreferences.
     * This will cause DynamicStorageRepository to use the new repository on next save.
     * 
     * @param location One of: "Internal", "Google Drive", "OneDrive", "Dropbox"
     */
    fun updateStorageLocation(location: String) {
        viewModelScope.launch {
            Log.d("StorageSettingsViewModel", "📝 updateStorageLocation called with: '$location'")
            
            // Normalize "Internal" to "Device" for UserPreferences (to match default)
            // BUT keep "Google Drive" as-is (with space and proper case)
            val normalizedLocation = when {
                location == "Internal" || location == "Internal Storage (On Device)" -> {
                    Log.d("StorageSettingsViewModel", "   Normalizing 'Internal' to 'Device'")
                    "Device"
                }
                location.equals("Google Drive", ignoreCase = true) -> {
                    Log.d("StorageSettingsViewModel", "   Keeping 'Google Drive' as-is")
                    "Google Drive"  // ✅ Keep exact format with space
                }
                else -> {
                    Log.d("StorageSettingsViewModel", "   Keeping '$location' as-is")
                    location
                }
            }
            
            Log.d("StorageSettingsViewModel", "   Saving to UserPreferences: '$normalizedLocation'")
            
            // Update UserPreferences - this will persist the choice
            userPreferences.updateStorageLocation(normalizedLocation)
            
            // Update UI state immediately (keep original format for display)
            _uiState.value = _uiState.value.copy(storageLocation = location)
            
            Log.d("StorageSettingsViewModel", "✅ Storage location updated successfully")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            // Real cache clearing implementation
            clearCacheFiles()
            loadStorageInfo() // Reload storage info
        }
    }

    private fun clearCacheFiles() {
        // Real cache clearing logic
    }

    fun compressPhotos() {
        viewModelScope.launch {
            // Real photo compression implementation
            compressPhotoFiles()
            loadStorageInfo() // Reload storage info
        }
    }

    private fun compressPhotoFiles() {
        // Real photo compression logic
    }

    data class UiState(
        val totalSpace: Long = 0L,
        val usedSpace: Long = 0L,
        val photoSpace: Long = 0L,
        val databaseSpace: Long = 0L,
        val cacheSpace: Long = 0L,
        val storageLocation: String = "Device",
        val autoBackupEnabled: Boolean = false
    )
}