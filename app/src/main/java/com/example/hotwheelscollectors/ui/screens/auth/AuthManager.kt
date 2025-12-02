// AuthManager.kt
package com.example.hotwheelscollectors.auth

import com.example.hotwheelscollectors.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<AuthResult?>(null)
    val currentUser: StateFlow<AuthResult?> = _currentUser.asStateFlow()

    fun setCurrentUser(user: AuthResult) {
        _currentUser.value = user
    }

    fun clearCurrentUser() {
        _currentUser.value = null
    }
}

