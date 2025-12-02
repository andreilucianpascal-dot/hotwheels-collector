package com.example.hotwheelscollectors.model

data class SortState(
    val field: String = "Name",
    val ascending: Boolean = true
) {
    companion object {
        val NAME_ASC = SortState("Name", true)
        val NAME_DESC = SortState("Name", false)
        val YEAR_ASC = SortState("Year", true)
        val YEAR_DESC = SortState("Year", false)
        val NUMBER_ASC = SortState("Number", true)
        val NUMBER_DESC = SortState("Number", false)
        val SERIES_ASC = SortState("Series", true)
        val SERIES_DESC = SortState("Series", false)
        val BRAND_ASC = SortState("Brand", true)
        val BRAND_DESC = SortState("Brand", false)
    }
}