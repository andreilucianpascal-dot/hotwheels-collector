package com.example.hotwheelscollectors.data.management

import android.content.Context
import androidx.room.withTransaction
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val storage: FirebaseStorage,
    private val gson: Gson
) {
    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState = _restoreState.asStateFlow()

    suspend fun restoreBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _restoreState.value = RestoreState.Downloading

            // Download backup
            val zipFile = downloadBackup(backupId)

            _restoreState.value = RestoreState.Extracting

            // Extract backup
            val backupDir = extractBackup(zipFile)

            // Read metadata
            val metadata = gson.fromJson(
                File(backupDir, "metadata.json").readText(),
                BackupManager.BackupMetadata::class.java
            )

            _restoreState.value = RestoreState.Restoring

            // Restore database
            restoreDatabase(File(backupDir, metadata.databaseFile))

            // Restore photos if included
            if (metadata.includesPhotos && metadata.photoDirectory != null) {
                restorePhotos(File(backupDir, metadata.photoDirectory))
            }

            // Cleanup
            backupDir.deleteRecursively()
            zipFile.delete()

            _restoreState.value = RestoreState.Completed
            Result.success(Unit)
        } catch (e: Exception) {
            _restoreState.value = RestoreState.Error(e.message ?: "Restore failed")
            Result.failure(e)
        }
    }

    private suspend fun downloadBackup(backupId: String): File {
        return withContext(Dispatchers.IO) {
            val zipFile = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
            val ref = storage.reference
                .child("backups")
                .child(backupId)
                .child("backup.zip")

            ref.getFile(zipFile).await()
            zipFile
        }
    }

    private suspend fun extractBackup(zipFile: File): File {
        return withContext(Dispatchers.IO) {
            val extractDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}")
            extractDir.mkdirs()

            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(extractDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            extractDir
        }
    }

    private suspend fun restoreDatabase(dbFile: File) {
        withContext(Dispatchers.IO) {
            val backup = gson.fromJson(
                dbFile.readText(),
                BackupManager.DatabaseBackup::class.java
            )

            db.withTransaction {
                // Clear existing data
                carDao.deleteAll()
                photoDao.deleteAll()

                // Restore backup data
                backup.cars.forEach { car ->
                    carDao.insertCar(car)
                }
                backup.photos.forEach { photo ->
                    photoDao.insertPhoto(photo)
                }
            }
        }
    }

    private suspend fun restorePhotos(photosDir: File) {
        withContext(Dispatchers.IO) {
            val appPhotosDir = File(context.filesDir, "photos")
            appPhotosDir.mkdirs()

            photosDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val targetFile = File(appPhotosDir, file.name)
                    file.copyTo(targetFile, overwrite = true)
                }
            }
        }
    }

    sealed class RestoreState {
        object Idle : RestoreState()
        object Downloading : RestoreState()
        object Extracting : RestoreState()
        object Restoring : RestoreState()
        object Completed : RestoreState()
        data class Error(val message: String) : RestoreState()
    }
}