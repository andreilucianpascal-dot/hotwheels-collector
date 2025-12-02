// app/src/main/java/com/example/hotwheelscollectors/auth/PasswordResetState.kt
package com.example.hotwheelscollectors.auth

sealed class PasswordResetState {
    object Initial : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}