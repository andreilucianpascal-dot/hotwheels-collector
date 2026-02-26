package com.example.hotwheelscollectors.data.repository

import android.content.Context
import android.util.Log
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveRootFolderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "DriveRootFolderManager"
        private const val ROOT_FOLDER_NAME = "HotWheelsCollectors"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"

        private const val PREFS_NAME = "drive_root_folder_prefs"
        private const val KEY_ROOT_FOLDER_ID = "root_folder_id"
    }

    private val lock = Any()
    private var cachedRootFolderId: String? = null

    private fun getStoredFolderId(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROOT_FOLDER_ID, null)

    private fun storeFolderId(id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROOT_FOLDER_ID, id)
            .apply()
    }

    fun clearCache() {
        synchronized(lock) {
            cachedRootFolderId = null
            Log.d(TAG, "âœ… Root folder cache cleared")
        }
    }

    fun getOrCreateRootFolderId(driveService: Drive): String {
        synchronized(lock) {
            cachedRootFolderId?.let { cached ->
                Log.d(TAG, "âœ… Using cached root folder ID: $cached")
                return cached
            }

            // 1) Prefer persisted ID (survives app restart) to avoid re-creating folder each launch.
            val storedId = getStoredFolderId()
            if (!storedId.isNullOrEmpty()) {
                try {
                    val meta = driveService.files().get(storedId)
                        .setFields("id,trashed")
                        .execute()
                    val ok = meta != null && (meta.trashed == null || meta.trashed == false)
                    if (ok) {
                        cachedRootFolderId = storedId
                        Log.i(TAG, "âœ… Using stored root folder ID: $storedId")
                        return storedId
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "âš ï¸ Stored root folder ID invalid/unavailable: ${e.message}")
                }
            }

            // 2) Search in root
            val query = "name = '$ROOT_FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false and 'root' in parents"
            val request = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name,parents)")
                .setPageSize(10)

            val fileList: FileList = request.execute()
            val files = fileList.files

            if (!files.isNullOrEmpty()) {
                // DacÄƒ sunt multiple foldere (duplicate vechi), alege folderul care conÈ›ine db.json.
                var chosenId: String? = null
                if (files.size > 1) {
                    for (f in files) {
                        try {
                            val q = "name = 'db.json' and '${f.id}' in parents and trashed = false"
                            val hasDb = driveService.files().list()
                                .setQ(q)
                                .setSpaces("drive")
                                .setFields("files(id)")
                                .setPageSize(1)
                                .execute()
                                .files
                                ?.isNotEmpty() == true
                            if (hasDb) {
                                chosenId = f.id
                                break
                            }
                        } catch (_: Exception) {
                            // ignore and continue
                        }
                    }
                }

                val folderId = chosenId ?: files[0].id
                cachedRootFolderId = folderId
                storeFolderId(folderId)

                if (files.size > 1) {
                    Log.w(TAG, "âš ï¸ Found ${files.size} '$ROOT_FOLDER_NAME' folders in root. Using ID: $folderId")
                } else {
                    Log.i(TAG, "âœ… Root folder '$ROOT_FOLDER_NAME' found: $folderId")
                }

                return folderId
            }
// 3) Create in root
            Log.i(TAG, "Creating root folder '$ROOT_FOLDER_NAME' in Drive...")
            val metadata = File().apply {
                name = ROOT_FOLDER_NAME
                mimeType = FOLDER_MIME
                parents = Collections.singletonList("root")
            }

            val created = driveService.files().create(metadata)
                .setFields("id,parents")
                .execute()

            val folderId = created.id
            cachedRootFolderId = folderId
            storeFolderId(folderId)
            Log.i(TAG, "âœ… Root folder '$ROOT_FOLDER_NAME' created: $folderId (parents: ${created.parents})")
            return folderId
        }
    }
}

