// GoogleAuthProvider.kt
package com.example.hotwheelscollectors.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthProvider @Inject constructor(
    private val auth: FirebaseAuth
) {
    private lateinit var googleSignInClient: GoogleSignInClient

    fun initialize(context: Context, webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    suspend fun signIn(idToken: String): Result<AuthResult> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
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

    fun getSignInClient() = googleSignInClient

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            // Handle sign out error
        }
    }
}