package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.StorageOrchestrator
import com.example.hotwheelscollectors.data.auth.GoogleDriveAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * MainViewModel handles navigation logic and business rules for MainScreen.
 * Manages subscription checks, user limits, and navigation conditions.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val carDao: CarDao,
    private val storageOrchestrator: StorageOrchestrator,
    private val googleDriveAuthService: GoogleDriveAuthService
) : ViewModel() {

    sealed class NavigationState {
        object IDLE : NavigationState()
        object LOADING : NavigationState()
        object READY : NavigationState()
        data class ERROR(val message: String) : NavigationState()
    }

    sealed class UiEvent {
        object RequireDriveLogin : UiEvent()
    }

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.IDLE)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _uiEvents = kotlinx.coroutines.flow.MutableSharedFlow<UiEvent>(replay = 1)
    val uiEvents = _uiEvents.asSharedFlow()

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                _navigationState.value = NavigationState.LOADING
                val user = authRepository.getCurrentUser()
                
                if (user != null) {
                    // ✅ OPTIMIZATION: Set user info immediately without blocking on stats query
                    // Allow UI to render immediately, load stats in background
                    _userInfo.value = UserInfo(
                        userId = user.uid,
                        email = user.email ?: "",
                        isPremium = true, // TODO: Implement real premium subscription check
                        carsCount = 0 // Will be updated async below
                    )
                    
                    // Set READY state immediately - don't block navigation
                    _navigationState.value = NavigationState.READY
                    
                    // ✅ Load stats asynchronously in background (non-blocking)
                    launch(Dispatchers.IO) {
                        try {
                            val stats = carDao.getCollectionStats(user.uid).first()
                            val carsCount = stats?.totalCars ?: 0
                            
                            // Update user info with actual count (non-blocking)
                            _userInfo.value = _userInfo.value?.copy(carsCount = carsCount)
                        } catch (e: Exception) {
                            // Silently fail - stats are not critical for initial UI
                            android.util.Log.w("MainViewModel", "Failed to load collection stats: ${e.message}")
                        }
                    }
                    
                    // ✅ Orchestrate storage settings restore on app start and detect if Drive login is required
                    launch(Dispatchers.IO) {
                        try {
                            android.util.Log.d("MainViewModel", "Orchestrating storage settings restore on app start...")
                            val startupState = storageOrchestrator.onAppStart()
                            
                            when (startupState) {
                                com.example.hotwheelscollectors.data.repository.StorageStartupState.READY -> {
                                    android.util.Log.i("MainViewModel", "✅ Storage is ready - app can proceed")
                                }
                                com.example.hotwheelscollectors.data.repository.StorageStartupState.DRIVE_LOGIN_REQUIRED -> {
                                    android.util.Log.w("MainViewModel", "⚠️ Drive login required - navigating to Settings")
                                    _uiEvents.emit(UiEvent.RequireDriveLogin)
                                }
                            }
                        } catch (e: Exception) {
                            // Don't crash app if storage restore fails
                            android.util.Log.e("MainViewModel", "Failed to orchestrate storage restore", e)
                        }
                    }
                } else {
                    _navigationState.value = NavigationState.READY
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error loading user info: ${e.message}", e)
                _navigationState.value = NavigationState.READY // ✅ Don't block on error
            }
        }
    }

    fun canAddMainline(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canAddPremium(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canAddTreasureHunt(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canAddSuperTreasureHunt(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canAddOthers(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canViewCollection(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium && user.carsCount > 0
    }

    fun canExport(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun canBrowseGlobal(): Boolean {
        val user = _userInfo.value ?: return false
        return user.isPremium
    }

    fun getSubscriptionRequiredMessage(): String {
        return "This app requires a Premium subscription. Subscribe now to access all features!"
    }

    fun getEmptyCollectionMessage(): String {
        return "Your collection is empty. Start by adding your first car!"
    }

    fun refreshUserInfo() {
        loadUserInfo()
    }

    companion object {
        // No free limits - app is 100% paid
    }

    data class UserInfo(
        val userId: String,
        val email: String,
        val isPremium: Boolean,
        val carsCount: Int
    )
}