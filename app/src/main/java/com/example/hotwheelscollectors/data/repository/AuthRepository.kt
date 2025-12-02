package com.example.hotwheelscollectors.data.repository

import com.example.hotwheelscollectors.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser

    fun isUserLoggedIn() = auth.currentUser != null
    
    fun authStateChanges(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        
        // Send current user immediately
        trySend(auth.currentUser)
        
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        AuthResult(
            userId = result.user?.uid ?: "",
            email = result.user?.email ?: "",
            displayName = result.user?.displayName,
            photoUrl = result.user?.photoUrl?.toString()
        )
    }

    suspend fun signInWithGoogle(idToken: String): Result<AuthResult> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        AuthResult(
            userId = result.user?.uid ?: "",
            email = result.user?.email ?: "",
            displayName = result.user?.displayName,
            photoUrl = result.user?.photoUrl?.toString()
        )
    }

    suspend fun register(email: String, password: String, displayName: String): Result<AuthResult> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
        )?.await()
        AuthResult(
            userId = result.user?.uid ?: "",
            email = result.user?.email ?: "",
            displayName = displayName,
            photoUrl = null
        )
    }
}