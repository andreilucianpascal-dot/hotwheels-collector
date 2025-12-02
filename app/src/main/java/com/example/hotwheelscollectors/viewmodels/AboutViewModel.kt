package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val installDate = Date(packageInfo.firstInstallTime)
                val lastUpdateDate = Date(packageInfo.lastUpdateTime)

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                _uiState.value = _uiState.value.copy(
                    appVersion = packageInfo.versionName ?: "Unknown",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    },
                    buildType = BuildConfig.BUILD_TYPE,
                    installDate = dateFormat.format(installDate),
                    lastUpdateDate = dateFormat.format(lastUpdateDate),
                    packageName = context.packageName
                )
            } catch (e: Exception) {
                // Fallback to default values if package info is unavailable
                _uiState.value = _uiState.value.copy(
                    appVersion = "1.0.0",
                    versionCode = "1",
                    buildType = "release",
                    packageName = context.packageName
                )
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Checking

                // Simulate update check with Play Store
                val isUpdateAvailable = checkPlayStoreForUpdates()

                if (isUpdateAvailable) {
                    _updateState.value = UpdateState.UpdateAvailable(
                        newVersion = "1.0.1",
                        updateDescription = "Bug fixes and performance improvements",
                        isForced = false
                    )
                } else {
                    _updateState.value = UpdateState.UpToDate
                }

            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Failed to check for updates: ${e.message}")
            }
        }
    }

    private suspend fun checkPlayStoreForUpdates(): Boolean {
        // In a real implementation, you would:
        // 1. Use Google Play Core Library's AppUpdateManager
        // 2. Or make API call to your backend to check latest version
        // 3. Compare with current version

        // For now, simulate update check
        kotlinx.coroutines.delay(2000) // Simulate network request

        // Return false for demo - no update available
        return false
    }

    fun dismissUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun getDebugInfo(): String {
        val currentState = _uiState.value
        return buildString {
            appendLine("=== Debug Information ===")
            appendLine("App Version: ${currentState.appVersion}")
            appendLine("Version Code: ${currentState.versionCode}")
            appendLine("Build Type: ${currentState.buildType}")
            appendLine("Package Name: ${currentState.packageName}")
            appendLine("Install Date: ${currentState.installDate}")
            appendLine("Last Update: ${currentState.lastUpdateDate}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Architecture: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
        }
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        object UpToDate : UpdateState()
        data class UpdateAvailable(
            val newVersion: String,
            val updateDescription: String,
            val isForced: Boolean,
        ) : UpdateState()

        data class Error(val message: String) : UpdateState()
    }
}

data class AboutUiState(
    val appVersion: String = "Loading...",
    val versionCode: String = "",
    val buildType: String = "release",
    val installDate: String = "",
    val lastUpdateDate: String = "",
    val packageName: String = "",
    val buildNumber: String = "1",
    val helpCenterUrl: String = "https://help.hotwheelscollectors.com",
    val bugReportUrl: String = "https://bugs.hotwheelscollectors.com",
    val feedbackUrl: String = "https://feedback.hotwheelscollectors.com",
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.example.hotwheelscollectors",
    val shareMessage: String = "Check out this awesome die-cast car collectors app!",
)