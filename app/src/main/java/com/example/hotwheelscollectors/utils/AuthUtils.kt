package com.example.hotwheelscollectors.utils

import com.example.hotwheelscollectors.data.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthUtils @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return authRepository.getCurrentUser() != null
    }
    
    /**
     * Get current user ID or throw exception if not authenticated
     */
    fun getCurrentUserIdOrThrow(): String {
        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            throw IllegalStateException("User must be authenticated to perform this action")
        }
        return userId
    }
    
    /**
     * Get current user ID or return null if not authenticated
     */
    fun getCurrentUserIdOrNull(): String? {
        return authRepository.getCurrentUser()?.uid
    }
}
