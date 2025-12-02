package com.example.hotwheelscollectors.data.local.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["userId", "query"]),
        Index(value = ["timestamp"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val query: String,
    val filters: Map<String, String> = emptyMap(),
    val resultCount: Int = 0,
    val timestamp: Date = Date()
)