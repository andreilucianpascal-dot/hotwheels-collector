package com.example.hotwheelscollectors.domain.usecase.auth

import com.example.hotwheelscollectors.data.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }
        return authRepository.login(email, password)
    }
}