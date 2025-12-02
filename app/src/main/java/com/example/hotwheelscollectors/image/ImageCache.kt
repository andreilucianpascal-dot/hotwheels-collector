// ImageCache.kt
package com.example.hotwheelscollectors.image

import android.content.Context
import android.net.Uri
import androidx.collection.LruCache
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCache @Inject constructor(
    private val context: Context
) {
    private val memoryCache = MemoryCache.Builder(context)
        .maxSizePercent(0.25) // Use 25% of app memory
        .build()

    private val diskCache = LruCache<String, String>(100)

    private val imageLoader = ImageLoader.Builder(context)
        .memoryCache(memoryCache)
        .crossfade(true)
        .build()

    suspend fun cacheImage(uri: Uri, type: ImageType) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(
                when (type) {
                    ImageType.THUMBNAIL -> 400
                    ImageType.PROFILE -> 500
                    else -> SIZE_ORIGINAL
                }
            )
            .build()

        imageLoader.execute(request)
        diskCache.put(uri.toString(), uri.toString())
    }

    fun getCachedImage(uri: Uri): Flow<Uri>? {
        val cached = diskCache.get(uri.toString())
        return cached?.let {
            flow { emit(Uri.parse(it)) }
        }
    }

    fun removeFromCache(uri: Uri) {
        memoryCache.remove(MemoryCache.Key(uri.toString()))
        diskCache.remove(uri.toString())
    }

    fun clearCache() {
        memoryCache.clear()
        diskCache.evictAll()
    }

    fun clearOldCache() {
        // Clear 50% of cache when running low on memory
        val halfSize = diskCache.maxSize() / 2
        diskCache.trimToSize(halfSize)
    }

    companion object {
        const val SIZE_ORIGINAL = -1
        
        @Volatile
        private var INSTANCE: ImageCache? = null

        fun getInstance(context: Context): ImageCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCache(context).also { INSTANCE = it }
            }
        }
    }
}