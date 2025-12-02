package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.domain.manager.CameraManager
import com.example.hotwheelscollectors.domain.usecase.collection.AddCarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * AddSuperTreasureHuntViewModel handles adding Super Treasure Hunt cars.
 * 
 * SUPER TREASURE HUNT SPECIFIC LOGIC:
 * - Auto-completed fields: Barcode
 * - Editable fields: Name, Brand, Year, Color, Notes
 * - Series: "Mainline"
 * - isTH: false
 * - isSTH: true
 * - isPremium: false
 */
@HiltViewModel
class AddSuperTreasureHuntViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val addCarUseCase: AddCarUseCase,
    private val cameraManager: CameraManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddCarUiState>(AddCarUiState.Idle)
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    // Auto-completed fields: Barcode
    var barcode by mutableStateOf("")
        private set

    // Editable fields: Name, Brand, Year, Color, Notes
    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var year by mutableStateOf("")
    var color by mutableStateOf("")
    var notes by mutableStateOf("")

    // Photo URIs
    private var thumbnailUri: Uri? = null
    private var fullPhotoUri: Uri? = null

    /**
     * ✅ MAIN ENTRY POINT: Process photos AND save car (ca la Mainline)
     * This is the unified function that handles the entire flow.
     */
    fun processAndSaveCar(
        frontPhotoUri: Uri,
        backPhotoUri: Uri?
    ) {
        saveJob?.cancel()

        saveJob = persistentScope.launch {
            try {
                _uiState.value = AddCarUiState.ProcessingPhoto

                Timber.d("Processing and saving Super Treasure Hunt car")

                val result = cameraManager.processCarPhotos(frontPhotoUri, backPhotoUri)

                if (!result.success) {
                    Timber.e("Photo processing failed: ${result.error}")
                    _uiState.value = AddCarUiState.Error("Failed to process photos: ${result.error}")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    barcode = result.barcode
                }

                thumbnailUri = result.thumbnailUri
                fullPhotoUri = result.fullPhotoUri

                Timber.d("Photos optimized: thumbnail=${result.thumbnailUri}, full=${result.fullPhotoUri}")

                // Step 3: Save car locally FIRST (instant, blocking until saved)
                // ✅ CRITICAL: Save locally BEFORE setting Success
                // This ensures car appears in "My Collection" instantly
                // Thumbnail appears in Browse instantly (via sync incremental in background)
                saveCarInternal()
                
                // ✅ CRITICAL: Set Success ONLY AFTER local save is complete
                // Car is now saved locally and appears in "My Collection"
                // Navigation happens instantly, sync incremental continues in background
                withContext(Dispatchers.Main) {
                    _uiState.value = AddCarUiState.Success("Car saved!")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error in processAndSaveCar")
                if (isActive) {
                    _uiState.value = AddCarUiState.Error("Failed to process and save car: ${e.message}")
                }
            }
        }
    }

    /**
     * Processes photos to generate optimized versions (300KB thumbnail + 500KB full)
     * This happens silently without UI feedback - just prepares photos for instant save
     * 
     * @Deprecated Use processAndSaveCar() instead for unified processing and saving
     */
    @Deprecated("Use processAndSaveCar() for unified processing and saving", ReplaceWith("processAndSaveCar()"))
    suspend fun processPhotos(frontPhotoUri: Uri, backPhotoUri: Uri?) {
        try {
            Timber.d("Processing photos for Super Treasure Hunt car (silent optimization)")
            
            val result = cameraManager.processCarPhotos(frontPhotoUri, backPhotoUri)
            
            if (result.success) {
                withContext(Dispatchers.Main) {
                    barcode = result.barcode
                }
                thumbnailUri = result.thumbnailUri
                fullPhotoUri = result.fullPhotoUri
                
                Timber.d("Photos optimized: thumbnail=${result.thumbnailUri}, full=${result.fullPhotoUri}")
            } else {
                Timber.e("Photo processing failed: ${result.error}")
                throw IllegalStateException("Failed to process photos: ${result.error}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing photos")
            throw e
        }
    }

    // Job pentru a putea anula save-ul
    private var saveJob: kotlinx.coroutines.Job? = null
    private val persistentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Saves Super Treasure Hunt car using AddCarUseCase (Clean Architecture).
     * Photos are already optimized by processPhotos() - UseCase handles the rest.
     * ✅ FIX: Poate fi anulat dacă utilizatorul navighează înainte.
     */
    fun saveCar() {
        saveJob?.cancel()
        saveJob = persistentScope.launch {
            saveCarInternal()
        }
    }
    
    /**
     * ✅ FIX: Anulează save-ul în progres
     * Funcționează întotdeauna, indiferent de starea curentă
     */
    fun cancelSave() {
        try {
            saveJob?.cancel()
            saveJob = null
            // ✅ Asigură că state-ul este resetat la Idle pentru a permite navigare
            _uiState.value = AddCarUiState.Idle
            Timber.d("Save cancelled by user - state reset to Idle")
        } catch (e: Exception) {
            // Dacă apare orice eroare, resetăm totuși state-ul pentru a preveni blocarea
            _uiState.value = AddCarUiState.Idle
            Timber.w(e, "Error during cancelSave, but state reset to prevent blocking")
        }
    }

    private suspend fun saveCarInternal() {
        try {
            // ✅ Save locally FIRST - Success will be set AFTER this completes
            // This ensures car appears in "My Collection" before navigation

            val localThumbnail = thumbnailUri ?: throw IllegalStateException("Photos not optimized yet.")
            val localFull = fullPhotoUri ?: throw IllegalStateException("Photos not optimized yet.")

            val userId = getCurrentUserId()
            val thumbnailPath = uriToPathString(localThumbnail)
            val fullPath = uriToPathString(localFull)

            val carData = CarDataToSync(
                userId = userId,
                name = name,
                brand = brand,
                series = "Mainline",
                category = "STH",
                subcategory = null,
                color = color,
                year = year.toIntOrNull(),
                barcode = barcode,
                notes = notes,
                isTH = false,
                isSTH = true,
                isPremium = false,
                screenType = "SuperTreasureHunt",
                pendingPhotos = emptyList(),
                preOptimizedThumbnailPath = thumbnailPath,
                preOptimizedFullPath = fullPath
            )

            Timber.d("Saving Super Treasure Hunt car via AddCarUseCase...")

            // ✅ CRITICAL: Save locally FIRST (blocking until saved)
            // This ensures car appears in "My Collection" instantly
            // Sync incremental is launched in background by AddCarUseCase (non-blocking)
            val result = addCarUseCase.invoke(carData)
            
            if (result.isSuccess) {
                val carId = result.getOrNull()
                Timber.d("Super Treasure Hunt car saved successfully with ID: $carId")
                Timber.d("Car now appears in My Collection - thumbnail sync in background")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to save car"
                Timber.e("Failed to save Super Treasure Hunt car: $error")
                throw Exception(error)
            }

        } catch (e: kotlinx.coroutines.CancellationException) {
            Timber.d("Save cancelled: ${e.message}")
            withContext(Dispatchers.Main) {
                _uiState.value = AddCarUiState.Idle
            }
            throw e
        } catch (e: Exception) {
            if (coroutineContext.isActive) {
                Timber.e(e, "Failed to save Super Treasure Hunt car")
                withContext(Dispatchers.Main) {
                    _uiState.value = AddCarUiState.Error("Failed to save car: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Converts Uri to path String.
     * Handles both file:// and content:// URIs.
     */
    private fun uriToPathString(uri: Uri): String {
        return when (uri.scheme) {
            "file" -> uri.path ?: throw IllegalArgumentException("Invalid file URI path")
            "content" -> {
                // For content:// URIs, we need to get the actual file path
                // CameraManager should return file:// URIs, but handle content:// just in case
                val file = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                file.absolutePath
            }
            else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        }
    }

    /**
     * Updates editable fields
     */
    fun updateName(value: String) { name = value }
    fun updateBrand(value: String) { brand = value }
    fun updateYear(value: String) { year = value }
    fun updateColor(value: String) { color = value }
    fun updateNotes(value: String) { notes = value }

    /**
     * Gets current user ID
     */
    private suspend fun getCurrentUserId(): String {
        return try {
            val user = authRepository.getCurrentUser()
            user?.uid ?: throw IllegalStateException("User not authenticated")
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            throw e
        }
    }

    /**
     * Resets form after successful save (cleanup)
     */
    private fun resetForm() {
        viewModelScope.launch {
            name = ""
            brand = ""
            year = ""
            color = ""
            notes = ""
            barcode = ""
            thumbnailUri = null
            fullPhotoUri = null
            _uiState.value = AddCarUiState.Idle
            Timber.d("Form reset after successful save")
        }
    }
}