package com.example.hotwheelscollectors.offline

data class FilterState(
    val isActive: Boolean = false,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val brand: String = "",
    val isPremium: Boolean? = null
)

enum class SortState {
    NAME_ASC,
    NAME_DESC,
    YEAR_ASC,
    YEAR_DESC,
    BRAND_ASC,
    BRAND_DESC
}

enum class ViewType {
    GRID,
    LIST
}

sealed class ExportResult {
    object Success : ExportResult()
    data class Error(val message: String) : ExportResult()
}