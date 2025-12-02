package com.example.hotwheelscollectors.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Initial)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            try {
                val result = authRepository.signInWithEmail(email, password)
                result.fold(
                    onSuccess = { authResult ->
                        _authState.value = AuthState.Success(authResult)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            try {
                // This will be handled by the UI layer with Google Sign-In client
                // The UI will call signInWithGoogleCredential with the ID token
                _authState.value = AuthState.Error("Google Sign-In requires UI implementation")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign in failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogleCredential(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            try {
                val result = authRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { authResult ->
                        _authState.value = AuthState.Success(authResult)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Google sign in failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign in failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogleSuccess(idToken: String) {
        signInWithGoogleCredential(idToken)
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            try {
                val result = authRepository.register(email, password, displayName)
                result.fold(
                    onSuccess = { authResult ->
                        _authState.value = AuthState.Success(authResult)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Registration failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _passwordResetState.value = PasswordResetState.Loading

            try {
                val result = authRepository.resetPassword(email)
                result.fold(
                    onSuccess = {
                        _passwordResetState.value = PasswordResetState.Success
                    },
                    onFailure = { exception ->
                        _passwordResetState.value = PasswordResetState.Error(
                            exception.message ?: "Failed to send reset email"
                        )
                    }
                )
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(
                    e.message ?: "Failed to send reset email"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _authState.value = AuthState.SignedOut
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }

    fun resetPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Initial
    }

    fun clearError() {
        when (_authState.value) {
            is AuthState.Error -> _authState.value = AuthState.Initial
            else -> {}
        }
        when (_passwordResetState.value) {
            is PasswordResetState.Error -> _passwordResetState.value = PasswordResetState.Initial
            else -> {}
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val authResult: AuthResult) : AuthState()
    data class Error(val message: String) : AuthState()
    object SignedOut : AuthState()
}

sealed class PasswordResetState {
    object Initial : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}