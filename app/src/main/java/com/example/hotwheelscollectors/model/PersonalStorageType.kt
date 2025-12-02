package com.example.hotwheelscollectors.model

enum class PersonalStorageType(val value: String, val displayName: String) {
    LOCAL("LOCAL", "Local Storage"),
    GOOGLE_DRIVE("GOOGLE_DRIVE", "Google Drive");
    
    companion object {
        fun fromString(value: String): PersonalStorageType {
            return values().find { it.value == value } ?: LOCAL
        }
    }
}
