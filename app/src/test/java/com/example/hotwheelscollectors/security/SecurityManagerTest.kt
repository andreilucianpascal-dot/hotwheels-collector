// SecurityManagerTest.kt
package com.example.hotwheelscollectors.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityManagerTest {
    private lateinit var securityManager: SecurityManager
    private lateinit var encryption: Encryption
    private lateinit var secureStorage: SecureStorage
    private lateinit var authValidator: AuthValidator

    @Before
    fun setup() {
        encryption = mock()
        secureStorage = mock()
        authValidator = mock()
        securityManager = SecurityManager(
            ApplicationProvider.getApplicationContext(),
            encryption,
            secureStorage,
            authValidator
        )
    }

    @Test
    fun `encryptData delegates to encryption`() = runTest {
        // Given
        val data = "sensitive data"
        val encrypted = "encrypted data"
        whenever(encryption.encrypt(data)).thenReturn(encrypted)

        // When
        val result = securityManager.encryptData(data)

        // Then
        assertThat(result).isEqualTo(encrypted)
        verify(encryption).encrypt(data)
    }

    @Test
    fun `validateAuthToken delegates to validator`() {
        // Given
        val token = "valid_token"
        whenever(authValidator.validateToken(token)).thenReturn(true)

        // When
        val result = securityManager.validateAuthToken(token)

        // Then
        assertThat(result).isTrue()
        verify(authValidator).validateToken(token)
    }
}