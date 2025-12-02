package com.example.hotwheelscollectors.data.management

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val storage: FirebaseStorage,
    private val gson: Gson
) {
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState = _backupState.asStateFlow()

    suspend fun createBackup(includePhotos: Boolean = true): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                _backupState.value = BackupState.Creating

                // Create backup directory
                val backupDir = File(context.cacheDir, "backup_${System.currentTimeMillis()}")
                backupDir.mkdirs()

                // Backup database
                val dbBackup = backupDatabase(backupDir)

                // Backup photos if requested
                val photoBackup = if (includePhotos) {
                    backupPhotos(backupDir)
                } else null

                // Create metadata
                val metadata = BackupMetadata(
                    timestamp = System.currentTimeMillis(),
                    databaseFile = dbBackup.name,
                    photoDirectory = photoBackup?.name,
                    includesPhotos = includePhotos
                )

                // Save metadata
                File(backupDir, "metadata.json").writeText(gson.toJson(metadata))

                // Create zip file
                val zipFile = createZipFile(backupDir)

                // Upload to cloud storage
                val backupId = uploadBackup(zipFile)

                // Cleanup
                backupDir.deleteRecursively()
                zipFile.delete()

                _backupState.value = BackupState.Completed(backupId)
                Result.success(backupId)
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Backup failed")
                Result.failure(e)
            }
        }

    private suspend fun backupDatabase(backupDir: File): File {
        return withContext(Dispatchers.IO) {
            val dbFile = File(backupDir, "database.json")
            db.withTransaction {
                val cars = carDao.getAllCars().first()
                val photos = photoDao.getAllPhotos().first()
                val backup = DatabaseBackup(cars = cars, photos = photos)
                dbFile.writeText(gson.toJson(backup))
            }
            dbFile
        }
    }

    private suspend fun backupPhotos(backupDir: File): File {
        return withContext(Dispatchers.IO) {
            val photosDir = File(backupDir, "photos")
            photosDir.mkdirs()

            photoDao.getAllPhotos().first().forEach { photo ->
                val sourceFile = File(photo.localPath)
                if (sourceFile.exists()) {
                    val targetFile = File(photosDir, sourceFile.name)
                    sourceFile.copyTo(targetFile)
                }
            }
            photosDir
        }
    }

    private suspend fun createZipFile(backupDir: File): File {
        return withContext(Dispatchers.IO) {
            val zipFile = File(context.cacheDir, "backup_${System.currentTimeMillis()}.zip")
            ZipOutputStream(zipFile.outputStream()).use { zip ->
                backupDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry(file.relativeTo(backupDir).path)
                        zip.putNextEntry(entry)
                        file.inputStream().copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }
            zipFile
        }
    }

    private suspend fun uploadBackup(zipFile: File): String {
        return withContext(Dispatchers.IO) {
            val backupId = UUID.randomUUID().toString()
            val ref = storage.reference
                .child("backups")
                .child(backupId)
                .child("backup.zip")

            ref.putFile(zipFile.toUri()).await()
            backupId
        }
    }

    sealed class BackupState {
        object Idle : BackupState()
        object Creating : BackupState()
        data class Completed(val backupId: String) : BackupState()
        data class Error(val message: String) : BackupState()
    }

    data class BackupMetadata(
        val timestamp: Long,
        val databaseFile: String,
        val photoDirectory: String?,
        val includesPhotos: Boolean
    )

    data class DatabaseBackup(
        val cars: List<CarEntity>,
        val photos: List<PhotoEntity>
    )
}