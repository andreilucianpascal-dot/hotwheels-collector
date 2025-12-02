// app/src/test/java/com/example/hotwheelscollectors/data/CarDatabaseTest.kt
package com.example.hotwheelscollectors.data

import com.example.hotwheelscollectors.data.local.CarDatabase
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CarDatabaseTest {
    private val carDatabase = CarDatabase

    @Test
    fun `getAllMainlineBrands returns sorted list`() {
        // When
        val brands = carDatabase.getAllMainlineBrands()

        // Then
        assertThat(brands).isInOrder()
        assertThat(brands).contains("Ferrari")
        assertThat(brands).contains("Porsche")
    }

    @Test
    fun `getModelsForBrand returns correct models`() {
        // When
        val ferrariModels = carDatabase.getModelsForBrand("ferrari")
        val porscheModels = carDatabase.getModelsForBrand("porsche")

        // Then
        assertThat(ferrariModels).contains("F40")
        assertThat(ferrariModels).contains("LaFerrari")
        assertThat(porscheModels).contains("911")
        assertThat(porscheModels).contains("918 Spyder")
    }

    @Test
    fun `generateSearchKeywords includes all relevant terms`() {
        // Given
        val car = HotWheelsCar(
            id = "test_id",
            name = "Ferrari F40",
            brand = "Ferrari",
            model = "F40",
            series = "Car Culture",
            subseries = "Premium",
            year = 2023,
            color = "Red",
            barcode = "TEST_123",
            isSTH = true,
            isPremium = true
        )

        // When
        val keywords = carDatabase.generateSearchKeywords(car)

        // Then
        assertThat(keywords).containsAtLeast(
            "ferrari",
            "f40",
            "car culture",
            "super treasure hunt"
        )
    }
}