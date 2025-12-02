# Database Migration Guide

## ⚠️ IMPORTANT: Never Use Destructive Migration in Production!

The `fallbackToDestructiveMigration()` method **WILL DELETE ALL USER DATA** when the database schema changes. This is catastrophic for production apps.

## How to Add New Migrations

### 1. When You Need to Change the Database Schema

**Before making any changes:**
1. **Backup your data** (export to JSON, cloud, etc.)
2. **Test migrations thoroughly** on a copy of production data
3. **Never skip version numbers** (1→2→3, not 1→3)

### 2. Steps to Add a Migration

1. **Increment the version number** in `@Database(version = X)`
2. **Create a new migration** in `DatabaseMigrations.kt`:
   ```kotlin
   val MIGRATION_X_Y = object : Migration(X, Y) {
       override fun migrate(database: SupportSQLiteDatabase) {
           // Your migration code here
       }
   }
   ```
3. **Add it to the migrations array**:
   ```kotlin
   fun getAllMigrations(): Array<Migration> {
       return arrayOf(
           MIGRATION_1_2,
           MIGRATION_2_3,  // Add new migration here
           // ... more migrations
       )
   }
   ```

### 3. Common Migration Examples

#### Adding a New Column
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE cars ADD COLUMN new_field TEXT")
    }
}
```

#### Adding a New Table
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS new_table (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
    }
}
```

#### Renaming a Column
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite doesn't support ALTER COLUMN, so we need to:
        // 1. Create new table with new column name
        // 2. Copy data from old table
        // 3. Drop old table
        // 4. Rename new table
        
        database.execSQL("""
            CREATE TABLE cars_new (
                id TEXT PRIMARY KEY NOT NULL,
                new_column_name TEXT,  -- renamed from old_column_name
                -- ... other columns
            )
        """)
        
        database.execSQL("""
            INSERT INTO cars_new (id, new_column_name, ...)
            SELECT id, old_column_name, ... FROM cars
        """)
        
        database.execSQL("DROP TABLE cars")
        database.execSQL("ALTER TABLE cars_new RENAME TO cars")
    }
}
```

### 4. Testing Migrations

**Always test migrations before releasing:**

1. **Create test data** that matches your current schema
2. **Run the migration** on test data
3. **Verify data integrity** after migration
4. **Test rollback scenarios** if possible

### 5. Emergency Procedures

If a migration fails in production:

1. **Don't panic** - users' data is still safe
2. **Fix the migration** code
3. **Test thoroughly** on a copy of production data
4. **Release a hotfix** with the corrected migration

### 6. Best Practices

- ✅ **Always backup before schema changes**
- ✅ **Test migrations on production-like data**
- ✅ **Use transactions for complex migrations**
- ✅ **Add logging to track migration progress**
- ✅ **Keep migrations simple and atomic**
- ❌ **Never use fallbackToDestructiveMigration() in production**
- ❌ **Never skip version numbers**
- ❌ **Never assume data will be in a specific state**

## Current Migration Status

- **Version 1→2**: No-op migration (starting fresh)
- **Future migrations**: Add them here as you create them

## Schema Export

With `exportSchema = true`, Room will generate schema files in:
`app/schemas/com.example.hotwheelscollectors.data.local.AppDatabase/`

These files help you understand the database structure and create proper migrations.
