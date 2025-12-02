package com.example.hotwheelscollectors.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MemoryManager private constructor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val maxMemory = Runtime.getRuntime().maxMemory()
    private val warningThreshold = 0.85 // 85% of max memory

    init {
        startMemoryMonitoring()
    }

    private fun startMemoryMonitoring() {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            monitorMemoryUsage()
        }
    }

    private suspend fun monitorMemoryUsage() {
        withContext(Dispatchers.Default) {
            while (true) {
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                if (isMemoryUsageCritical()) {
                    performMemoryOptimization()
                }

                kotlinx.coroutines.delay(MONITORING_INTERVAL)
            }
        }
    }

    private fun isMemoryUsageCritical(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return (usedMemory.toDouble() / maxMemory) > warningThreshold
    }

    suspend fun performMemoryOptimization() {
        withContext(Dispatchers.Default) {
            clearExcessCache()
            System.gc()
            Runtime.getRuntime().gc()
        }
    }

    fun performMemoryOptimizationSync() {
        try {
            clearExcessCacheSync()
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            android.util.Log.e("MemoryManager", "Failed to optimize memory", e)
        }
    }

    private fun clearExcessCacheSync() {
        try {
            val cacheDir = context.cacheDir
            val maxCacheSize = 50L * 1024 * 1024 // 50MB
            
            if (getDirSize(cacheDir) > maxCacheSize) {
                clearOldestCacheFiles(cacheDir, maxCacheSize)
            }
        } catch (e: Exception) {
            android.util.Log.e("MemoryManager", "Failed to clear cache", e)
        }
    }

    fun getMemoryMetrics(): MemoryMetrics {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory

        return MemoryMetrics(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = maxMemory,
            usedMemory = usedMemory,
            availableMemory = availableMemory
        )
    }

    private suspend fun clearExcessCache() {
        withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val maxCacheSize = 50L * 1024 * 1024 // 50MB
            
            if (getDirSize(cacheDir) > maxCacheSize) {
                clearOldestCacheFiles(cacheDir, maxCacheSize)
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun clearOldestCacheFiles(dir: File, targetSize: Long) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var currentSize = getDirSize(dir)

        for (file in files) {
            if (currentSize <= targetSize) break
            if (file.isFile) {
                currentSize -= file.length()
                file.delete()
            }
        }
    }

    data class MemoryMetrics(
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usedMemory: Long,
        val availableMemory: Long
    )

    companion object {
        private const val MONITORING_INTERVAL = 5000L // 5 seconds

        @Volatile
        private var INSTANCE: MemoryManager? = null

        fun getInstance(context: Context): MemoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryManager(context).also { INSTANCE = it }
            }
        }
    }
}