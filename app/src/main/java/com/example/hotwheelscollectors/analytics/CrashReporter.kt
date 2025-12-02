package com.example.hotwheelscollectors.analytics

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class CrashReporter private constructor(private val context: Context) {

    private val crashLogsDir: File = File(context.filesDir, "crash_logs")
    private val maxLogFiles = 50
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    init {
        setupCrashHandler()
        createCrashLogsDirectory()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createCrashLogsDirectory() {
        if (!crashLogsDir.exists()) {
            crashLogsDir.mkdirs()
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val crashData = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("thread_name", thread.name)
            put("exception_type", throwable.javaClass.name)
            put("exception_message", throwable.message)
            put("stack_trace", throwable.stackTraceToString())
            put("device_info", getDeviceInfo())
        }

        saveCrashLog(crashData)
    }

    private fun getDeviceInfo(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("android_version", Build.VERSION.SDK_INT)
            put("app_version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        }
    }

    private fun saveCrashLog(crashData: JSONObject) {
        try {
            val timestamp = dateFormat.format(Date())
            val logFile = File(crashLogsDir, "crash_$timestamp.json")
            logFile.writeText(crashData.toString(2))
            cleanupOldLogs()
        } catch (e: Exception) {
            android.util.Log.e("CrashReporter", "Failed to save crash log", e)
        }
    }

    suspend fun getCrashLogs(): List<JSONObject> = withContext(Dispatchers.IO) {
        crashLogsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { JSONObject(it.readText()) }
            ?.sortedByDescending { it.getLong("timestamp") }
            ?: emptyList()
    }

    private fun cleanupOldLogs() {
        crashLogsDir.listFiles()?.let { files ->
            if (files.size > maxLogFiles) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - maxLogFiles)
                    .forEach { it.delete() }
            }
        }
    }

    fun reportError(
        error: Throwable,
        additionalInfo: Map<String, Any>? = null
    ) {
        val errorData = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("error_type", error.javaClass.name)
            put("error_message", error.message)
            put("stack_trace", error.stackTraceToString())
            additionalInfo?.let { info ->
                put("additional_info", JSONObject(info))
            }
            put("device_info", getDeviceInfo())
        }

        saveCrashLog(errorData)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        crashLogsDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        @Volatile
        private var INSTANCE: CrashReporter? = null

        fun getInstance(context: Context): CrashReporter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashReporter(context).also { INSTANCE = it }
            }
        }
    }
}