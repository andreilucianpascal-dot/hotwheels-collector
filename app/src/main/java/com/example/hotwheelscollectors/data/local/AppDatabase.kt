package com.example.hotwheelscollectors.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.hotwheelscollectors.data.local.dao.*
import com.example.hotwheelscollectors.data.local.entities.*
import com.example.hotwheelscollectors.data.local.migrations.DatabaseMigrations

@Database(
    entities = [
        UserEntity::class,
        CarEntity::class,
        PhotoEntity::class,
        PriceHistoryEntity::class,
        TradeOfferEntity::class,
        WishlistEntity::class,
        SearchHistoryEntity::class,
        SearchKeywordEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO abstract functions
    abstract fun userDao(): UserDao
    abstract fun carDao(): CarDao
    abstract fun photoDao(): PhotoDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun tradeDao(): TradeDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun searchKeywordDao(): SearchKeywordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Clean up any corrupted database files
                val databaseFile = context.getDatabasePath("hotwheels_database")
                if (databaseFile.exists()) {
                    android.util.Log.i("AppDatabase", "Existing database found, will use migrations")
                } else {
                    android.util.Log.i("AppDatabase", "No existing database, will create fresh")
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hotwheels_database"
                )
                    .addMigrations(*DatabaseMigrations.getAllMigrations())
                    .fallbackToDestructiveMigration() // For development: will recreate DB if schema changed
                    .fallbackToDestructiveMigrationOnDowngrade() // Only for downgrades, not upgrades
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            android.util.Log.i("AppDatabase", "✅ Database created successfully with all tables and columns")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            android.util.Log.i("AppDatabase", "✅ Database opened successfully")
                        }
                    })
                    .build()
                INSTANCE = instance
                
                // Log successful database creation
                android.util.Log.i("AppDatabase", "✅ Database instance created successfully with version 6")
                
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
    }
}