// CacheManager.kt
package com.example.hotwheelscollectors.offline

import android.content.Context
import androidx.room.withTransaction
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val carDao: CarDao,
    private val firestoreRepository: FirestoreRepository
) {
    private val cacheSize = 100 * 1024 * 1024 // 100MB

    suspend fun prefetchCriticalData() {
        db.withTransaction {
            // Fetch and cache mainline cars
            val mainlineCars = firestoreRepository.getMainlineCars()
            carDao.insertCars(mainlineCars)

            // Fetch and cache premium cars
            val premiumCars = firestoreRepository.getPremiumCars()
            carDao.insertCars(premiumCars)

            // Cache user preferences and settings
            cacheUserPreferences()
        }
    }

    suspend fun clearCache() {
        context.cacheDir.deleteRecursively()
        db.withTransaction {
            carDao.deleteAll()
            // Clear other cached data
        }
    }

    private suspend fun cacheUserPreferences() {
        // Cache user-specific settings and preferences
    }

    fun getCacheSize(): Long {
        return context.cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    fun shouldClearCache(): Boolean {
        return getCacheSize() > cacheSize
    }
}