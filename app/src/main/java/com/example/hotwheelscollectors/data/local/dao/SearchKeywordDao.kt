package com.example.hotwheelscollectors.data.local.dao

import androidx.room.*
import com.example.hotwheelscollectors.data.local.entities.SearchKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchKeywordDao {

    // Basic CRUD operations
    @Query("SELECT * FROM search_keywords WHERE carId = :carId")
    fun getKeywordsForCar(carId: String): Flow<List<SearchKeywordEntity>>

    @Query("SELECT keyword FROM search_keywords WHERE carId = :carId")
    suspend fun getKeywordsForCarSync(carId: String): List<String>

    @Query("SELECT * FROM search_keywords WHERE keyword LIKE '%' || :query || '%'")
    fun searchKeywords(query: String): Flow<List<SearchKeywordEntity>>

    @Query("SELECT * FROM search_keywords")
    fun getAllKeywords(): Flow<List<SearchKeywordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: SearchKeywordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(keywords: List<SearchKeywordEntity>)

    @Update
    suspend fun updateKeyword(keyword: SearchKeywordEntity)

    @Delete
    suspend fun deleteKeyword(keyword: SearchKeywordEntity)

    @Query("DELETE FROM search_keywords WHERE carId = :carId")
    suspend fun deleteKeywordsForCar(carId: String)

    @Query("DELETE FROM search_keywords")
    suspend fun deleteAllKeywords()

    // Statistics and counting
    @Query("SELECT COUNT(*) FROM search_keywords")
    suspend fun getKeywordCount(): Int

    @Query("SELECT COUNT(*) FROM search_keywords WHERE carId = :carId")
    suspend fun getKeywordCountForCar(carId: String): Int

    // User-specific queries
    @Query("""
        SELECT sk.* FROM search_keywords sk
        INNER JOIN cars c ON sk.carId = c.id
        WHERE c.userId = :userId
    """)
    fun getKeywordsForUser(userId: String): Flow<List<SearchKeywordEntity>>

    // Advanced queries
    @Query("""
        SELECT DISTINCT keyword FROM search_keywords
        ORDER BY keyword ASC
    """)
    fun getAllUniqueKeywords(): Flow<List<String>>

    @Query("""
        SELECT keyword, COUNT(*) as usageCount 
        FROM search_keywords 
        GROUP BY keyword 
        ORDER BY usageCount DESC
    """)
    fun getKeywordUsageStats(): Flow<List<KeywordUsageStats>>

    @Query("""
        SELECT sk.* FROM search_keywords sk
        INNER JOIN cars c ON sk.carId = c.id
        WHERE c.userId = :userId AND sk.keyword LIKE '%' || :query || '%'
    """)
    fun searchKeywordsForUser(userId: String, query: String): Flow<List<SearchKeywordEntity>>

    @Query("""
        SELECT sk.keyword, COUNT(*) as carCount
        FROM search_keywords sk
        INNER JOIN cars c ON sk.carId = c.id
        WHERE c.userId = :userId
        GROUP BY sk.keyword
        ORDER BY carCount DESC
    """)
    fun getKeywordCarCountForUser(userId: String): Flow<List<KeywordCarCount>>
}

data class KeywordUsageStats(
    val keyword: String,
    val usageCount: Int
)

data class KeywordCarCount(
    val keyword: String,
    val carCount: Int
)