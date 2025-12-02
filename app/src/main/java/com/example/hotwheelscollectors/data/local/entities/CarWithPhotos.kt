package com.example.hotwheelscollectors.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CarWithPhotos(
    @Embedded val car: CarEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "carId"
    )
    val photos: List<PhotoEntity> = emptyList()
)