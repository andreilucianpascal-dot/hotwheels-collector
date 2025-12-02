package com.example.hotwheelscollectors.domain.usecase.auth

import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): Result<Unit> {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        if (password != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords don't match"))
        }

        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        return try {
            // Register user
            authRepository.register(email, password).getOrThrow()

            // Initialize user preferences
            preferencesRepository.updateSettings(
                preferencesRepository.getSettings().first().copy(
                    // Set any default settings for new users
                    isDarkTheme = false,
                    themeName = "Classic",
                    storageLocation = "Device"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}