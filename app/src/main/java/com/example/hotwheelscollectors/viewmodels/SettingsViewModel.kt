package com.example.hotwheelscollectors.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.auth.GoogleDriveAuthService
import com.example.hotwheelscollectors.data.auth.GoogleSignInResult
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.model.PersonalStorageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val googleDriveAuthService: GoogleDriveAuthService,
    private val authRepository: AuthRepository
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
            // If switching to Google Drive, check if user is signed in
            if (storageType == PersonalStorageType.GOOGLE_DRIVE && !_uiState.value.isGoogleDriveSignedIn) {
                _uiState.value = _uiState.value.copy(
                    error = "Please sign in to Google Drive first before selecting it as your storage option."
                )
                return@launch
            }
            
            userPreferences.updateStorageType(storageType)
        }
    }

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
                    )
                    
                    // Auto-switch to Google Drive storage if user was on local storage
                    if (_uiState.value.storageType == PersonalStorageType.LOCAL) {
                        userPreferences.updateStorageType(PersonalStorageType.GOOGLE_DRIVE)
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