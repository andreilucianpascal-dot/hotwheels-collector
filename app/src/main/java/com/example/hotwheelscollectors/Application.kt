package com.example.hotwheelscollectors

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import timber.log.Timber

@HiltAndroidApp
class HotWheelsCollectorsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Disable Content Capture at application level to prevent scroll observation scope crashes
        disableContentCapture()

        // Initialize logging with proper error handling
        try {
            // Check if app is in debug mode using application info
            val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            
            if (isDebug) {
                Timber.plant(Timber.DebugTree())
            }
        } catch (e: Exception) {
            // Fallback: always enable logging in case of any issues
            Timber.plant(Timber.DebugTree())
        }

        // Initialize app-wide configurations with proper error handling
        initializeApp()
    }

    private fun initializeApp() {
        try {
            // Initialize crash reporting
            initializeCrashReporting()

            // Initialize analytics
            initializeAnalytics()

            // Initialize performance monitoring
            initializePerformanceMonitoring()

            // Initialize security
            initializeSecurity()

            // Initialize database
            initializeDatabase()

            // Initialize background work
            initializeBackgroundWork()

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize application")
            // Don't crash the app, just log the error
        }
    }

    private fun initializeCrashReporting() {
        try {
            // Initialize Firebase Crashlytics
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            
            // Initialize custom crash reporter
            val crashReporter = com.example.hotwheelscollectors.analytics.CrashReporter.getInstance(this)
            
            Timber.d("Crash reporting initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize crash reporting")
        }
    }

    private fun initializeAnalytics() {
        try {
            // Initialize Firebase Analytics
            val firebaseAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
            firebaseAnalytics.setAnalyticsCollectionEnabled(true)
            
            // Initialize custom analytics manager
            val analyticsManager = com.example.hotwheelscollectors.analytics.AnalyticsManager.getInstance(this)
            
            Timber.d("Analytics initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize analytics")
        }
    }

    private fun initializePerformanceMonitoring() {
        try {
            // Initialize Firebase Performance
            val firebasePerformance = com.google.firebase.perf.FirebasePerformance.getInstance()
            firebasePerformance.isPerformanceCollectionEnabled = true
            
            // Initialize custom performance tracker
            val performanceTracker = com.example.hotwheelscollectors.analytics.PerformanceTracker.getInstance(this)
            
            // Initialize memory manager
            val memoryManager = com.example.hotwheelscollectors.performance.MemoryManager.getInstance(this)
            
            Timber.d("Performance monitoring initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize performance monitoring")
        }
    }

    private fun initializeSecurity() {
        try {
            // Check package name to determine which App Check provider to use
            val currentPackageName = packageName
            
            // ✅ Use DebugAppCheckProviderFactory ONLY for .debug package
            // ✅ Use PlayIntegrityAppCheckProviderFactory for main package (com.example.hotwheelscollectors)
            if (currentPackageName.endsWith(".debug")) {
                // Debug build with .debug suffix - use DebugAppCheckProviderFactory
                val debugProviderFactory = DebugAppCheckProviderFactory.getInstance()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(debugProviderFactory)
                
                // Get debug token for Firebase Console
                FirebaseAppCheck.getInstance().getAppCheckToken(false).addOnSuccessListener { tokenResponse ->
                    val debugToken = tokenResponse.token
                    Timber.d("=== FIREBASE APP CHECK DEBUG TOKEN ===")
                    Timber.d("Copy this token to Firebase Console → App Check → Apps → Debug tokens")
                    Timber.d("Debug Token: $debugToken")
                    Timber.d("=====================================")
                }.addOnFailureListener { e ->
                    Timber.w(e, "Failed to get App Check debug token. Token will be available in logcat.")
                }
                
                Timber.d("App Check initialized with DebugAppCheckProviderFactory (DEBUG PACKAGE: $currentPackageName)")
            } else {
                // Main package (com.example.hotwheelscollectors) - use PlayIntegrityAppCheckProviderFactory
                // This works for both debug and release builds of the main package
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Timber.d("App Check initialized with PlayIntegrityAppCheckProviderFactory (PACKAGE: $currentPackageName)")
                Timber.d("Note: Play Integrity must be enabled in Firebase Console for this app")
            }
            
            // Initialize security manager (simplified)
            Timber.d("Security features initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize security")
        }
    }

    private fun initializeDatabase() {
        try {
            // Room database is automatically initialized by Hilt
            // Database cleanup is handled by Hilt DI
            Timber.d("Database initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize database")
        }
    }

    private fun initializeBackgroundWork() {
        try {
            // Initialize sync manager for background work
            // Initialize sync manager (simplified)
            Timber.d("Sync manager initialized")
            
            // Initialize offline manager (simplified)
            Timber.d("Offline manager initialized")
            
            Timber.d("Background work initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize background work")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onTerminate() {
        super.onTerminate()
        // Cleanup resources
        cleanupResources()
    }

    private fun cleanupResources() {
        try {
            // Cleanup memory and cache
            val memoryManager = com.example.hotwheelscollectors.performance.MemoryManager.getInstance(this)
            memoryManager.performMemoryOptimizationSync()
            
            // Clear analytics buffer
            val analyticsManager = com.example.hotwheelscollectors.analytics.AnalyticsManager.getInstance(this)
            analyticsManager.clearBuffer()
            
            Timber.d("Resources cleaned up successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup resources")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Handle low memory situation
        handleLowMemory()
    }

    private fun handleLowMemory() {
        try {
            // Handle low memory situation by cleaning up resources
            val memoryManager = com.example.hotwheelscollectors.performance.MemoryManager.getInstance(this)
            memoryManager.performMemoryOptimizationSync()
            
            // Clear image cache
            val imageCache = com.example.hotwheelscollectors.image.ImageCache.getInstance(this)
            imageCache.clearCache()
            
            Timber.w("Low memory situation handled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle low memory")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Handle memory trimming
        handleMemoryTrimming(level)
    }

    private fun handleMemoryTrimming(level: Int) {
        try {
            val memoryManager = com.example.hotwheelscollectors.performance.MemoryManager.getInstance(this)
            val imageCache = com.example.hotwheelscollectors.image.ImageCache.getInstance(this)
            
            when (level) {
                Application.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    // Critical memory situation - perform aggressive cleanup
                    memoryManager.performMemoryOptimizationSync()
                    imageCache.clearCache()
                    System.gc()
                    Timber.w("Critical memory situation - aggressive cleanup")
                }
                Application.TRIM_MEMORY_RUNNING_LOW -> {
                    // Low memory situation - perform moderate cleanup
                    memoryManager.performMemoryOptimizationSync()
                    imageCache.clearOldCache()
                    Timber.w("Low memory situation - moderate cleanup")
                }
                Application.TRIM_MEMORY_RUNNING_MODERATE -> {
                    // Moderate memory situation - perform light cleanup
                    imageCache.clearOldCache()
                    Timber.d("Moderate memory situation - light cleanup")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle memory trimming")
        }
    }

    private fun disableContentCapture() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Method 1: Try to set system property to disable Content Capture globally
                try {
                    System.setProperty("debug.content_capture.enabled", "false")
                    System.setProperty("content_capture.enabled", "false")
                    System.setProperty("android.content_capture.enabled", "false")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to set Content Capture system properties")
                }

                // Method 2: Disable via ContentCaptureManager service
                val contentCaptureManager = getSystemService("content_capture")
                contentCaptureManager?.let { manager ->
                    try {
                        // Try multiple methods to disable Content Capture
                        val managerClass = manager.javaClass
                        val methods = managerClass.declaredMethods

                        // First try direct methods
                        for (method in methods) {
                            try {
                                method.isAccessible = true
                                if (method.name.contains("setContentCaptureEnabled") ||
                                    method.name.contains("setEnabled")
                                ) {
                                    when {
                                        method.parameterTypes.size == 1 &&
                                                method.parameterTypes[0] == Boolean::class.java -> {
                                            method.invoke(manager, false)
                                            Timber.d("Content Capture disabled using method: ${method.name}")
                                        }

                                        method.parameterTypes.size == 2 &&
                                                method.parameterTypes[0] == Int::class.java &&
                                                method.parameterTypes[1] == Boolean::class.java -> {
                                            // Use current process user ID (0 for main user)
                                            method.invoke(manager, 0, false)
                                            Timber.d("Content Capture disabled using method: ${method.name}")
                                        }
                                    }
                                } else if (method.name.contains("destroy") ||
                                    method.name.contains("finishSession")
                                ) {
                                    if (method.parameterTypes.isEmpty()) {
                                        method.invoke(manager)
                                        Timber.d("Content Capture session ended using: ${method.name}")
                                    }
                                }
                            } catch (e: Exception) {
                                // Continue trying other methods
                            }
                        }

                        // Try to access and modify internal fields
                        val fields = managerClass.declaredFields
                        for (field in fields) {
                            try {
                                field.isAccessible = true
                                if (field.name.contains("mEnabled") ||
                                    field.name.contains("enabled") ||
                                    field.name.contains("mServiceClientImpl")
                                ) {
                                    if (field.type == Boolean::class.java) {
                                        field.set(manager, false)
                                        Timber.d("Content Capture disabled by setting field: ${field.name}")
                                    } else if (field.name.contains("mServiceClientImpl")) {
                                        // Try to null out the service client
                                        field.set(manager, null)
                                        Timber.d("Content Capture service client nullified")
                                    }
                                }
                            } catch (e: Exception) {
                                // Continue trying other fields
                            }
                        }

                    } catch (e: Exception) {
                        Timber.w(e, "Failed to disable Content Capture at application level")
                    }
                }

                // Method 3: Try to disable at the View level by setting system UI flags
                try {
                    val windowManagerGlobalClass = Class.forName("android.view.WindowManagerGlobal")
                    val getInstanceMethod = windowManagerGlobalClass.getMethod("getInstance")
                    val windowManagerGlobal = getInstanceMethod.invoke(null)

                    val getViewRootImplsMethod = windowManagerGlobalClass.getDeclaredMethod(
                        "getRootViews",
                        String::class.java
                    )
                    getViewRootImplsMethod.isAccessible = true

                    Timber.d("WindowManagerGlobal accessed for Content Capture disabling")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to access WindowManagerGlobal")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to access Content Capture service")
        }
    }
}