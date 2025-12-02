package com.example.hotwheelscollectors.auth

data class AuthResult(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null
)
