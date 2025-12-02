package com.example.hotwheelscollectors.data.management

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataMigration @Inject constructor() {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add estimated_value column to cars table
            database.execSQL(
                "ALTER TABLE cars ADD COLUMN estimated_value REAL"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add version column to all tables
            database.execSQL("ALTER TABLE cars ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE photos ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

            // Add sync_status column
            database.execSQL(
                "ALTER TABLE cars ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'"
            )
            database.execSQL(
                "ALTER TABLE photos ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create backup_metadata table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS backup_metadata (
                    id TEXT PRIMARY KEY NOT NULL,
                    timestamp INTEGER NOT NULL,
                    size INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    cloud_path TEXT
                )
            """)
        }
    }

    fun getMigrations() = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4
    )
}