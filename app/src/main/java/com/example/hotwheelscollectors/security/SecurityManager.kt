// SecurityManager.kt
package com.example.hotwheelscollectors.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context,
    private val encryption: Encryption,
    private val secureStorage: SecureStorage,
    private val authValidator: AuthValidator
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun encryptData(data: String): String {
        return encryption.encrypt(data)
    }

    suspend fun decryptData(encryptedData: String): String {
        return encryption.decrypt(encryptedData)
    }

    suspend fun storeSecurely(key: String, value: String) {
        secureStorage.store(key, value)
    }

    suspend fun retrieveSecurely(key: String): String? {
        return secureStorage.retrieve(key)
    }

    fun validateAuthToken(token: String): Boolean {
        return authValidator.validateToken(token)
    }

    fun secureUserData(userId: String) {
        encryptedPrefs.edit()
            .putString("user_id", userId)
            .apply()
    }

    fun clearSecureData() {
        encryptedPrefs.edit().clear().apply()
        secureStorage.clearAll()
    }
}