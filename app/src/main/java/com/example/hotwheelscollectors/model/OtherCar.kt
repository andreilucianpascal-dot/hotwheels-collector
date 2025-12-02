package com.example.hotwheelscollectors.model

data class OtherCar(
    val id: String = "",
    val model: String = "",
    val brand: String = "",
    val year: Int = 0,
    val photoUrl: String = "",
    val folderPath: String = "",
    val isPremium: Boolean = false,
    val timestamp: Long = 0L,
    val barcode: String = "",
    val frontPhotoPath: String = "",
    val backPhotoPath: String = "",
    val combinedPhotoPath: String = "",
    val searchKeywords: List<String> = emptyList(),
    val series: String = "",
    val color: String = "",
    val wheelType: String = "",
    val baseType: String = "",
    val category: String = "",
    val notes: String = ""
)