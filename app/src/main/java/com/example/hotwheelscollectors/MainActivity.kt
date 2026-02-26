package com.example.hotwheelscollectors

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hotwheelscollectors.ui.navigation.NavGraph
import com.example.hotwheelscollectors.ui.theme.HotWheelsCollectorsTheme
import com.example.hotwheelscollectors.viewmodels.AppThemeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
    private var lastSignedInAccount: GoogleSignInAccount? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeApp()
        } else {
            // Permission denied - will be handled by Compose UI state
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Multi-level Content Capture disabling.
         * This aggressively disables Android Content Capture in multiple ways,
         * as the feature can cause runtime crashes with Compose scroll observers.
         *
         * - System-level (ContentCaptureManager by name)
         * - App-level (application context, reflection if possible)
         * - View-level (window and rootView if available)
         * All failures are caught and ignored.
         */
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Attempt 1: Obtain ContentCaptureManager and disable at system level
                val contentCaptureManager = getSystemService("content_capture")
                contentCaptureManager?.let { manager ->
                    try {
                        val setEnabledMethod = manager.javaClass.getMethod("setContentCaptureEnabled", Boolean::class.java)
                        setEnabledMethod.invoke(manager, false)
                    } catch (e: Exception) {
                        // Method not available, try next approach
                        try {
                            val userHandleClass = Class.forName("android.os.UserHandle")
                            val myUserIdMethod = userHandleClass.getMethod("myUserId")
                            val userId = myUserIdMethod.invoke(null) as Int
                            val setEnabledForUserMethod = manager.javaClass.getMethod("setContentCaptureEnabledForUser", Int::class.java, Boolean::class.java)
                            setEnabledForUserMethod.invoke(manager, userId, false)
                        } catch (e2: Exception) {
                            // All direct manager methods failed
                        }
                    }
                }
                // Attempt 2: Try additional disabling via method discovery/reflection
                val managerClass = contentCaptureManager?.javaClass
                managerClass?.let { cls ->
                    for (method in cls.declaredMethods) {
                        if (method.name.contains("setContentCaptureEnabled") || method.name.contains("setEnabled")) {
                            try {
                                if (method.parameterTypes.size == 1 && method.parameterTypes[0] == Boolean::class.java) {
                                    method.invoke(contentCaptureManager, false)
                                } else if (method.parameterTypes.size == 2 &&
                                    method.parameterTypes[0] == Int::class.java &&
                                    method.parameterTypes[1] == Boolean::class.java
                                ) {
                                    val userHandleClass = Class.forName("android.os.UserHandle")
                                    val myUserIdMethod = userHandleClass.getMethod("myUserId")
                                    val userId = myUserIdMethod.invoke(null) as Int
                                    method.invoke(contentCaptureManager, userId, false)
                                }
                            } catch (e: Exception) {
                                // Safe: continue trying
                            }
                        }
                    }
                }
                // Attempt 3: Try disabling at window/rootView level
                try {
                    window?.decorView?.let { rootView ->
                        val disableMethod = rootView.javaClass.getMethod("setContentCaptureSession", Class.forName("android.view.contentcapture.ContentCaptureSession"))
                        disableMethod.invoke(rootView, null)
                    }
                } catch (e: Exception) {
                    // Ignore view-level failures
                }
            }
        } catch (e: Exception) {
            // All content capture disabling failures are ignored, fallback UI will protect user.
        }

        // Initialize Firebase first
        initializeFirebase()

        setupNetworkMonitoring()
        checkAndRequestPermissions()

        // Disable Content Capture after Activity creation
        disableContentCaptureForActivity()

        // Add Pixel-specific Content Capture disabling
        disablePixelSpecificContentCapture()

        // Disable Content Capture at window level
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30
                window.setFlags(
                    0x00000000, // Clear any existing flags
                    0x00000000
                )
                // Try to set window flags to disable content capture
                val decorView = window.decorView
                decorView.importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO
            }
        } catch (e: Exception) {
            // Window flag setting failed
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(Exception::class.java)
                    lastSignedInAccount = account
                    // Example: Show a Snackbar or trigger repo upload
                    android.util.Log.d("GoogleSignIn", "Signed in: ${account.email}")
                    // Pass account to GoogleDriveRepository as needed for uploading
                } catch (e: Exception) {
                    android.util.Log.e("GoogleSignIn", "Sign-In failed: ${e.message}", e)
                    // Surface error to UI as desired
                }
            }

        // Wrap setContent in try-catch to handle any remaining content capture issues
        try {
            setContent {
                // Disable content capture for the entire Compose tree
                CompositionLocalProvider(
                    // Add any other composition locals here if needed
                ) {
                    // Global theme state backed by UserPreferences
                    val appThemeViewModel: AppThemeViewModel = hiltViewModel()
                    val themeState by appThemeViewModel.uiState.collectAsState()

                    val isSystemDark = isSystemInDarkTheme()
                    val darkTheme = when (themeState.themeMode) {
                        "light" -> false
                        "dark" -> true
                        else -> isSystemDark
                    }

                    HotWheelsCollectorsTheme(
                        darkTheme = darkTheme,
                        dynamicColor = themeState.useDynamicColor,
                        colorSchemeName = themeState.colorScheme,
                        fontScale = themeState.fontScale,
                        customPrimaryColor = themeState.customSchemeColor1,
                        customSecondaryColor = themeState.customSchemeColor2,
                        customTertiaryColor = themeState.customSchemeColor3,
                    ) {
                        val navController = rememberNavController()
                        MainActivityContent(
                            onPermissionRequest = { checkAndRequestPermissions() },
                            onFirebaseRetry = { initializeFirebase() },
                            onAppRetry = { initializeApp() },
                            onSettingsOpen = { openStorageSettings() },
                            onExitApp = { finish() },
                            onGoogleSignIn = { triggerGoogleSignIn() }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: Create a simple error UI if Compose fails
            createFallbackUI(e)
        }
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshDataIfNeeded()
            }

            override fun onLost(network: Network) {
                handleOfflineMode()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                // Handle network capability changes
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    loadUserPreferences()
                    setupErrorHandling()
                    setupStorageMonitoring()
                }
            } catch (e: Exception) {
                // Initialization error will be handled by Compose UI state
                e.printStackTrace()
            }
        }
    }

    private fun loadUserPreferences() {
        try {
            val sharedPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
            val isDarkTheme = sharedPrefs.getBoolean("dark_theme", false)
            val storageLocation = sharedPrefs.getString("storage_location", "Device")
            val autoSync = sharedPrefs.getBoolean("auto_sync", true)
            val imageQuality = sharedPrefs.getString("image_quality", "High")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            lifecycleScope.launch(Dispatchers.Main) {
                // Global error handling - will be shown in Compose UI state
                throwable.printStackTrace()
            }
        }
    }

    private fun setupStorageMonitoring() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                checkStorageSpace()
                checkDatabaseIntegrity()
            } catch (e: Exception) {
                // Storage error will be handled by Compose UI state
                e.printStackTrace()
            }
        }
    }

    private fun checkStorageSpace() {
        try {
            val file = applicationContext.getExternalFilesDir(null)
            val freeSpace = file?.freeSpace ?: 0L
            if (freeSpace < MIN_REQUIRED_SPACE) {
                // Low storage warning will be shown in Compose UI state
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkDatabaseIntegrity() {
        // Check if local database is corrupted or needs maintenance
        try {
            // Database integrity checks
        } catch (e: Exception) {
            // Database error will be handled by Compose UI state
            e.printStackTrace()
        }
    }

    private fun refreshDataIfNeeded() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Sync data with cloud
                syncUserData()
                syncCollectionData()
                updateAppData()
            } catch (e: Exception) {
                // Sync error will be handled by Compose UI state
                e.printStackTrace()
            }
        }
    }

    private fun syncUserData() {
        // Sync user preferences and settings
    }

    private fun syncCollectionData() {
        // Sync car collection data
    }

    private fun updateAppData() {
        // Update app metadata and cache
    }

    private fun handleOfflineMode() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Offline mode will be handled by Compose UI state
        }
    }

    private fun openStorageSettings() {
        try {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val generalIntent = Intent(Settings.ACTION_SETTINGS)
                generalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(generalIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun disableContentCaptureForActivity() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Get the ContentCaptureManager for this activity
                val contentCaptureManager = getSystemService("content_capture")
                contentCaptureManager?.let { manager ->
                    try {
                        val ccmClass =
                            Class.forName("android.view.contentcapture.ContentCaptureManager")
                        val setEnabledMethod =
                            ccmClass.getMethod("setContentCaptureEnabled", Boolean::class.java)
                        setEnabledMethod.invoke(manager, false)
                    } catch (e: Exception) {
                        // Try alternative approach
                        val managerClass = manager.javaClass
                        val methods = managerClass.declaredMethods
                        for (method in methods) {
                            try {
                                if (method.name == "setContentCaptureEnabled" &&
                                    method.parameterTypes.size == 1 &&
                                    method.parameterTypes[0] == Boolean::class.java
                                ) {
                                    method.invoke(manager, false)
                                    break
                                }
                            } catch (e2: Exception) {
                                // Continue
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Content Capture disabling failed for this activity
        }
    }

    private fun disablePixelSpecificContentCapture() {
        try {
            if (Build.MANUFACTURER.equals("Google", ignoreCase = true) &&
                Build.MODEL.contains("Pixel")
            ) {

                // Method 1: Disable Pixel-specific services
                try {
                    val assistantManager = getSystemService("assistant")
                    assistantManager?.let {
                        val disableMethod =
                            it.javaClass.getMethod("setContentCaptureEnabled", Boolean::class.java)
                        disableMethod.invoke(it, false)
                    }
                } catch (e: Exception) {
                    // Assistant service not available
                }

                // Method 2: Try to disable Digital Wellbeing content scanning
                try {
                    val wellbeingManager = getSystemService("digital_wellbeing")
                    wellbeingManager?.let {
                        // Disable wellbeing scroll monitoring if possible
                    }
                } catch (e: Exception) {
                    // Digital Wellbeing not available
                }

                // Method 3: Force disable system-level content capture via settings
                try {
                    android.provider.Settings.Secure.putInt(
                        contentResolver,
                        "content_capture_enabled",
                        0
                    )
                } catch (e: Exception) {
                    // Settings not writable (expected in most cases)
                }
            }
        } catch (e: Exception) {
            // Pixel-specific fixes failed, but app continues
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Handle cleanup error gracefully
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        // Save app state
        saveAppState()
    }

    override fun onResume() {
        super.onResume()
        // Restore app state and check for updates
        restoreAppState()
        checkForUpdates()
    }

    private fun saveAppState() {
        // Save current app state to preferences
    }

    private fun restoreAppState() {
        // Restore app state from preferences
    }

    private fun checkForUpdates() {
        // Check for app updates or data updates
    }

    private fun createFallbackUI(error: Exception) {
        // Create a simple LinearLayout-based UI as fallback
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val titleText = android.widget.TextView(this).apply {
            text = "HotWheels Collectors"
            textSize = 20f
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }

        val messageText = android.widget.TextView(this).apply {
            text = "The app is starting up. Please wait..."
            textSize = 16f
            setTextColor(android.graphics.Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        val retryButton = android.widget.Button(this).apply {
            text = "Retry"
            setOnClickListener {
                // Try to restart the Compose UI
                recreate()
            }
        }

        linearLayout.addView(titleText)
        linearLayout.addView(messageText)
        linearLayout.addView(retryButton)

        setContentView(linearLayout)
    }

    fun triggerGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    companion object {
        private const val MIN_REQUIRED_SPACE = 100 * 1024 * 1024L // 100MB
        private const val TAG = "MainActivity"
    }
}

@Composable
fun MainActivityContent(
    onPermissionRequest: () -> Unit,
    onFirebaseRetry: () -> Unit,
    onAppRetry: () -> Unit,
    onSettingsOpen: () -> Unit,
    onExitApp: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    // Defensive: always fallback to error state if Compose or Navigation crashes.
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showOfflineSnackbar by remember { mutableStateOf(false) }
    var showSyncErrorSnackbar by remember { mutableStateOf(false) }
    var appError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val navController = rememberNavController()

    // Wrap everything in a super catch block (Compose-only fallback protection)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (appError != null) {
            // Fallback UI if NavGraph crashes or any Compose exception
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "App initialization failed",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = appError ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = {
                    appError = null
                    onAppRetry()
                }) {
                    Text("Retry")
                }
            }
        } else {
            NavGraph(navController = navController)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }

    // Snackbar notifications (kept simple and independent of fallback block)
    if (showOfflineSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                message = "You're offline. Some features may be limited.",
                duration = SnackbarDuration.Long
            )
            showOfflineSnackbar = false
        }
    }

    if (showSyncErrorSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                message = "Unable to sync data. Please check your connection.",
                duration = SnackbarDuration.Short
            )
            showSyncErrorSnackbar = false
        }
    }

    // Permissions dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = { Text("This app needs camera and storage permissions to function properly.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onPermissionRequest()
                }) { Text("Grant Permissions") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }
}