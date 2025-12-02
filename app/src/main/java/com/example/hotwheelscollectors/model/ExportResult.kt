package com.example.hotwheelscollectors.model

sealed class ExportResult {
    object Success : ExportResult()
    data class Error(val message: String) : ExportResult()
}