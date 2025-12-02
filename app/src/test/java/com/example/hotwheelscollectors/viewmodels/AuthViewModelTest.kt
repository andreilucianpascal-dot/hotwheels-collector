// app/src/test/java/com/example/hotwheelscollectors/viewmodels/AuthViewModelTest.kt
package com.example.hotwheelscollectors.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.auth.AuthResult
import com.example.hotwheelscollectors.viewmodels.AuthState
import com.example.hotwheelscollectors.viewmodels.PasswordResetState
import com.example.hotwheelscollectors.viewmodels.AuthViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        authRepository = mock<AuthRepository>()
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `signInWithEmail success updates state to Success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val authResult = AuthResult("user123", email)
        whenever(authRepository.signInWithEmail(email, password))
            .thenReturn(Result.success(authResult))

        // When
        viewModel.signInWithEmail(email, password)

        // Then
        val authState = viewModel.authState.first()
        assertThat(authState).isEqualTo(AuthState.Success(authResult))
    }

    @Test
    fun `signInWithEmail failure updates state to Error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        whenever(authRepository.signInWithEmail(email, password))
            .thenReturn(Result.failure(Exception("Invalid credentials")))

        // When
        viewModel.signInWithEmail(email, password)

        // Then
        val authState = viewModel.authState.first()
        assertThat(authState).isEqualTo(AuthState.Error("Invalid credentials"))
    }

    @Test
    fun `signOut clears state`() = runTest {
        // When
        viewModel.signOut()

        // Then
        verify(authRepository).signOut()
        val authState = viewModel.authState.first()
        assertThat(authState).isEqualTo(AuthState.SignedOut)
    }
}