package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SearchHistoryDao {
    @Query("""
        SELECT * FROM search_history 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentSearches(userId: String, limit: Int = 10): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearSearchHistory(userId: String)

    @Query("""
        DELETE FROM search_history 
        WHERE userId = :userId 
        AND timestamp < :cutoffDate
    """)
    suspend fun deleteOldSearches(
        userId: String,
        cutoffDate: Date = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    )

    @Query("""
        SELECT `query`, COUNT(*) as count 
        FROM search_history 
        WHERE userId = :userId 
        GROUP BY `query` 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getPopularSearches(userId: String, limit: Int = 5): Flow<List<PopularSearch>>

    @Query("""
        SELECT * FROM search_history 
        WHERE userId = :userId 
        AND `query` LIKE '%' || :prefix || '%' 
        GROUP BY `query` 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getSuggestedSearches(
        userId: String,
        prefix: String,
        limit: Int = 5
    ): Flow<List<SearchHistoryEntity>>
}

data class PopularSearch(
    val query: String,
    val count: Int
)