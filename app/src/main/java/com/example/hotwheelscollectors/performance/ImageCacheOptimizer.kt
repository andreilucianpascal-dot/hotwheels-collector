package com.example.hotwheelscollectors.performance

import android.content.Context
import android.graphics.Bitmap
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageCacheOptimizer private constructor(private val context: Context) {

    private val memoryCache: LruCache<String, Bitmap>
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val diskCacheDir: File

    init {
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

        // Initialize disk cache directory
        diskCacheDir = File(context.cacheDir, "image_cache")
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    suspend fun addBitmapToCache(key: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                // Add to memory cache
                memoryCache.put(key, bitmap)

                // Save to disk cache
                val file = File(diskCacheDir, key.hashCode().toString())
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
            } catch (e: IOException) {
                // Handle disk write errors silently
            }
        }
    }

    suspend fun getBitmapFromCache(key: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Try memory cache first
                memoryCache.get(key) ?: run {
                    // Try disk cache
                    val file = File(diskCacheDir, key.hashCode().toString())
                    if (file.exists()) {
                        try {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                // Add back to memory cache
                                memoryCache.put(key, bitmap)
                            }
                            bitmap
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                // Clear memory cache
                memoryCache.evictAll()

                // Clear disk cache
                diskCacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            } catch (e: Exception) {
                // Handle cleanup errors silently
            }
        }
    }

    fun getCacheSize(): Int {
        return memoryCache.size()
    }

    fun getMaxCacheSize(): Int {
        return memoryCache.maxSize()
    }

    companion object {
        @Volatile
        private var INSTANCE: ImageCacheOptimizer? = null

        fun getInstance(context: Context): ImageCacheOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCacheOptimizer(context).also { INSTANCE = it }
            }
        }
    }
}