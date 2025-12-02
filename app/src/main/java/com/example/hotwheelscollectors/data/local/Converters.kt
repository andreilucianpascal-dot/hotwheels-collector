package com.example.hotwheelscollectors.data.local

import androidx.room.TypeConverter
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.PhotoSyncStatus
import com.example.hotwheelscollectors.data.local.entities.DataSyncStatus
import java.util.Date

class Converters {

    // String List Converters
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // Double List Converters
    @TypeConverter
    fun fromDoubleList(value: List<Double>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toDoubleList(value: String?): List<Double> {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toDouble() } ?: emptyList()
    }

    // String Map Converters
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.entries?.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return value.split(";").associate {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }.filter { it.key.isNotEmpty() }
    }

    // Date Converters
    @TypeConverter
    fun fromDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // SyncStatus Converters
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? {
        return value?.let { SyncStatus.valueOf(it) }
    }

    // PhotoType Converters
    @TypeConverter
    fun fromPhotoType(value: PhotoType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPhotoType(value: String?): PhotoType? {
        return value?.let { PhotoType.valueOf(it) }
    }

    // PhotoSyncStatus Converters
    @TypeConverter
    fun fromPhotoSyncStatus(value: PhotoSyncStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPhotoSyncStatus(value: String?): PhotoSyncStatus? {
        return value?.let { PhotoSyncStatus.valueOf(it) }
    }

    // DataSyncStatus Converters
    @TypeConverter
    fun fromDataSyncStatus(value: DataSyncStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDataSyncStatus(value: String?): DataSyncStatus? {
        return value?.let { DataSyncStatus.valueOf(it) }
    }

    // Boolean List Converters
    @TypeConverter
    fun fromBooleanList(value: List<Boolean>?): String? {
        return value?.joinToString(",") { if (it) "1" else "0" }
    }

    @TypeConverter
    fun toBooleanList(value: String?): List<Boolean> {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it == "1" } ?: emptyList()
    }

    // Int List Converters
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() } ?: emptyList()
    }

    // Long List Converters
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toLong() } ?: emptyList()
    }
}