package com.example.hotwheelscollectors.performance

import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseOptimizer private constructor() {

    private var isOptimizing = false

    suspend fun optimizeDatabase(database: RoomDatabase) {
        if (isOptimizing) return

        withContext(Dispatchers.IO) {
            try {
                isOptimizing = true
                database.query("PRAGMA optimize", null)
                database.query("VACUUM", null)
                database.query("ANALYZE", null)
            } finally {
                isOptimizing = false
            }
        }
    }

    fun createOptimalIndexes(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_car_name ON hot_wheels_cars(name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_car_year ON hot_wheels_cars(year)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_car_series ON hot_wheels_cars(series)")
    }

    fun configureDatabaseSettings(database: SQLiteDatabase) {
        database.execSQL("PRAGMA journal_mode=WAL")
        database.execSQL("PRAGMA synchronous=NORMAL")
        database.execSQL("PRAGMA temp_store=MEMORY")
        database.execSQL("PRAGMA cache_size=10000")
    }

    suspend fun getPerformanceMetrics(database: RoomDatabase): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val metrics = mutableMapOf<String, Any>()
            
            database.query("PRAGMA page_count", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    metrics["page_count"] = cursor.getLong(0)
                }
            }

            database.query("PRAGMA page_size", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    metrics["page_size"] = cursor.getLong(0)
                }
            }

            database.query("PRAGMA cache_size", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    metrics["cache_size"] = cursor.getLong(0)
                }
            }

            metrics
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DatabaseOptimizer? = null

        fun getInstance(): DatabaseOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseOptimizer().also { INSTANCE = it }
            }
        }
    }
}