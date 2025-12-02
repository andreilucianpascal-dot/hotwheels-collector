package com.example.hotwheelscollectors.docs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class UserGuide private constructor(private val context: Context) {

    private val guideDir: File = File(context.filesDir, "user_guide")
    private val cache = mutableMapOf<String, GuideSection>()

    init {
        createGuideDirectory()
    }

    private fun createGuideDirectory() {
        if (!guideDir.exists()) {
            guideDir.mkdirs()
        }
    }

    suspend fun updateSection(
        sectionId: String,
        title: String,
        content: String,
        parentId: String? = null,
        order: Int = 0,
        tags: List<String>? = null
    ) = withContext(Dispatchers.IO) {
        val sectionData = JSONObject().apply {
            put("section_id", sectionId)
            put("title", title)
            put("content", content)
            put("parent_id", parentId ?: JSONObject.NULL)
            put("order", order)
            put("tags", tags ?: JSONObject.NULL)
            put("last_updated", System.currentTimeMillis())
        }

        val sectionFile = File(guideDir, "$sectionId.json")
        sectionFile.writeText(sectionData.toString(2))
        cache[sectionId] = parseGuideSection(sectionData)
    }

    suspend fun getSection(sectionId: String): GuideSection? = withContext(Dispatchers.IO) {
        cache[sectionId]?.let { return@withContext it }

        val sectionFile = File(guideDir, "$sectionId.json")
        if (sectionFile.exists()) {
            val sectionData = JSONObject(sectionFile.readText())
            val section = parseGuideSection(sectionData)
            cache[sectionId] = section
            return@withContext section
        }

        null
    }

    suspend fun getGuideStructure(): List<GuideSection> = withContext(Dispatchers.IO) {
        val sections = guideDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val sectionData = JSONObject(file.readText())
                    parseGuideSection(sectionData)
                } catch (e: Exception) {
                    null
                }
            }
            ?: return@withContext emptyList()

        val rootSections = sections.filter { it.parentId == null }
        buildSectionHierarchy(rootSections, sections)
    }

    suspend fun searchGuide(
        query: String,
        tags: List<String>? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val queryLower = query.lowercase()
        val allSections = getGuideStructure()
        
        allSections.flatMap { section ->
            searchInSection(section, queryLower, tags ?: emptyList())
        }.sortedByDescending { it.relevance }
    }

    private fun searchInSection(
        section: GuideSection,
        query: String,
        tags: List<String>
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        val titleMatch = section.title.lowercase().contains(query)
        val contentMatch = section.content.lowercase().contains(query)
        val tagMatch = tags.isEmpty() || (section.tags?.any { it in tags } == true)
        
        if ((titleMatch || contentMatch) && tagMatch) {
            results.add(
                SearchResult(
                    section = section,
                    relevance = when {
                        titleMatch -> 2
                        contentMatch -> 1
                        else -> 0
                    }
                )
            )
        }

        section.subsections?.forEach { subsection ->
            results.addAll(searchInSection(subsection, query, tags))
        }

        return results
    }

    private fun buildSectionHierarchy(
        currentLevel: List<GuideSection>,
        allSections: List<GuideSection>
    ): List<GuideSection> {
        return currentLevel.map { section ->
            val subsections = allSections.filter { it.parentId == section.sectionId }
            if (subsections.isNotEmpty()) {
                section.copy(subsections = buildSectionHierarchy(subsections, allSections))
            } else {
                section
            }
        }.sortedBy { it.order }
    }

    private fun parseGuideSection(json: JSONObject): GuideSection {
        return GuideSection(
            sectionId = json.getString("section_id"),
            title = json.getString("title"),
            content = json.getString("content"),
            parentId = json.optString("parent_id", null).takeUnless { it.isEmpty() },
            order = json.getInt("order"),
            tags = json.optJSONArray("tags")?.let { array ->
                List(array.length()) { i -> array.getString(i) }
            },
            lastUpdated = json.getLong("last_updated")
        )
    }

    data class GuideSection(
        val sectionId: String,
        val title: String,
        val content: String,
        val parentId: String? = null,
        val order: Int = 0,
        val tags: List<String>? = null,
        val lastUpdated: Long,
        val subsections: List<GuideSection>? = null
    )

    data class SearchResult(
        val section: GuideSection,
        val relevance: Int
    )

    companion object {
        @Volatile
        private var INSTANCE: UserGuide? = null

        fun getInstance(context: Context): UserGuide {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserGuide(context).also { INSTANCE = it }
            }
        }
    }
}