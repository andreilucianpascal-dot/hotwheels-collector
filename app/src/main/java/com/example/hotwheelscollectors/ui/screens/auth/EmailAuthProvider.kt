package com.example.hotwheelscollectors.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailAuthProvider @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun signIn(email: String, password: String): Result<AuthResult> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(
            AuthResult(
                userId = result.user?.uid ?: "",
                email = result.user?.email ?: "",
                displayName = result.user?.displayName,
                photoUrl = result.user?.photoUrl?.toString()
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(email: String, password: String, displayName: String): Result<AuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
        )?.await()
        Result.success(
            AuthResult(
                userId = result.user?.uid ?: "",
                email = result.user?.email ?: "",
                displayName = displayName,
                photoUrl = null
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resetPassword(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val email = auth.currentUser?.email
                ?: return Result.failure(Exception("User not signed in"))

            // Reauthenticate
            auth.signInWithEmailAndPassword(email, currentPassword).await()

            // Update password
            auth.currentUser?.updatePassword(newPassword)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val email = auth.currentUser?.email
                ?: return Result.failure(Exception("User not signed in"))

            // Reauthenticate
            auth.signInWithEmailAndPassword(email, password).await()

            // Delete account
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(displayName: String? = null, photoUrl: String? = null): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not signed in"))

            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()

            currentUser.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser
    fun isUserSignedIn() = auth.currentUser != null
}