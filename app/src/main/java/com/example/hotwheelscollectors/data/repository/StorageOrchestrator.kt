package com.example.hotwheelscollectors.data.repository

import android.util.Log
import com.example.hotwheelscollectors.data.auth.GoogleDriveAuthService
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.model.PersonalStorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StorageOrchestrator - Centralizes all storage-related logic.
 * 
 * RESPONSIBILITIES:
 * 1. Decide when to restore/sync data based on storage mode
 * 2. Handle storage migrations (Local ↔ Drive) - REAL migrations
 * 3. Orchestrate data sync on app start
 * 
 * ARCHITECTURAL RULE:
 * - Application does NOT know about storage logic
 * - ViewModels do NOT migrate data directly
 * - StorageOrchestrator is the SINGLE SOURCE OF TRUTH for storage operations
 */
/**
 * Result of storage initialization on app start.
 * Indicates whether storage is ready or requires user action.
 */
enum class StorageStartupState {
    /** Storage is ready, app can proceed normally */
    READY,
    /** Google Drive is PRIMARY but user is not authenticated - login required */
    DRIVE_LOGIN_REQUIRED
}

@Singleton
class StorageOrchestrator @Inject constructor(
    private val userPreferences: UserPreferences,
    private val googleDriveAuthService: GoogleDriveAuthService,
    private val googleDriveRepository: GoogleDriveRepository,
    private val cloudUserSettingsRepository: CloudUserSettingsRepository
) {
    
    companion object {
        private const val TAG = "StorageOrchestrator"
    }
    
    
    /**
     * One place for the Drive-primary startup flow:
     * - sync Room cache from db.json
     * - download photos locally (Drive files are private; URL loading isn't reliable without OAuth)
     */
    private suspend fun syncAndDownloadIfDrivePrimary(): Result<Unit> = withContext(Dispatchers.IO) {
        val storageType = userPreferences.storageType.first()
        if (storageType != PersonalStorageType.GOOGLE_DRIVE) return@withContext Result.success(Unit)

        if (!googleDriveAuthService.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("User not signed in to Google Drive"))
        }

        val syncResult = googleDriveRepository.syncRoomFromDrive()
        if (syncResult.isFailure) return@withContext syncResult

        val driveDb = googleDriveRepository.loadDbJson().getOrNull()
        if (driveDb != null && driveDb.tables.photos.isNotEmpty()) {
            Log.d(TAG, "Downloading photos locally after Drive sync...")
            googleDriveRepository.downloadAllPhotosLocally(driveDb)
        }

        Result.success(Unit)
    }/**
     * Called when app starts.
     * Orchestrates initial settings restore and detects if Drive login is required.
     * 
     * ✅ RESPONSIBILITY:
     * - Restore primaryStorage from cloud (if available)
     * - Detect if Drive login is required (Drive PRIMARY but user not authenticated)
     * 
     * ❌ DOES NOT: Sync data (that happens in onUserAuthenticated())
     * 
     * @return StorageStartupState indicating if storage is ready or login is required
     */
    suspend fun onAppStart(): StorageStartupState = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STORAGE ORCHESTRATOR: APP START ===")
            Log.d(TAG, "Restoring primary storage from cloud...")
            
            // ✅ STEP 1: Restore primaryStorage from cloud (CRITICAL for restore after uninstall)
            // NOTE: This may return null if user is not signed in to Drive yet - that's OK
            val restoreResult = cloudUserSettingsRepository.restorePrimaryStorageFromCloud()
            if (restoreResult.isSuccess) {
                val cloudStorageType = restoreResult.getOrNull()
                if (cloudStorageType != null) {
                    Log.i(TAG, "✅ Primary storage restored from cloud: $cloudStorageType")
                } else {
                    Log.d(TAG, "No cloud settings found - using local settings")
                }
            } else {
                Log.w(TAG, "⚠️ Failed to restore primary storage from cloud: ${restoreResult.exceptionOrNull()?.message}")
                // Continue with local settings (fallback)
            }
            
            // ✅ STEP 2: Check current storage type (may have been restored from cloud)
            val storageType = userPreferences.storageType.first()
            Log.d(TAG, "Current storage type after restore: $storageType")
            
            // ✅ STEP 3: Detect if Drive login is required
            if (storageType == PersonalStorageType.GOOGLE_DRIVE) {
                val isSignedIn = googleDriveAuthService.isSignedIn()
                if (!isSignedIn) {
                    Log.w(TAG, "⚠️ Drive is PRIMARY but user not signed in → LOGIN REQUIRED")
                    Log.d(TAG, "UI must navigate to Settings → Google Drive Login")
                    return@withContext StorageStartupState.DRIVE_LOGIN_REQUIRED
                } else {
                    Log.d(TAG, "✅ Drive is PRIMARY and user is signed in")
                                        // âœ… FIX: On reinstall, user may already be signed in via Google Play Services.
                    // Sync NOW; otherwise My Collection stays empty until user toggles settings.
                    val sync = syncAndDownloadIfDrivePrimary()
                    if (sync.isSuccess) {
                        Log.i(TAG, "âœ… Auto-restore completed on app start (Drive primary)")
                    } else {
                        Log.w(TAG, "âš ï¸ Auto-restore failed on app start: ${sync.exceptionOrNull()?.message}")
                    }
                }
            } else {
                Log.d(TAG, "Local is PRIMARY - no Drive login required")
            }
            
            Log.d(TAG, "Storage is ready - app can proceed")
            StorageStartupState.READY
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to orchestrate storage on app start", e)
            // On error, assume ready (fallback to local)
            StorageStartupState.READY
        }
    }
    
    /**
     * Called when user successfully authenticates to Google Drive.
     * Orchestrates data restore/sync from Drive.
     * 
     * ✅ RESPONSIBILITY: Restore data from Drive if Drive is PRIMARY
     * 
     * Flow:
     * - Restore primaryStorage from cloud (guaranteed to work now - user is signed in)
     * - If Drive PRIMARY: Load db.json → Clear Room → Populate Room
     * - If Local PRIMARY: Do nothing (Room is truth)
     * 
     * ⚠️ CRITICAL: This MUST be called AFTER Google Drive sign-in succeeds
     */
    suspend fun onUserAuthenticated(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STORAGE ORCHESTRATOR: USER AUTHENTICATED ===")
            Log.d(TAG, "User is now signed in to Google Drive - checking storage settings...")
            
            // ✅ STEP 1: Restore primaryStorage from cloud (now guaranteed to work - user is signed in)
            Log.d(TAG, "Step 1: Restoring primary storage from cloud (user is authenticated)...")
            val restoreResult = cloudUserSettingsRepository.restorePrimaryStorageFromCloud()
            if (restoreResult.isSuccess) {
                val cloudStorageType = restoreResult.getOrNull()
                if (cloudStorageType != null) {
                    Log.i(TAG, "✅ Primary storage restored from cloud: $cloudStorageType")
                } else {
                    Log.d(TAG, "No cloud settings found - using local settings")
                }
            } else {
                Log.w(TAG, "⚠️ Failed to restore primary storage from cloud: ${restoreResult.exceptionOrNull()?.message}")
                // Continue with local settings (fallback)
            }
            
            // ✅ STEP 2: Check current storage type (may have been restored from cloud)
            val storageType = userPreferences.storageType.first()
            Log.d(TAG, "Current storage type: $storageType")

            when (storageType) {
                PersonalStorageType.GOOGLE_DRIVE -> {
                    Log.d(TAG, "Drive is PRIMARY - syncing + downloading photos after authentication...")
                    syncAndDownloadIfDrivePrimary()
                }

                PersonalStorageType.LOCAL -> {
                    // Local is PRIMARY - Room is truth, do nothing
                    Log.d(TAG, "Local is PRIMARY - Room is truth, no sync needed")
                    Result.success(Unit)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to orchestrate storage after user authentication", e)
            Result.failure(e)
        }
    }
    
    /**
     * Called when user changes storage mode.
     * Orchestrates REAL migration between storage types.
     * 
     * Flow:
     * Local → Drive: Export local → Upload db.json → Upload photos → Clear Room → Load from Drive
     * Drive → Local: Load db.json → Clear Room → Save to Room → Download photos
     */
    suspend fun onStorageChanged(
        from: PersonalStorageType,
        to: PersonalStorageType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STORAGE ORCHESTRATOR: STORAGE CHANGED ===")
            Log.d(TAG, "From: $from → To: $to")
            
            when {
                // Local → Drive: REAL migration
                from == PersonalStorageType.LOCAL && to == PersonalStorageType.GOOGLE_DRIVE -> {
                    Log.d(TAG, "Migrating from Local to Drive (REAL migration)...")
                    migrateLocalToDrive()
                }
                
                // Drive → Local: REAL migration
                from == PersonalStorageType.GOOGLE_DRIVE && to == PersonalStorageType.LOCAL -> {
                    Log.d(TAG, "Migrating from Drive to Local (REAL migration)...")
                    migrateDriveToLocal()
                }
                
                // Same storage type: Do nothing
                from == to -> {
                    Log.d(TAG, "Storage type unchanged - no migration needed")
                    Result.success(Unit)
                }
                
                else -> {
                    Log.w(TAG, "Unknown migration path: $from → $to")
                    Result.success(Unit)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to orchestrate storage migration", e)
            Result.failure(e)
        }
    }
    
    /**
     * REAL migration from Local to Google Drive.
     * 
     * Flow OBLIGATORIU:
     * 1. Export local DB → DatabaseExport
     * 2. Upload db.json to Drive (OVERWRITE)
     * 3. Upload all local photos to Drive
     * 4. Clear Room (devine cache)
     * 5. Reload Room from Drive db.json
     */
    private suspend fun migrateLocalToDrive(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== MIGRATING LOCAL → DRIVE (REAL) ===")
            
            // 1️⃣ Export local DB
            Log.d(TAG, "Step 1: Exporting local database...")
            val exportResult = googleDriveRepository.exportLocalDatabase()
            if (exportResult.isFailure) {
                Log.e(TAG, "❌ Failed to export local database: ${exportResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    exportResult.exceptionOrNull() ?: Exception("Failed to export local database")
                )
            }
            val localData = exportResult.getOrThrow()
            Log.i(TAG, "✅ Local database exported: ${localData.tables.cars.size} cars, ${localData.tables.photos.size} photos")
            
            
            // âœ… SAFETY: fresh reinstall (local empty) -> restore from Drive, DO NOT overwrite Drive
            if (localData.tables.cars.isEmpty() && localData.tables.photos.isEmpty()) {
                Log.w(TAG, "âš ï¸ Local DB is empty. Checking Drive db.json to restore instead of overwriting...")
                val driveDb = googleDriveRepository.loadDbJson().getOrNull()
                if (driveDb != null && driveDb.tables.cars.isNotEmpty()) {
                    Log.i(TAG, "âœ… Drive has existing data ( cars). Restoring Room from Drive...")
                    return@withContext googleDriveRepository.syncRoomFromDrive()
                }
                Log.w(TAG, "âš ï¸ Drive db.json missing/empty too. Proceeding with migration (will create new db.json).")
            }// 2️⃣ Upload all local photos to Drive + UPDATE EXPORT WITH DRIVE METADATA
            Log.d(TAG, "Step 2: Uploading local photos to Drive and updating export...")
            val uploadPhotosResult = googleDriveRepository.uploadAllLocalPhotos(localData)
            if (uploadPhotosResult.isFailure) {
                Log.e(TAG, "❌ Failed to upload photos to Drive: ${uploadPhotosResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    uploadPhotosResult.exceptionOrNull() ?: Exception("Failed to upload photos to Drive")
                )
            }
            val driveReadyData = uploadPhotosResult.getOrThrow()
            Log.i(TAG, "✅ All photos uploaded to Drive successfully")
            Log.i(TAG, "✅ Export updated with Drive URLs/IDs (NO local paths)")
            
            // ✅ VALIDATE: Ensure no local paths in export
            val hasLocalPaths = driveReadyData.tables.photos.any { photo ->
                (photo.localPath.isNotEmpty() && photo.localPath.contains("/data/user/")) ||
                (photo.thumbnailPath?.contains("/data/user/") == true) ||
                (photo.fullSizePath?.contains("/data/user/") == true)
            }
            if (hasLocalPaths) {
                Log.e(TAG, "❌ CRITICAL: Export still contains local paths! This will break Drive primary mode!")
                return@withContext Result.failure(
                    IllegalStateException("Export contains local paths - Drive migration failed")
                )
            }
            
            // 3️⃣ Save db.json to Drive (FINAL VERSION with Drive metadata only)
            Log.d(TAG, "Step 3: Saving db.json to Drive (with Drive URLs/IDs only)...")
            val saveResult = googleDriveRepository.saveDbJson(driveReadyData)
            if (saveResult.isFailure) {
                Log.e(TAG, "❌ Failed to save db.json to Drive: ${saveResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    saveResult.exceptionOrNull() ?: Exception("Failed to save db.json to Drive")
                )
            }
            Log.i(TAG, "✅ db.json saved to Drive successfully (Drive-ready)")
            
            // 4️⃣ Clear Room (devine cache)
            Log.d(TAG, "Step 4: Clearing Room (becomes cache)...")
            val clearResult = googleDriveRepository.clearRoomForUser()
            if (clearResult.isFailure) {
                Log.e(TAG, "❌ Failed to clear Room: ${clearResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    clearResult.exceptionOrNull() ?: Exception("Failed to clear Room")
                )
            }
            Log.i(TAG, "✅ Room cleared")
            
            // 5️⃣ Reload Room from Drive db.json
            Log.d(TAG, "Step 5: Reloading Room from Drive db.json...")
            val syncResult = googleDriveRepository.syncRoomFromDrive()
            if (syncResult.isFailure) {
                Log.e(TAG, "❌ Failed to sync Room from Drive: ${syncResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    syncResult.exceptionOrNull() ?: Exception("Failed to sync Room from Drive")
                )
            }
            Log.i(TAG, "✅ Room reloaded from Drive db.json")
            
            // 6️⃣ Clear ONLY db.json cache (NOT folder cache - folder must persist!)
            Log.d(TAG, "Step 6: Clearing db.json cache (keeping folder cache)...")
            googleDriveRepository.clearDbJsonCache()
            Log.i(TAG, "✅ db.json cache cleared (folder cache preserved)")
            
            Log.i(TAG, "✅ Migration Local → Drive completed successfully!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration Local → Drive failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * REAL migration from Drive to Local.
     * 
     * Flow OBLIGATORIU:
     * 1. Load db.json from Drive
     * 2. Clear Room
     * 3. Save to Room (Room devine truth)
     * 4. Download all photos locally
     */
    private suspend fun migrateDriveToLocal(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== MIGRATING DRIVE → LOCAL (REAL) ===")
            
            // 1️⃣ Load db.json from Drive
            Log.d(TAG, "Step 1: Loading db.json from Drive...")
            val loadResult = googleDriveRepository.loadDbJson()
            if (loadResult.isFailure) {
                Log.e(TAG, "❌ Failed to load db.json from Drive: ${loadResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    loadResult.exceptionOrNull() ?: Exception("Failed to load db.json from Drive")
                )
            }
            val exportData = loadResult.getOrNull()
            if (exportData == null) {
                Log.w(TAG, "⚠️ db.json is empty - nothing to migrate")
                return@withContext Result.success(Unit)
            }
            Log.i(TAG, "✅ Loaded db.json from Drive: ${exportData.tables.cars.size} cars, ${exportData.tables.photos.size} photos")
            
            // 2️⃣ Clear Room
            Log.d(TAG, "Step 2: Clearing Room...")
            val clearResult = googleDriveRepository.clearRoomForUser()
            if (clearResult.isFailure) {
                Log.e(TAG, "❌ Failed to clear Room: ${clearResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    clearResult.exceptionOrNull() ?: Exception("Failed to clear Room")
                )
            }
            Log.i(TAG, "✅ Room cleared")
            
            // 3️⃣ Save to Room (Room devine truth)
            Log.d(TAG, "Step 3: Saving to Room (Room becomes truth)...")
            val saveResult = googleDriveRepository.saveToRoom(exportData)
            if (saveResult.isFailure) {
                Log.e(TAG, "❌ Failed to save to Room: ${saveResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(
                    saveResult.exceptionOrNull() ?: Exception("Failed to save to Room")
                )
            }
            Log.i(TAG, "✅ Data saved to Room (Room is now truth)")
            
            // 4️⃣ Download all photos locally
            Log.d(TAG, "Step 4: Downloading photos locally...")
            val downloadResult = googleDriveRepository.downloadAllPhotosLocally(exportData)
            if (downloadResult.isFailure) {
                Log.w(TAG, "⚠️ Some photos failed to download: ${downloadResult.exceptionOrNull()?.message}")
                // Continue anyway - photos can be downloaded later
            } else {
                Log.i(TAG, "✅ All photos downloaded locally")
            }
            
            Log.i(TAG, "✅ Migration Drive → Local completed successfully!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration Drive → Local failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}




