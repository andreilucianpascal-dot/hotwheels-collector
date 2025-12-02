package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainViewModel handles navigation logic and business rules for MainScreen.
 * Manages subscription checks, user limits, and navigation conditions.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val carDao: CarDao
) : ViewModel() {

    sealed class NavigationState {
        object IDLE : NavigationState()
        object LOADING : NavigationState()
        object READY : NavigationState()
        data class ERROR(val message: String) : NavigationState()
    }

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.IDLE)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

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
                    // Get cars count from database
                    val stats = carDao.getCollectionStats(user.uid).first()
                    val carsCount = stats?.totalCars ?: 0
                    
                    // Premium check: For production, integrate with subscription service
                    // Currently defaults to true (all features enabled)
                    val isPremium = true // TODO: Implement real premium subscription check
                    
                    _userInfo.value = UserInfo(
                        userId = user.uid,
                        email = user.email ?: "",
                        isPremium = isPremium,
                        carsCount = carsCount
                    )
                }
                
                _navigationState.value = NavigationState.READY
                        } catch (e: Exception) {
                _navigationState.value = NavigationState.ERROR("Failed to load user info: ${e.message}")
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