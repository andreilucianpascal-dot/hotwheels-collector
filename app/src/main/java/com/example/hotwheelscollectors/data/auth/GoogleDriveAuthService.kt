package com.example.hotwheelscollectors.data.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveAuthService @Inject constructor(
    private val context: Context
) {
    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(com.example.hotwheelscollectors.R.string.default_web_client_id))
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Check if user is currently signed in to Google Drive
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && account.grantedScopes?.contains(Scope(DriveScopes.DRIVE_FILE)) == true
    }

    /**
     * Get the current signed-in account
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Get the sign-in intent for launching the authentication flow
     */
    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    /**
     * Handle the sign-in result
     */
    suspend fun handleSignInResult(data: Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            
            if (account.grantedScopes?.contains(Scope(DriveScopes.DRIVE_FILE)) == true) {
                GoogleSignInResult.Success(account)
            } else {
                GoogleSignInResult.Error("Google Drive access not granted")
            }
        } catch (e: ApiException) {
            GoogleSignInResult.Error("Sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            GoogleSignInResult.Error("Sign-in failed: ${e.message}")
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut() {
        signInClient.signOut().await()
    }

    /**
     * Revoke access and sign out
     */
    suspend fun revokeAccess() {
        signInClient.revokeAccess().await()
    }

    /**
     * Get the user's display name
     */
    fun getUserDisplayName(): String? {
        return getCurrentAccount()?.displayName
    }

    /**
     * Get the user's email
     */
    fun getUserEmail(): String? {
        return getCurrentAccount()?.email
    }
}

sealed class GoogleSignInResult {
    data class Success(val account: GoogleSignInAccount) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
