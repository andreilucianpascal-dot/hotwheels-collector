package com.example.hotwheelscollectors.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.PriceHistoryDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.CarWithPhotos
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class CarDetailsViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CarDetailsViewModel
    private lateinit var carDao: CarDao
    private lateinit var photoDao: PhotoDao
    private lateinit var priceHistoryDao: PriceHistoryDao

    @Before
    fun setup() {
        carDao = mock<CarDao>()
        photoDao = mock<PhotoDao>()
        priceHistoryDao = mock<PriceHistoryDao>()
        viewModel = CarDetailsViewModel(carDao, photoDao, priceHistoryDao)
    }

    @Test
    fun `loadCar success updates state with car details`() = runTest {
        // Given
        val carId = "car123"
        val car = CarEntity(id = carId, model = "Test Car", brand = "Test Brand", series = "Test Series", year = 2023, isPremium = false, userId = "user123")
        val photos = listOf(
            PhotoEntity(id = "1", carId = carId, localPath = "/path1", type = PhotoType.FRONT, order = 1),
            PhotoEntity(id = "2", carId = carId, localPath = "/path2", type = PhotoType.BACK, order = 2)
        )
        val carWithPhotos = CarWithPhotos(car, photos)

        whenever(carDao.getCarWithPhotosById(carId)).thenReturn(flowOf(carWithPhotos))

        // When
        viewModel.loadCar(carId)

        // Then
        val uiState = viewModel.uiState.first()
        assertThat(uiState).isEqualTo(CarDetailsViewModel.UiState.Success(carWithPhotos))
    }

    @Test
    fun `loadCar not found updates state with error`() = runTest {
        // Given
        val carId = "nonexistent"
        whenever(carDao.getCarWithPhotosById(carId)).thenReturn(flowOf(null))

        // When
        viewModel.loadCar(carId)

        // Then
        val uiState = viewModel.uiState.first()
        assertThat(uiState).isEqualTo(CarDetailsViewModel.UiState.Error("Car not found"))
    }

    @Test
    fun `deleteCar updates state to deleted`() = runTest {
        // Given
        val carId = "car123"

        // When
        viewModel.deleteCar(carId)

        // Then
        verify(carDao).deleteCarById(carId)
        val uiState = viewModel.uiState.first()
        assertThat(uiState).isEqualTo(CarDetailsViewModel.UiState.Deleted)
    }
}