// EncryptionTest.kt
package com.example.hotwheelscollectors.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.security.KeyStore

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptionTest {
    private lateinit var encryption: Encryption
    private lateinit var context: Context
    private lateinit var keyStore: KeyStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        encryption = Encryption()
    }

    @Test
    fun `encrypt and decrypt returns original data`() = runTest {
        // Given
        val originalData = "sensitive test data"

        // When
        val encrypted = encryption.encrypt(originalData)
        val decrypted = encryption.decrypt(encrypted)

        // Then
        assertThat(decrypted).isEqualTo(originalData)
    }

    @Test
    fun `encrypted data is different from original`() = runTest {
        // Given
        val originalData = "sensitive test data"

        // When
        val encrypted = encryption.encrypt(originalData)

        // Then
        assertThat(encrypted).isNotEqualTo(originalData)
    }

    @Test
    fun `encrypt generates different ciphertext for same input`() = runTest {
        // Given
        val data = "test data"

        // When
        val encrypted1 = encryption.encrypt(data)
        val encrypted2 = encryption.encrypt(data)

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2)
    }

    @Test
    fun `decrypt fails for invalid data`() = runTest {
        // Given
        val invalidData = "invalid encrypted data"

        // When/Then
        try {
            encryption.decrypt(invalidData)
            throw AssertionError("Should have thrown exception")
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `key is stored in AndroidKeyStore`() {
        // When/Then
        assertThat(keyStore.containsAlias("HotWheelsKey")).isTrue()
    }

    @Test
    fun `encrypt handles empty string`() = runTest {
        // Given
        val emptyData = ""

        // When
        val encrypted = encryption.encrypt(emptyData)
        val decrypted = encryption.decrypt(encrypted)

        // Then
        assertThat(decrypted).isEmpty()
    }

    @Test
    fun `encrypt handles special characters`() = runTest {
        // Given
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        // When
        val encrypted = encryption.encrypt(specialChars)
        val decrypted = encryption.decrypt(encrypted)

        // Then
        assertThat(decrypted).isEqualTo(specialChars)
    }
}