package com.example.hotwheelscollectors.docs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class DeveloperGuide private constructor(private val context: Context) {

    private val devGuideDir: File = File(context.filesDir, "developer_guide")
    private val cache = mutableMapOf<String, GuideEntry>()

    init {
        createDevGuideDirectory()
    }

    private fun createDevGuideDirectory() {
        if (!devGuideDir.exists()) {
            devGuideDir.mkdirs()
        }
    }

    suspend fun updateEntry(
        entryId: String,
        title: String,
        content: String,
        category: String,
        subcategory: String? = null,
        codeExamples: List<CodeExample>? = null,
        dependencies: List<String>? = null,
        tags: List<String>? = null
    ) = withContext(Dispatchers.IO) {
        val entryData = JSONObject().apply {
            put("entry_id", entryId)
            put("title", title)
            put("content", content)
            put("category", category)
            put("subcategory", subcategory ?: JSONObject.NULL)
            put("code_examples", codeExamples?.map { it.toJson() } ?: JSONObject.NULL)
            put("dependencies", dependencies ?: JSONObject.NULL)
            put("tags", tags ?: JSONObject.NULL)
            put("last_updated", System.currentTimeMillis())
        }

        val entryFile = File(devGuideDir, "$entryId.json")
        entryFile.writeText(entryData.toString(2))
        cache[entryId] = parseGuideEntry(entryData)
    }

    suspend fun getEntry(entryId: String): GuideEntry? = withContext(Dispatchers.IO) {
        cache[entryId]?.let { return@withContext it }

        val entryFile = File(devGuideDir, "$entryId.json")
        if (entryFile.exists()) {
            val entryData = JSONObject(entryFile.readText())
            val entry = parseGuideEntry(entryData)
            cache[entryId] = entry
            return@withContext entry
        }

        null
    }

    suspend fun listEntriesByCategory(
        category: String,
        subcategory: String? = null
    ): List<GuideEntry> = withContext(Dispatchers.IO) {
        devGuideDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val entryData = JSONObject(file.readText())
                    val entry = parseGuideEntry(entryData)
                    if (entry.category == category &&
                        (subcategory == null || entry.subcategory == subcategory)
                    ) {
                        entry
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedBy { it.title }
            ?: emptyList()
    }

    suspend fun searchGuide(
        query: String,
        category: String? = null,
        tags: List<String>? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val queryLower = query.lowercase()
        
        devGuideDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val entryData = JSONObject(file.readText())
                    val entry = parseGuideEntry(entryData)
                    
                    if (category != null && entry.category != category) {
                        return@mapNotNull null
                    }
                    
                    if (tags != null && tags.isNotEmpty() &&
                        (entry.tags == null || !entry.tags.any { it in tags })
                    ) {
                        return@mapNotNull null
                    }

                    val titleMatch = entry.title.lowercase().contains(queryLower)
                    val contentMatch = entry.content.lowercase().contains(queryLower)
                    val codeMatch = entry.codeExamples?.any {
                        it.code.lowercase().contains(queryLower)
                    } ?: false

                    when {
                        titleMatch -> SearchResult(entry, 3)
                        codeMatch -> SearchResult(entry, 2)
                        contentMatch -> SearchResult(entry, 1)
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.relevance }
            ?: emptyList()
    }

    suspend fun getCategories(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val categories = mutableMapOf<String, MutableSet<String>>()
        
        devGuideDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.forEach { file ->
                try {
                    val entryData = JSONObject(file.readText())
                    val category = entryData.getString("category")
                    val subcategory = entryData.optString("subcategory", null)
                    
                    categories.getOrPut(category) { mutableSetOf() }.apply {
                        if (subcategory != null) {
                            add(subcategory)
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid entries
                }
            }

        categories.mapValues { it.value.toList().sorted() }
    }

    private fun parseGuideEntry(json: JSONObject): GuideEntry {
        return GuideEntry(
            entryId = json.getString("entry_id"),
            title = json.getString("title"),
            content = json.getString("content"),
            category = json.getString("category"),
            subcategory = json.optString("subcategory", null).takeUnless { it.isEmpty() },
            codeExamples = json.optJSONArray("code_examples")?.let { array ->
                List(array.length()) { i ->
                    val exampleJson = array.getJSONObject(i)
                    CodeExample(
                        title = exampleJson.getString("title"),
                        code = exampleJson.getString("code"),
                        description = exampleJson.optString("description", null)
                    )
                }
            },
            dependencies = json.optJSONArray("dependencies")?.let { array ->
                List(array.length()) { i -> array.getString(i) }
            },
            tags = json.optJSONArray("tags")?.let { array ->
                List(array.length()) { i -> array.getString(i) }
            },
            lastUpdated = json.getLong("last_updated")
        )
    }

    data class GuideEntry(
        val entryId: String,
        val title: String,
        val content: String,
        val category: String,
        val subcategory: String? = null,
        val codeExamples: List<CodeExample>? = null,
        val dependencies: List<String>? = null,
        val tags: List<String>? = null,
        val lastUpdated: Long
    )

    data class CodeExample(
        val title: String,
        val code: String,
        val description: String? = null
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("title", title)
            put("code", code)
            description?.let { put("description", it) }
        }
    }

    data class SearchResult(
        val entry: GuideEntry,
        val relevance: Int
    )

    companion object {
        @Volatile
        private var INSTANCE: DeveloperGuide? = null

        fun getInstance(context: Context): DeveloperGuide {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeveloperGuide(context).also { INSTANCE = it }
            }
        }
    }
}