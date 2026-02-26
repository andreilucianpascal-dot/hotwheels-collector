package com.example.hotwheelscollectors.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for Hot Wheels Collectors app
 * 
 * IMPORTANT: Never use fallbackToDestructiveMigration() in production!
 * Always implement proper migrations to preserve user data.
 */

object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2
     * This migration adds new columns to photos table for global database system
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 1 to 2 - Adding global database columns to photos table")
            
            try {
                // Add new columns to photos table for global database system
                // Use IF NOT EXISTS to prevent errors if columns already exist
                database.execSQL("ALTER TABLE photos ADD COLUMN barcode TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added barcode column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN isGlobal INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added isGlobal column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN photoType TEXT NOT NULL DEFAULT 'OTHER'")
                android.util.Log.d("DatabaseMigrations", "✅ Added photoType column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN collectionFolder TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added collectionFolder column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN contributorUserId TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added contributorUserId column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN verificationCount INTEGER NOT NULL DEFAULT 1")
                android.util.Log.d("DatabaseMigrations", "✅ Added verificationCount column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN isVerified INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added isVerified column")
                
                android.util.Log.i("DatabaseMigrations", "🎉 Successfully migrated from version 1 to 2 - All global database columns added")
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Migration from version 2 to 3
     * This migration adds optimized photo system columns to photos table
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 2 to 3 - Adding optimized photo system columns")
            
            try {
                // Add optimized photo system columns to photos table
                database.execSQL("ALTER TABLE photos ADD COLUMN fullSizePath TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullSizePath column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN thumbnailWidth INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailWidth column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN thumbnailHeight INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailHeight column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN fullSizeWidth INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullSizeWidth column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN fullSizeHeight INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullSizeHeight column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN thumbnailSizeKB INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailSizeKB column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN fullSizeSizeKB INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullSizeSizeKB column")
                
                database.execSQL("ALTER TABLE photos ADD COLUMN isTemporary INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added isTemporary column")
                
                android.util.Log.i("DatabaseMigrations", "🎉 Successfully migrated from version 2 to 3 - All optimized photo system columns added")
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Migration from version 3 to 4
     * This migration adds incremental sync status columns to cars table
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 3 to 4 - Adding incremental sync status columns")
            
            try {
                // Overall sync status
                database.execSQL("ALTER TABLE cars ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added syncAttempts column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN lastSyncError TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added lastSyncError column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN lastSyncAttempt INTEGER")
                android.util.Log.d("DatabaseMigrations", "✅ Added lastSyncAttempt column")
                
                // Thumbnail sync status
                database.execSQL("ALTER TABLE cars ADD COLUMN thumbnailSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailSyncStatus column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN thumbnailFirebaseUrl TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailFirebaseUrl column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN thumbnailSyncAttempts INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added thumbnailSyncAttempts column")
                
                // Full photo sync status
                database.execSQL("ALTER TABLE cars ADD COLUMN fullPhotoSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullPhotoSyncStatus column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN fullPhotoFirebaseUrl TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullPhotoFirebaseUrl column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN fullPhotoSyncAttempts INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added fullPhotoSyncAttempts column")
                
                // Barcode sync status
                database.execSQL("ALTER TABLE cars ADD COLUMN barcodeSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                android.util.Log.d("DatabaseMigrations", "✅ Added barcodeSyncStatus column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN barcodeFirebaseUrl TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added barcodeFirebaseUrl column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN barcodeSyncAttempts INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added barcodeSyncAttempts column")
                
                // Firestore data sync status
                database.execSQL("ALTER TABLE cars ADD COLUMN firestoreDataSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                android.util.Log.d("DatabaseMigrations", "✅ Added firestoreDataSyncStatus column")
                
                database.execSQL("ALTER TABLE cars ADD COLUMN firestoreDataSyncAttempts INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added firestoreDataSyncAttempts column")
                
                // Sync priority
                database.execSQL("ALTER TABLE cars ADD COLUMN syncPriority INTEGER NOT NULL DEFAULT 100")
                android.util.Log.d("DatabaseMigrations", "✅ Added syncPriority column")
                
                // Created at timestamp
                database.execSQL("ALTER TABLE cars ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                android.util.Log.d("DatabaseMigrations", "✅ Added createdAt column")
                
                android.util.Log.i("DatabaseMigrations", "🎉 Successfully migrated from version 3 to 4 - All incremental sync status columns added")
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Migration from version 4 to 5
     * This migration removes BackupDao and BackupMetadataEntity tables (if they exist)
     * Note: This is a no-op migration since we're just cleaning up unused tables
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 4 to 5 - Cleaning up unused backup tables")
            
            try {
                // Drop backup tables if they exist (they were removed from entities)
                // SQLite doesn't support IF EXISTS for DROP TABLE, so we'll use a try-catch
                try {
                    database.execSQL("DROP TABLE IF EXISTS backup_metadata")
                    android.util.Log.d("DatabaseMigrations", "✅ Dropped backup_metadata table (if existed)")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigrations", "backup_metadata table didn't exist, skipping")
                }
                
                android.util.Log.i("DatabaseMigrations", "🎉 Successfully migrated from version 4 to 5 - Cleaned up unused backup tables")
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                // Don't throw - this is a cleanup migration, not critical
            }
        }
    }
    
    /**
     * Migration from version 5 to 6
     * This migration adds originalBrowsePhotoUrl column to cars table
     * Used to prevent duplicate cars from Browse (each Firebase photo has unique URL)
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 5 to 6 - Adding originalBrowsePhotoUrl column")
            
            try {
                // Add originalBrowsePhotoUrl column to cars table
                // This stores the Firebase Storage URL of the photo from Browse
                // Used to prevent adding the same car from Browse multiple times
                database.execSQL("ALTER TABLE cars ADD COLUMN originalBrowsePhotoUrl TEXT")
                android.util.Log.d("DatabaseMigrations", "✅ Added originalBrowsePhotoUrl column")
                
                android.util.Log.i("DatabaseMigrations", "🎉 Successfully migrated from version 5 to 6 - Added originalBrowsePhotoUrl column")
                
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Migration from version 6 to 7
     * Adds purchaseCurrency column to cars table (ISO 4217 code like RON/EUR/GBP).
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            android.util.Log.d("DatabaseMigrations", "🔄 Migrating from version 6 to 7 - Adding purchaseCurrency column")
            try {
                database.execSQL("ALTER TABLE cars ADD COLUMN purchaseCurrency TEXT NOT NULL DEFAULT ''")
                android.util.Log.d("DatabaseMigrations", "✅ Added purchaseCurrency column")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigrations", "❌ Migration failed: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Get all migrations for the database
     * Add new migrations to this list as you create them
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
            // Add future migrations here: MIGRATION_6_7, etc.
        )
    }
}
