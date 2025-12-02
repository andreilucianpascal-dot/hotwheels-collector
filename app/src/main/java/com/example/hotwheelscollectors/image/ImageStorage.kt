// ImageStorage.kt
package com.example.hotwheelscollectors.image

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorage @Inject constructor(
    private val context: Context
) {
    suspend fun store(file: File, type: ImageType): String = withContext(Dispatchers.IO) {
        val directory = getTypeDirectory(type)
        val targetFile = File(
            directory,
            "img_${System.currentTimeMillis()}${type.extension}"
        )

        file.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        targetFile.absolutePath
    }

    fun load(path: String): Flow<Uri> = flow {
        val file = File(path)
        if (file.exists()) {
            emit(file.toUri())
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        File(path).delete()
    }

    private fun getTypeDirectory(type: ImageType): File {
        val baseDir = File(context.filesDir, "images")
        val typeDir = when (type) {
            ImageType.CAR_FRONT -> File(baseDir, "cars/front")
            ImageType.CAR_BACK -> File(baseDir, "cars/back")
            ImageType.CARD_FRONT -> File(baseDir, "cards/front")
            ImageType.CARD_BACK -> File(baseDir, "cards/back")
            ImageType.THUMBNAIL -> File(baseDir, "thumbnails")
            ImageType.PROFILE -> File(baseDir, "profiles")
        }
        typeDir.mkdirs()
        return typeDir
    }

    fun clearStorage() {
        File(context.filesDir, "images").deleteRecursively()
    }
}