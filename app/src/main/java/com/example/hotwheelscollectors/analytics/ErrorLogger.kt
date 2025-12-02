package com.example.hotwheelscollectors.analytics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class ErrorLogger private constructor(private val context: Context) {

    private val errorQueue = ConcurrentLinkedQueue<JSONObject>()
    private val logDir: File = File(context.filesDir, "error_logs")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val maxQueueSize = 1000
    private val maxLogAge = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds

    init {
        setupErrorLogger()
    }

    private fun setupErrorLogger() {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        cleanupOldLogs()
    }

    fun logError(
        error: Throwable,
        severity: ErrorSeverity,
        context: Map<String, Any>? = null
    ) {
        val errorData = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("severity", severity.name)
            put("error_type", error.javaClass.name)
            put("error_message", error.message)
            put("stack_trace", error.stackTraceToString())
            context?.let { contextMap ->
                put("context", JSONObject(contextMap))
            }
        }

        addToQueue(errorData)
        if (severity == ErrorSeverity.CRITICAL) {
            forcePersistErrors()
        }

        Log.e("ErrorLogger", "Error logged: ${error.message}", error)
    }

    fun logCustomError(
        message: String,
        severity: ErrorSeverity,
        context: Map<String, Any>? = null
    ) {
        val errorData = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("severity", severity.name)
            put("error_type", "CustomError")
            put("error_message", message)
            context?.let { contextMap ->
                put("context", JSONObject(contextMap))
            }
        }

        addToQueue(errorData)
        if (severity == ErrorSeverity.CRITICAL) {
            forcePersistErrors()
        }

        Log.e("ErrorLogger", "Custom error logged: $message")
    }

    private fun addToQueue(errorData: JSONObject) {
        errorQueue.offer(errorData)
        if (errorQueue.size > maxQueueSize) {
            errorQueue.poll()
        }
    }

    private fun forcePersistErrors() {
        val currentDate = dateFormat.format(Date())
        val logFile = File(logDir, "error_log_$currentDate.json")
        
        val errors = mutableListOf<JSONObject>()
        while (errorQueue.isNotEmpty()) {
            errorQueue.poll()?.let { errors.add(it) }
        }

        if (errors.isNotEmpty()) {
            try {
                val existingErrors = if (logFile.exists()) {
                    JSONObject(logFile.readText()).getJSONArray("errors")
                } else {
                    org.json.JSONArray()
                }

                val combinedErrors = org.json.JSONArray().apply {
                    for (i in 0 until existingErrors.length()) {
                        put(existingErrors.get(i))
                    }
                    errors.forEach { put(it) }
                }

                val logData = JSONObject().apply {
                    put("errors", combinedErrors)
                }

                logFile.writeText(logData.toString(2))
            } catch (e: Exception) {
                Log.e("ErrorLogger", "Failed to persist errors", e)
            }
        }
    }

    suspend fun getErrors(
        startDate: Date,
        endDate: Date,
        severity: ErrorSeverity? = null
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<JSONObject>()
        
        logDir.listFiles()?.forEach { file ->
            try {
                val fileDate = dateFormat.parse(file.nameWithoutExtension.removePrefix("error_log_"))
                if (fileDate in startDate..endDate) {
                    val logData = JSONObject(file.readText())
                    val fileErrors = logData.getJSONArray("errors")
                    
                    for (i in 0 until fileErrors.length()) {
                        val error = fileErrors.getJSONObject(i)
                        if (severity == null || error.getString("severity") == severity.name) {
                            errors.add(error)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ErrorLogger", "Failed to read error log file: ${file.name}", e)
            }
        }

        errors.sortedByDescending { it.getLong("timestamp") }
    }

    private fun cleanupOldLogs() {
        val currentTime = System.currentTimeMillis()
        logDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > maxLogAge) {
                file.delete()
            }
        }
    }

    enum class ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    companion object {
        @Volatile
        private var INSTANCE: ErrorLogger? = null

        fun getInstance(context: Context): ErrorLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ErrorLogger(context).also { INSTANCE = it }
            }
        }
    }
}