// SecureStorage.kt
package com.example.hotwheelscollectors.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    suspend fun store(key: String, value: String) {
        val file = File(context.filesDir, getFileName(key))
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(value.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun retrieve(key: String): String? {
        val file = File(context.filesDir, getFileName(key))
        if (!file.exists()) return null

        return try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileInput().use { inputStream ->
                String(inputStream.readBytes(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearAll() {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("secure_")) {
                file.delete()
            }
        }
    }

    private fun getFileName(key: String): String {
        return "secure_${key.hashCode()}"
    }
}