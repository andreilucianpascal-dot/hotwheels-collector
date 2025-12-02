package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.utils.DatabaseCleanup
import com.example.hotwheelscollectors.utils.DatabaseStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseCleanupViewModel @Inject constructor(
    private val databaseCleanup: DatabaseCleanup,
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseCleanupUiState())
    val uiState: StateFlow<DatabaseCleanupUiState> = _uiState.asStateFlow()

    init {
        loadDatabaseStats()
    }

    private fun loadDatabaseStats() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val stats = databaseCleanup.getDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        databaseStats = stats,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "User not authenticated",
                        isError = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCleanupViewModel", "Failed to load database stats: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to load database stats: ${e.message}",
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun removeGenericBrandCars() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "",
                    isError = false
                )

                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    databaseCleanup.removeGenericBrandCars()
                    
                    // Reload stats
                    val stats = databaseCleanup.getDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        databaseStats = stats,
                        message = "Generic brand cars removed successfully",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "User not authenticated",
                        isError = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCleanupViewModel", "Failed to remove generic brand cars: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to remove generic brand cars: ${e.message}",
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun removeDuplicateCars() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "",
                    isError = false
                )

                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    databaseCleanup.removeDuplicateCars()
                    
                    // Reload stats
                    val stats = databaseCleanup.getDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        databaseStats = stats,
                        message = "Duplicate cars removed successfully",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "User not authenticated",
                        isError = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCleanupViewModel", "Failed to remove duplicate cars: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to remove duplicate cars: ${e.message}",
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun clearUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "",
                    isError = false
                )

                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    databaseCleanup.clearUserData()
                    
                    // Reload stats
                    val stats = databaseCleanup.getDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        databaseStats = stats,
                        message = "User data cleared successfully",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        message = "User not authenticated",
                        isError = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCleanupViewModel", "Failed to clear user data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to clear user data: ${e.message}",
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "",
                    isError = false
                )

                databaseCleanup.clearAllData()
                
                // Reload stats
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val stats = databaseCleanup.getDatabaseStats()
                    _uiState.value = _uiState.value.copy(
                        databaseStats = stats,
                        message = "All data cleared successfully",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        databaseStats = DatabaseStats(0, 0, 0, 0, 0, 0, 0),
                        message = "All data cleared successfully",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseCleanupViewModel", "Failed to clear all data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    message = "Failed to clear all data: ${e.message}",
                    isError = true,
                    isLoading = false
                )
            }
        }
    }

    fun checkAuthenticationStatus(): String {
        return firestoreRepository.checkAuthenticationStatus()
    }
}

data class DatabaseCleanupUiState(
    val isLoading: Boolean = true,
    val message: String = "",
    val isError: Boolean = false,
    val databaseStats: DatabaseStats? = null
)
