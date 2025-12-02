
package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.domain.manager.CameraManager
import com.example.hotwheelscollectors.domain.usecase.collection.AddCarUseCase
import com.example.hotwheelscollectors.utils.SmartCategorizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * AddMainlineViewModel handles adding Mainline cars.
 * 
 * MAINLINE SPECIFIC LOGIC:
 * - Auto-completed fields: Category, Brand, Barcode
 * - Editable fields: Model, Year, Color, Notes
 * - Series: "Mainline"
 * - isPremium: false
 * - isTH: false
 * - isSTH: false
 */
@HiltViewModel
class AddMainlineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val addCarUseCase: AddCarUseCase,
    private val cameraManager: CameraManager,
    private val smartCategorizer: SmartCategorizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddCarUiState>(AddCarUiState.Idle)
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    // Auto-completed fields: Category, Brand, Barcode
    var category by mutableStateOf("")
        private set
    var brand by mutableStateOf("")
        private set
    var barcode by mutableStateOf("")
        private set

    // Editable fields: Model, Year, Color, Notes
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var color by mutableStateOf("")
    var notes by mutableStateOf("")

    // Photo URIs (optimized versions: 300KB thumbnail + 500KB full)
    private var thumbnailUri: Uri? = null
    private var fullPhotoUri: Uri? = null

    /**
     * ✅ CLEAN ARCHITECTURE: Main entry point for processing and saving Mainline car.
     * UI layer (AddMainlineScreen) only passes data - ViewModel handles all business logic.
     * 
     * @param frontPhotoUri URI of front photo
     * @param backPhotoUri URI of back photo (optional)
     * @param category Auto-completed category from TakePhotosScreen
     * @param brand Auto-completed brand from TakePhotosScreen
     * @param preDetectedBarcode Barcode detected in TakePhotosScreen (optional, used if photo extraction fails)
     */
    fun processAndSaveCar(
        frontPhotoUri: Uri,
        backPhotoUri: Uri?,
        category: String,
        brand: String,
        preDetectedBarcode: String? = null
    ) {
        // Cancel previous job if exists
        saveJob?.cancel()

        // Cache values on main thread before launching background job
        this.category = category
        this.brand = brand
        Timber.d("Auto-completed fields (pre-save): category=$category, brand=$brand")

        // Start new processing and saving job on a persistent scope so navigation won't cancel it
        saveJob = persistentScope.launch {
            try {
                _uiState.value = AddCarUiState.ProcessingPhoto

                // Step 2: Process photos (optimize and extract barcode)
                Timber.d("Processing photos for Mainline car...")
                val result = cameraManager.processCarPhotos(frontPhotoUri, backPhotoUri)

                if (!result.success) {
                    Timber.e("Photo processing failed: ${result.error}")
                    _uiState.value = AddCarUiState.Error("Failed to process photos: ${result.error}")
                    return@launch
                }

                // Auto-complete barcode from photo processing
                // ✅ FIX: Folosește barcode-ul pre-detectat din TakePhotosScreen dacă există,
                // altfel folosește cel extras din back photo
                val finalBarcode = if (result.barcode.isNotEmpty()) {
                    result.barcode
                } else {
                    preDetectedBarcode ?: ""
                }
                withContext(Dispatchers.Main) {
                    barcode = finalBarcode
                }
                Timber.d("Barcode: extracted='${result.barcode}', preDetected='$preDetectedBarcode', final='$finalBarcode'")

                // Store optimized photo URIs (300KB thumbnail + 500KB full)
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
                    _uiState.value = AddCarUiState.Error("Failed to process car: ${e.message}")
                }
            }
        }
    }
    
    /**
     * @deprecated Use processAndSaveCar() instead. This method is kept for backward compatibility.
     */
    @Deprecated("Use processAndSaveCar() instead")
    suspend fun processPhotos(frontPhotoUri: Uri, backPhotoUri: Uri?) {
        Timber.w("processPhotos() is deprecated - use processAndSaveCar() instead")
        val result = cameraManager.processCarPhotos(frontPhotoUri, backPhotoUri)
        
        if (result.success) {
            barcode = result.barcode
            thumbnailUri = result.thumbnailUri
            fullPhotoUri = result.fullPhotoUri
        } else {
            throw IllegalStateException("Failed to process photos: ${result.error}")
        }
    }

    /**
     * @deprecated Use processAndSaveCar() instead. This method is kept for backward compatibility.
     */
    @Deprecated("Use processAndSaveCar() instead")
    fun updateAutoCompletedFields(category: String, brand: String) {
        Timber.w("updateAutoCompletedFields() is deprecated - use processAndSaveCar() instead")
        this.category = category
        this.brand = brand
    }

    // Job pentru a putea anula save-ul
    private var saveJob: kotlinx.coroutines.Job? = null
    
    /**
     * Saves Mainline car using AddCarUseCase (Clean Architecture).
     * ✅ NOTE: This is called internally by processAndSaveCar() after photos are processed.
     * Photos are already optimized by processAndSaveCar() - UseCase handles the rest.
     * ✅ FIX: Poate fi anulat dacă utilizatorul navighează înainte.
     * 
     * ⚠️ WARNING: This method requires photos to be processed first (thumbnailUri and fullPhotoUri must be set).
     * For BrandSelectionScreen compatibility, this is kept public but should only be called after processAndSaveCar().
     */
    fun saveCar() {
        // Anulează save-ul anterior dacă există
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
    fun updateModel(value: String) { model = value }
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
     * Updates series (for BrandSelectionScreen)
     */
    fun updateSeries(value: String) { 
        // Series is fixed for Mainline, but this function is needed for BrandSelectionScreen
    }
    
    /**
     * Updates brand (for BrandSelectionScreen)
     */
    fun updateBrand(value: String) { 
        brand = value 
    }
    
    /**
     * Updates name/model (for BrandSelectionScreen)
     */
    fun updateName(value: String) { 
        model = value 
    }
    
    /**
     * Resets form after successful save (cleanup)
     */
    private fun resetForm() {
        viewModelScope.launch {
            model = ""
            year = ""
            color = ""
            notes = ""
            category = ""
            brand = ""
            barcode = ""
            thumbnailUri = null
            fullPhotoUri = null
            _uiState.value = AddCarUiState.Idle
            Timber.d("Form reset after successful save")
        }
    }

    private val persistentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun saveCarInternal() {
        try {
            // ✅ Success was already set in processAndSaveCar() after photo processing
            // Don't override it with Saving - navigation already happened

            // Get current user ID
            val userId = getCurrentUserId()

            val localThumbnail = thumbnailUri ?: throw IllegalStateException("Thumbnail URI missing")
            val localFull = fullPhotoUri ?: throw IllegalStateException("Full photo URI missing")

            val thumbnailPath = uriToPathString(localThumbnail)
            val fullPath = uriToPathString(localFull)

            val autoCompletedModel = if (model.isEmpty() && brand.isNotEmpty() && category.isNotEmpty()) {
                val subcategory = if (category.contains("/")) {
                    category.substringAfter("/").takeIf { it.isNotEmpty() } ?: category
                } else {
                    category
                }
                "$brand $subcategory"
            } else {
                model
            }

            Timber.d("Auto-completed model: '$autoCompletedModel' (original: '$model', brand: '$brand', category: '$category')")

            val carData = CarDataToSync(
                userId = userId,
                name = autoCompletedModel,
                brand = brand,
                series = "Mainline",
                category = category,
                subcategory = null,
                color = color,
                year = year.toIntOrNull(),
                barcode = barcode,
                notes = notes,
                isTH = false,
                isSTH = false,
                isPremium = false,
                screenType = "Mainline",
                pendingPhotos = emptyList(),
                preOptimizedThumbnailPath = thumbnailPath,
                preOptimizedFullPath = fullPath
            )

            Timber.d("Saving Mainline car via AddCarUseCase...")

            // ✅ CRITICAL: Save locally FIRST (blocking until saved)
            // This ensures car appears in "My Collection" instantly
            // Sync incremental is launched in background by AddCarUseCase (non-blocking)
            val result = addCarUseCase.invoke(carData)
            
            if (result.isSuccess) {
                val carId = result.getOrNull()
                Timber.d("Mainline car saved successfully with ID: $carId")
                Timber.d("Car now appears in My Collection - thumbnail sync in background")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to save car"
                Timber.e("Failed to save Mainline car: $error")
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
                Timber.e(e, "Failed to save Mainline car")
                withContext(Dispatchers.Main) {
                    _uiState.value = AddCarUiState.Error("Failed to save car: ${e.message}")
                }
            }
        }
    }
}

