// AuthValidator.kt
package com.example.hotwheelscollectors.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthValidator @Inject constructor(
    private val auth: FirebaseAuth
) {
    fun validateToken(token: String): Boolean {
        return try {
            auth.currentUser?.getIdToken(false)?.result?.token == token
        } catch (e: Exception) {
            false
        }
    }

    fun validateUser(user: FirebaseUser?): Boolean {
        return user?.let {
            it.isEmailVerified && !it.isAnonymous
        } ?: false
    }

    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(emailRegex.toRegex())
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { !it.isLetterOrDigit() }
    }

    fun requiresReauthentication(exception: Exception): Boolean {
        return exception.message?.contains("requires recent authentication") == true
    }
}