package com.example.hotwheelscollectors.model

data class FilterState(
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val series: String? = null,
    val color: String? = null
) {
    val isActive: Boolean
        get() = minYear != null || maxYear != null || series != null || color != null
}