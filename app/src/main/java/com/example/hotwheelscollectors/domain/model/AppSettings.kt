// app/src/main/java/com/example/hotwheelscollectors/domain/model/AppSettings.kt
package com.example.hotwheelscollectors.domain.model

data class AppSettings(
    val isDarkTheme: Boolean = false,
    val themeName: String = "Classic", // Classic, Premium, Racing, or Vintage
    val storageLocation: String = "Device", // Device or Cloud
    val lastSyncTime: Long = 0L,
    val notificationsEnabled: Boolean = true
)