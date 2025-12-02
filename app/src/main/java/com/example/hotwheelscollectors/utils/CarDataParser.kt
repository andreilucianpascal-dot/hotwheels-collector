package com.example.hotwheelscollectors.utils

data class CarParseResult(
    val model: String = "",
    val brand: String = "",
    val year: Int = 0,
    val barcode: String = ""
)

object CarDataParser {
    fun parseCarData(frontText: String, backText: String): CarParseResult {
        val barcode = extractBarcode(backText)
        val model = extractModel(frontText)
        val brand = extractBrand(frontText)
        val year = extractYear(frontText)
        return CarParseResult(
            model = model,
            brand = brand,
            year = year,
            barcode = barcode
        )
    }

    private fun extractBarcode(text: String): String {
        val barcodePattern = Regex("\\b\\d{8,13}\\b")
        return barcodePattern.find(text)?.value ?: ""
    }

    private fun extractModel(text: String): String {
        val words = text.split(" ")
        return words.firstOrNull { it.length > 2 } ?: "Unknown Model"
    }

    private fun extractBrand(text: String): String {
        val brands = listOf("Ford", "Chevrolet", "Dodge", "Nissan", "Toyota", "Honda", "BMW", "Mercedes", "Audi", "Porsche", "Ferrari", "Lamborghini")
        return brands.firstOrNull { text.contains(it, ignoreCase = true) } ?: "Unknown Brand"
    }

    private fun extractYear(text: String): Int {
        val yearPattern = Regex("\\b(19|20)\\d{2}\\b")
        return yearPattern.find(text)?.value?.toIntOrNull() ?: 2023
    }
}