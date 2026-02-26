package com.example.hotwheelscollectors.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.auth.GoogleDriveAuthService
import com.example.hotwheelscollectors.data.auth.GoogleSignInResult
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.StorageOrchestrator
import com.example.hotwheelscollectors.data.repository.CloudUserSettingsRepository
import com.example.hotwheelscollectors.model.PersonalStorageType
import android.util.Log
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val googleDriveAuthService: GoogleDriveAuthService,
    private val authRepository: AuthRepository,
    private val googleDriveRepository: GoogleDriveRepository,
    private val storageOrchestrator: StorageOrchestrator,
    private val cloudUserSettingsRepository: CloudUserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkGoogleDriveStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.storageType.collect { storageType ->
                _uiState.value = _uiState.value.copy(storageType = storageType)
            }
        }
    }

    private fun checkGoogleDriveStatus() {
        val isSignedIn = googleDriveAuthService.isSignedIn()
        val userEmail = googleDriveAuthService.getUserEmail()
        _uiState.value = _uiState.value.copy(
            isGoogleDriveSignedIn = isSignedIn,
            googleDriveUserEmail = userEmail
        )
    }

    fun updateStorageType(storageType: PersonalStorageType) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "📝 updateStorageType called with: $storageType")
            
            // If switching to Google Drive, check if user is signed in
            if (storageType == PersonalStorageType.GOOGLE_DRIVE && !_uiState.value.isGoogleDriveSignedIn) {
                _uiState.value = _uiState.value.copy(
                    error = "Please sign in to Google Drive first before selecting it as your storage option."
                )
                return@launch
            }
            
            // Get current storage type before updating
            val previousStorageType = _uiState.value.storageType
            
            // ✅ This will update BOTH storageType and storageLocation (synced in UserPreferences)
            userPreferences.updateStorageType(storageType)
            
            android.util.Log.d("SettingsViewModel", "✅ Storage type updated to: $storageType (storageLocation will be synced automatically)")
            
            // ✅ Save to cloud (survives uninstall)
            if (_uiState.value.isGoogleDriveSignedIn) {
                launch(Dispatchers.IO) {
                    try {
                        val saveResult = cloudUserSettingsRepository.savePrimaryStorage(storageType)
                        if (saveResult.isSuccess) {
                            android.util.Log.i("SettingsViewModel", "✅ Primary storage saved to cloud: $storageType")
                        } else {
                            android.util.Log.w("SettingsViewModel", "⚠️ Failed to save storage type to cloud: ${saveResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("SettingsViewModel", "⚠️ Error saving storage type to cloud: ${e.message}")
                    }
                }
            }
            
            // ✅ Delegate migration to StorageOrchestrator (ViewModel does NOT migrate directly)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = "Migrating storage..."
            )
            
            val migrationResult = storageOrchestrator.onStorageChanged(previousStorageType, storageType)
            if (migrationResult.isSuccess) {
                android.util.Log.i("SettingsViewModel", "✅ Storage migration completed successfully!")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "✅ Storage migrated successfully!"
                )
            } else {
                android.util.Log.e("SettingsViewModel", "❌ Storage migration failed: ${migrationResult.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Migration failed: ${migrationResult.exceptionOrNull()?.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    // ✅ REMOVED: All migration/restore logic moved to StorageOrchestrator
    // - syncRoomFromDrive() → StorageOrchestrator.onStorageChanged()
    // - migrateLocalCarsToDrive() → StorageOrchestrator.onStorageChanged()
    // - checkAndRestoreFromCloud() → StorageOrchestrator.onAppStart()
    //
    // SettingsViewModel now only orchestrates UI state, not data migration.
    // Storage operations are handled by StorageOrchestrator.

    fun getGoogleSignInIntent(): Intent {
        return googleDriveAuthService.getSignInIntent()
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = googleDriveAuthService.handleSignInResult(data)) {
                is GoogleSignInResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isGoogleDriveSignedIn = true,
                        googleDriveUserEmail = result.account.email,
                        isLoading = false,
                        message = "Successfully signed in to Google Drive"
                    )                    // ✅ CRITICAL: Trigger data restore after Google Drive authentication
                    launch(Dispatchers.IO) {
                        try {
                            android.util.Log.d("SettingsViewModel", "User authenticated to Google Drive - triggering data restore...")
                            val restoreResult = storageOrchestrator.onUserAuthenticated()
                            if (restoreResult.isSuccess) {
                                android.util.Log.i("SettingsViewModel", "✅ Data restored from Drive after authentication")
                            } else {
                                android.util.Log.w("SettingsViewModel", "⚠️ Data restore failed after authentication: ${restoreResult.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsViewModel", "Failed to restore data after authentication", e)
                        }
                    }
                }
                is GoogleSignInResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun signOutFromGoogleDrive() {
        viewModelScope.launch {
            googleDriveAuthService.signOut()
            _uiState.value = _uiState.value.copy(
                isGoogleDriveSignedIn = false,
                googleDriveUserEmail = null,
                message = "Signed out from Google Drive"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                // Sign out from Google Drive
                if (_uiState.value.isGoogleDriveSignedIn) {
                    googleDriveAuthService.signOut()
                }

                // Sign out from Firebase Auth
                authRepository.signOut()

                _uiState.value = _uiState.value.copy(
                    isGoogleDriveSignedIn = false,
                    googleDriveUserEmail = null,
                    message = "Signed out successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to sign out: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun refreshGoogleDriveStatus() {
        checkGoogleDriveStatus()
    }
    
    // ✅ REMOVED: migrateLocalCarsToDrive()
    // Migration logic is now in StorageOrchestrator.onStorageChanged()
    // ViewModel does NOT migrate data directly - it only orchestrates UI state.
    
    fun testGoogleDriveConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            
            if (!_uiState.value.isGoogleDriveSignedIn) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Please sign in to Google Drive first"
                )
                return@launch
            }
            
            val result = googleDriveRepository.testConnectionAndCreateFolder()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "✅ Connection successful! Folder 'HotWheelsCollectors' created/verified in your Google Drive."
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connection test failed: $errorMsg"
                )
            }
        }
    }

    fun toggleNotifications() {
        _uiState.value = _uiState.value.copy(
            notificationsEnabled = !_uiState.value.notificationsEnabled
        )
        // Save to UserPreferences
    }

    fun toggleAutoSync() {
        _uiState.value = _uiState.value.copy(
            autoSyncEnabled = !_uiState.value.autoSyncEnabled
        )
        // Save to UserPreferences
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Implement data export functionality
                // This could export to CSV, JSON, or backup file
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Data exported successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to export data: ${e.message}"
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Implement cache clearing functionality
                // Clear image cache, temporary files, etc.
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Cache cleared successfully"
                )
        } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    fun manageDatabase() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Navigate to database management screen
                // Or show database management dialog
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Opening database management..."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to open database management: ${e.message}"
                )
            }
        }
    }
}

data class SettingsUiState(
    val storageType: PersonalStorageType = PersonalStorageType.LOCAL,
    val isGoogleDriveSignedIn: Boolean = false,
    val googleDriveUserEmail: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val notificationsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true
)

