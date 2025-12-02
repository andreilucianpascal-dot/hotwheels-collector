// RoomDatabaseTest.kt
package com.example.hotwheelscollectors.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hotwheelscollectors.data.local.AppDatabase
import com.example.hotwheelscollectors.data.local.dao.CarDao
import com.example.hotwheelscollectors.data.local.dao.PhotoDao
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var carDao: CarDao
    private lateinit var photoDao: PhotoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        carDao = db.carDao()
        photoDao = db.photoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadCar() = runTest {
        // Given - Use the properties that actually exist in CarEntity
        val car = CarEntity(
            id = "test123",
            userId = "testUser",
            model = "Test Model",
            brand = "Ferrari",
            year = 2023,
            series = "Test Series",
            color = "Red"
        )

        // When
        carDao.insertCar(car)

        // Then
        val loaded = carDao.getCarById(car.id)
        assertEquals(car, loaded)
    }

    @Test
    fun searchCarsReturnsMatches() = runTest {
        // Given - Use the properties that actually exist in CarEntity
        val car1 = CarEntity(
            id = "1",
            userId = "testUser",
            model = "F40",
            brand = "Ferrari",
            year = 1990,
            series = "Mainline",
            color = "Red"
        )
        val car2 = CarEntity(
            id = "2",
            userId = "testUser",
            model = "911",
            brand = "Porsche",
            year = 2020,
            series = "Mainline",
            color = "Blue"
        )
        carDao.insertCar(car1)
        carDao.insertCar(car2)

        // When
        val results = carDao.searchCars("testUser", "Ferrari").first()

        // Then
        assertEquals(1, results.size)
        assertTrue(results.first().brand.contains("Ferrari"))
    }
}