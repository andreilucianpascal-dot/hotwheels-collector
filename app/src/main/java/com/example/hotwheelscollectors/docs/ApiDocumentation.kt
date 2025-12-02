package com.example.hotwheelscollectors.docs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ApiDocumentation private constructor(private val context: Context) {

    private val docsDir: File = File(context.filesDir, "api_docs")
    private val cache = mutableMapOf<String, JSONObject>()

    init {
        createDocsDirectory()
    }

    private fun createDocsDirectory() {
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }
    }

    suspend fun updateApiDoc(
        endpoint: String,
        method: String,
        description: String,
        parameters: List<ParameterDoc>,
        responses: List<ResponseDoc>,
        examples: List<ExampleDoc>? = null
    ) = withContext(Dispatchers.IO) {
        val docData = JSONObject().apply {
            put("endpoint", endpoint)
            put("method", method)
            put("description", description)
            put("parameters", parameters.map { it.toJson() })
            put("responses", responses.map { it.toJson() })
            examples?.let { put("examples", it.map { example -> example.toJson() }) }
            put("last_updated", System.currentTimeMillis())
        }

        val docFile = File(docsDir, "${method.lowercase()}_${endpoint.replace("/", "_")}.json")
        docFile.writeText(docData.toString(2))
        cache[getCacheKey(endpoint, method)] = docData
    }

    suspend fun getApiDoc(
        endpoint: String,
        method: String
    ): ApiDoc? = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(endpoint, method)
        
        cache[cacheKey]?.let { return@withContext parseApiDoc(it) }

        val docFile = File(docsDir, "${method.lowercase()}_${endpoint.replace("/", "_")}.json")
        if (docFile.exists()) {
            val docData = JSONObject(docFile.readText())
            cache[cacheKey] = docData
            return@withContext parseApiDoc(docData)
        }

        null
    }

    suspend fun listApiDocs(): List<ApiDoc> = withContext(Dispatchers.IO) {
        docsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val docData = JSONObject(file.readText())
                    parseApiDoc(docData)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedBy { it.endpoint }
            ?: emptyList()
    }

    suspend fun searchApiDocs(
        query: String,
        method: String? = null
    ): List<ApiDoc> = withContext(Dispatchers.IO) {
        val queryLower = query.lowercase()
        listApiDocs().filter { doc ->
            (method == null || doc.method.equals(method, ignoreCase = true)) &&
            (doc.endpoint.lowercase().contains(queryLower) ||
             doc.description.lowercase().contains(queryLower) ||
             doc.parameters.any { it.description.lowercase().contains(queryLower) })
        }
    }

    private fun getCacheKey(endpoint: String, method: String): String {
        return "${method.uppercase()}_$endpoint"
    }

    private fun parseApiDoc(json: JSONObject): ApiDoc {
        return ApiDoc(
            endpoint = json.getString("endpoint"),
            method = json.getString("method"),
            description = json.getString("description"),
            parameters = json.getJSONArray("parameters").let { array ->
                List(array.length()) { i ->
                    val paramJson = array.getJSONObject(i)
                    ParameterDoc(
                        name = paramJson.getString("name"),
                        type = paramJson.getString("type"),
                        description = paramJson.getString("description"),
                        required = paramJson.getBoolean("required")
                    )
                }
            },
            responses = json.getJSONArray("responses").let { array ->
                List(array.length()) { i ->
                    val respJson = array.getJSONObject(i)
                    ResponseDoc(
                        code = respJson.getInt("code"),
                        description = respJson.getString("description"),
                        schema = respJson.optJSONObject("schema")?.toString()
                    )
                }
            },
            examples = json.optJSONArray("examples")?.let { array ->
                List(array.length()) { i ->
                    val exampleJson = array.getJSONObject(i)
                    ExampleDoc(
                        title = exampleJson.getString("title"),
                        request = exampleJson.getString("request"),
                        response = exampleJson.getString("response")
                    )
                }
            },
            lastUpdated = json.getLong("last_updated")
        )
    }

    data class ApiDoc(
        val endpoint: String,
        val method: String,
        val description: String,
        val parameters: List<ParameterDoc>,
        val responses: List<ResponseDoc>,
        val examples: List<ExampleDoc>? = null,
        val lastUpdated: Long
    )

    data class ParameterDoc(
        val name: String,
        val type: String,
        val description: String,
        val required: Boolean
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("name", name)
            put("type", type)
            put("description", description)
            put("required", required)
        }
    }

    data class ResponseDoc(
        val code: Int,
        val description: String,
        val schema: String? = null
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("code", code)
            put("description", description)
            schema?.let { put("schema", JSONObject(it)) }
        }
    }

    data class ExampleDoc(
        val title: String,
        val request: String,
        val response: String
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("title", title)
            put("request", request)
            put("response", response)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ApiDocumentation? = null

        fun getInstance(context: Context): ApiDocumentation {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiDocumentation(context).also { INSTANCE = it }
            }
        }
    }
}