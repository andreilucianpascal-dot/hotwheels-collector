package com.example.hotwheelscollectors.utils

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DatabaseCleanup(
    private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao
) {

    fun optimizeDatabase() {
        try {
            // Database optimization is handled automatically by Room
            // We can add custom cleanup logic here if needed in the future
            Timber.d("Database optimization completed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to optimize database")
        }
    }

    suspend fun performCleanup() {
        withContext(Dispatchers.IO) {
            try {
                // Clean up old temporary files
                cleanTempFiles()
                
                // Vacuum database (not needed with WAL mode in Room)
                Timber.d("Database cleanup completed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to perform cleanup")
            }
        }
    }

    suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                val totalCars = carDao.getCarCount()
                val allCars = carDao.getAllCars().first()
                val allPhotos = photoDao.getAllPhotos().first()
                
                val mainlineCars = allCars.count { !it.isPremium && !it.isSTH && !it.isTH }
                val premiumCars = allCars.count { it.isPremium }
                val othersCars = allCars.count { !it.isPremium && (it.isSTH || it.isTH) }
                
                val duplicateCars = calculateDuplicateCars(allCars)
                val genericBrandCars = allCars.count { it.brand.isEmpty() || it.brand == "Unknown" }
                val orphanedPhotos = calculateOrphanedPhotos(allPhotos, allCars)
                
                DatabaseStats(
                    totalCars = totalCars,
                    totalPhotos = allPhotos.size,
                    totalKeywords = allCars.sumOf { it.searchKeywords.size },
                    mainlineCars = mainlineCars,
                    othersCars = othersCars,
                    premiumCars = premiumCars,
                    duplicateCars = duplicateCars,
                    genericBrandCars = genericBrandCars,
                    orphanedPhotos = orphanedPhotos,
                    databaseSize = getDatabaseSize(),
                    lastCleanupDate = getLastCleanupDate()
                )
        } catch (e: Exception) {
                Timber.e(e, "Failed to get database stats")
                DatabaseStats()
            }
        }
    }

    suspend fun removeGenericBrandCars(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                val allCars = carDao.getAllCars().first()
                val genericBrandCars = allCars.filter { it.brand.isEmpty() || it.brand == "Unknown" }
                
                genericBrandCars.forEach { car ->
                    carDao.deleteCar(car)
                }
                
                Timber.d("Removed ${genericBrandCars.size} generic brand cars")
                getDatabaseStats()
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove generic brand cars")
                DatabaseStats()
            }
        }
    }

    suspend fun removeDuplicateCars(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                val allCars = carDao.getAllCars().first()
                val groupedCars = allCars.groupBy { "${it.brand}_${it.model}_${it.year}" }
                
                var removedCount = 0
                groupedCars.values.forEach { group ->
                    if (group.size > 1) {
                        // Keep the first car, remove the rest
                        val carsToRemove = group.drop(1)
                        carsToRemove.forEach { car ->
                            carDao.deleteCar(car)
                            removedCount++
                        }
                    }
                }
                
                Timber.d("Removed $removedCount duplicate cars")
                getDatabaseStats()
        } catch (e: Exception) {
                Timber.e(e, "Failed to remove duplicate cars")
                DatabaseStats()
            }
        }
    }

    suspend fun clearUserData(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                val allCars = carDao.getAllCars().first()
                val allPhotos = photoDao.getAllPhotos().first()
                
                // Clear all cars and photos
                allCars.forEach { car ->
                    carDao.deleteCar(car)
                }
                allPhotos.forEach { photo ->
                    photoDao.hardDeletePhoto(photo.id)
                }
                
                Timber.d("Cleared ${allCars.size} cars and ${allPhotos.size} photos")
                getDatabaseStats()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear user data")
                DatabaseStats()
            }
        }
    }

    suspend fun clearAllData(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            try {
                // Clear all data from all tables
                carDao.deleteAll()
                photoDao.deleteAll()
                
                Timber.d("All data cleared from database")
                getDatabaseStats()
        } catch (e: Exception) {
                Timber.e(e, "Failed to clear all data")
                DatabaseStats()
            }
        }
    }

    private fun cleanTempFiles() {
        try {
            val tempDir = context.cacheDir
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clean temp files")
        }
    }

    private fun getDatabaseSize(): Long {
        return try {
            val dbFile = context.getDatabasePath("app_database")
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (e: Exception) {
            Timber.e(e, "Failed to get database size")
            0L
        }
    }

    private fun getLastCleanupDate(): String {
        return try {
            val prefs = context.getSharedPreferences("database_cleanup", Context.MODE_PRIVATE)
            prefs.getString("last_cleanup_date", "") ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Failed to get last cleanup date")
            ""
        }
    }

    private fun calculateDuplicateCars(cars: List<CarEntity>): Int {
        val groupedCars = cars.groupBy { "${it.brand}_${it.model}_${it.year}" }
        return groupedCars.values.sumOf { group -> maxOf(0, group.size - 1) }
    }

    private fun calculateOrphanedPhotos(photos: List<PhotoEntity>, cars: List<CarEntity>): Int {
        val carIds = cars.map { it.id }.toSet()
        return photos.count { photo -> photo.carId !in carIds }
    }
}
