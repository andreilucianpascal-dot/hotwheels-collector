// app/src/main/java/com/example/hotwheelscollectors/domain/model/User.kt
package com.example.hotwheelscollectors.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isGuest: Boolean = false,
    val lastLoginTime: Long = 0L
)