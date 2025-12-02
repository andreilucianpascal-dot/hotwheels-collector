// app/src/test/java/com/example/hotwheelscollectors/data/repository/FirestoreRepositoryTest.kt
package com.example.hotwheelscollectors.data.repository

import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.dao.UserDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.local.entities.PhotoEntity
import com.example.hotwheelscollectors.data.local.entities.UserEntity
import com.example.hotwheelscollectors.model.HotWheelsCar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class FirestoreRepositoryTest {

    @Mock
    private lateinit var firestore: FirebaseFirestore

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var storage: FirebaseStorage

    @Mock
    private lateinit var carDao: CarDao

    @Mock
    private lateinit var photoDao: PhotoDao

    @Mock
    private lateinit var userDao: UserDao

    private lateinit var repository: FirestoreRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FirestoreRepository(
            firestore = firestore,
            auth = auth,
            storage = storage,
            carDao = carDao,
            photoDao = photoDao,
            userDao = userDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getCurrentUser returns authenticated user`() = runTest {
        // Given
        val mockUser = mock(com.google.firebase.auth.FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("testUserId")
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(auth.currentUser).thenReturn(mockUser)

        // When
        val result = repository.getCurrentUser()

        // Then
        assertNotNull(result)
        assertEquals("testUserId", result?.uid)
        assertEquals("test@example.com", result?.email)
    }

    @Test
    fun `test getCurrentUser returns null when not authenticated`() = runTest {
        // Given
        `when`(auth.currentUser).thenReturn(null)

        // When
        val result = repository.getCurrentUser()

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `test addCar successfully adds car`() = runTest {
        // Given
        val car = HotWheelsCar(
            id = "testId",
            model = "Test Model",
            brand = "Test Brand",
            year = 2023
        )
        val mockUser = mock(com.google.firebase.auth.FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("testUserId")
        `when`(auth.currentUser).thenReturn(mockUser)

        // When
        val result = repository.addCar(car)

        // Then
        assertTrue(result.isSuccess)
        verify(carDao).insertCar(any())
    }

    @Test
    fun `test getCarsForUser returns cars from local database`() = runTest {
        // Given
        val userId = "testUserId"
        val mockCars = listOf(
            CarEntity(id = "1", userId = userId, model = "Car 1"),
            CarEntity(id = "2", userId = userId, model = "Car 2")
        )
        `when`(carDao.getCarsForUser(userId)).thenReturn(flowOf(mockCars))

        // When
        val result = repository.getCarsForUser(userId)

        // Then
        assertNotNull(result)
        val cars = result.first() // Collect the Flow to get the actual data
        assertEquals(2, cars.size)
        assertEquals("Car 1", cars[0].model)
        assertEquals("Car 2", cars[1].model)
    }

    @Test
    fun `test getCarById returns car from local database`() = runTest {
        // Given
        val carId = "testCarId"
        val mockCar = CarEntity(id = carId, model = "Test Car")
        `when`(carDao.getCarById(carId)).thenReturn(mockCar)

        // When
        val result = repository.getCarById(carId)

        // Then
        assertNotNull(result)
        assertEquals(carId, result.id)
        assertEquals("Test Car", result.model)
    }

    @Test
    fun `test updateCar successfully updates car`() = runTest {
        // Given
        val car = CarEntity(id = "testId", model = "Updated Model")
        val mockUser = mock(com.google.firebase.auth.FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("testUserId")
        `when`(auth.currentUser).thenReturn(mockUser)

        // When
        val result = repository.updateCar(car)

        // Then
        assertTrue(result.isSuccess)
        verify(carDao).updateCar(car)
    }

    @Test
    fun `test deleteCar successfully deletes car`() = runTest {
        // Given
        val carId = "testCarId"
        val mockUser = mock(com.google.firebase.auth.FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("testUserId")
        `when`(auth.currentUser).thenReturn(mockUser)

        // When
        val result = repository.deleteCar(carId)

        // Then
        assertTrue(result.isSuccess)
        verify(carDao).deleteCarById(carId)
    }

    @Test
    fun `test searchCars returns filtered results`() = runTest {
        // Given
        val userId = "testUserId"
        val query = "test"
        val mockCars = listOf(
            CarEntity(id = "1", userId = userId, model = "Test Car 1"),
            CarEntity(id = "2", userId = userId, model = "Test Car 2")
        )
        `when`(carDao.searchCars(userId, query)).thenReturn(flowOf(mockCars))

        // When
        val result = repository.searchCars(userId, query)

        // Then
        assertNotNull(result)
        val cars = result.first() // Collect the Flow to get the actual data
        assertEquals(2, cars.size)
        assertTrue(cars.all { it.model.contains("Test") })
    }

    @Test
    fun `test operations fail when user not authenticated`() = runTest {
        // Given
        `when`(auth.currentUser).thenReturn(null)

        // When & Then
        val addResult = repository.addCar(
            HotWheelsCar(id = "test", model = "Test", brand = "Test", year = 2023)
        )
        assertFalse(addResult.isSuccess)

        val updateResult = repository.updateCar(
            CarEntity(id = "test", model = "Test")
        )
        assertFalse(updateResult.isSuccess)

        val deleteResult = repository.deleteCar("test")
        assertFalse(deleteResult.isSuccess)
    }
}