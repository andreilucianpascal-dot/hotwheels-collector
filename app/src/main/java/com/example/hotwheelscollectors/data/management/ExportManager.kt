package com.example.hotwheelscollectors.data.management

import android.content.Context
import android.net.Uri
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carDao: CarDao,
    private val photoDao: PhotoDao,
    private val gson: Gson
) {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    suspend fun exportToJson(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _exportState.value = ExportState.Exporting

            val cars = carDao.getAllCars().first()
            val photos = photoDao.getAllPhotos().first()

            val export = CollectionExport(
                cars = cars,
                photos = photos.map { it.copy(localPath = "") },
                exportDate = System.currentTimeMillis(),
                version = "1.0"
            )

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(gson.toJson(export).toByteArray())
            }

            _exportState.value = ExportState.Completed
            Result.success(Unit)
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(e.message ?: "Export failed")
            Result.failure(e)
        }
    }

    suspend fun exportToCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _exportState.value = ExportState.Exporting

            val cars = carDao.getAllCars().first()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.writer().use { writer ->
                    writer.write("ID,Model,Brand,Series,Year,Color,Condition,CurrentValue\n")
                    cars.forEach { car ->
                        writer.write(
                            "${car.id},${car.model},${car.brand},${car.series}," +
                                    "${car.year},${car.color},${car.condition},${car.currentValue}\n"
                        )
                    }
                }
            }

            _exportState.value = ExportState.Completed
            Result.success(Unit)
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(e.message ?: "Export failed")
            Result.failure(e)
        }
    }

    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        object Completed : ExportState()
        data class Error(val message: String) : ExportState()
    }

    data class CollectionExport(
        val cars: List<CarEntity>,
        val photos: List<PhotoEntity>,
        val exportDate: Long,
        val version: String
    )
}