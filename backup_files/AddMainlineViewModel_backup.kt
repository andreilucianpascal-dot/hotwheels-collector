
package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.CarDataToSync
import com.example.hotwheelscollectors.data.repository.CarSyncRepository
import com.example.hotwheelscollectors.data.repository.PhotoData
import com.example.hotwheelscollectors.data.repository.PhotoProcessingRepository
import com.example.hotwheelscollectors.utils.CarUserInfo
import com.example.hotwheelscollectors.utils.SaveLocation
import com.example.hotwheelscollectors.utils.SmartCategorizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// --- UI State --- //
sealed class UiState {
    object Idle : UiState()
    object Saving : UiState()
    object ProcessingPhoto : UiState()
    object PhotoProcessed : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
    data class ContributedToGlobal(val carId: String, val message: String) : UiState()
    data class SmartCategorized(val saveLocation: SaveLocation) : UiState()
    object RequiresUserSelection : UiState()
    data class LowConfidenceCategory(val saveLocation: SaveLocation) : UiState()
    data class ExportReady(val exportData: String) : UiState()
    data class ShareReady(val shareText: String) : UiState()
}

@HiltViewModel
class AddMainlineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val photoProcessingRepository: PhotoProcessingRepository,
    private val addCarUseCase: com.example.hotwheelscollectors.domain.usecase.collection.AddCarUseCase,
    private val smartCategorizer: SmartCategorizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- Form State --- //
    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var series by mutableStateOf("Mainline") // Auto-set to Mainline for AddMainlineViewModel
    var subseries by mutableStateOf("") // Category selected by user (e.g., "Convertibles")
    var color by mutableStateOf("")
    var year by mutableStateOf<Int?>(null)
    var barcode by mutableStateOf("")
    var notes by mutableStateOf("")
    var isTH by mutableStateOf(false)
    var isSTH by mutableStateOf(false)
    var photos by mutableStateOf<List<Uri>>(emptyList())
    
    // --- Internal State --- //
    private var currentPhotoUri: Uri? = null
    private var pendingPhotos = mutableListOf<PhotoData>()
    private var thumbnailPath: String = ""
    private var fullSizePath: String = ""
    private var isSaving = false

    val hasUnsavedChanges: Boolean
        get() = name.isNotEmpty() || brand.isNotEmpty() || series.isNotEmpty() ||
                subseries.isNotEmpty() || color.isNotEmpty() || year != null || 
                barcode.isNotEmpty() || notes.isNotEmpty() || photos.isNotEmpty() || isTH || isSTH

    // --- Form Update Functions --- //
    fun updateName(value: String) { name = value }
    fun updateBrand(value: String) { brand = value }
    fun updateSeries(value: String) { series = value }
    fun updateSubseries(value: String) { subseries = value }
    fun updateColor(value: String) { color = value }
    fun updateYear(value: Int) { year = value }
    fun updateBarcode(value: String) { barcode = value }
    fun updateNotes(value: String) { notes = value }
    fun updateIsTH(value: Boolean) { isTH = value }
    fun updateIsSTH(value: Boolean) { isSTH = value }

    /**
     * Called from CameraCaptureScreen with paths to already optimized photos.
     */
    fun setOptimizedPhotoPaths(thumbnailPath: String, fullSizePath: String) {
        this.thumbnailPath = thumbnailPath
        this.fullSizePath = fullSizePath
        Log.d("AddMainlineViewModel", "Set optimized photo paths: thumb=$thumbnailPath, full=$fullSizePath")
    }

    // --- Main Save Function --- //
    fun saveCar(screenType: String = "Mainline") {
        if (isSaving) {
            Log.w("AddMainlineViewModel", "saveCar() already in progress - ignoring duplicate call")
            return
        }
        isSaving = true
        _uiState.value = UiState.Saving

        // 🔧 Use GlobalScope to prevent cancellation when user navigates away
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // 1. Validate input
                val validationError = validateCarData()
                if (validationError != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = UiState.Error(validationError)
                    }
                    return@launch
                }
                val currentUserId = getCurrentUserId()

                // 2. Package all data for the repository
                Log.d("AddMainlineViewModel", "=== PACKAGING CAR DATA FOR SYNC ===")
                Log.d("AddMainlineViewModel", "Pending photos count: ${pendingPhotos.size}")
                pendingPhotos.forEachIndexed { index, photo ->
                    Log.d("AddMainlineViewModel", "Pending photo $index: ${photo.savedPath}")
                    Log.d("AddMainlineViewModel", "Pending photo $index exists: ${File(photo.savedPath).exists()}")
                }
                Log.d("AddMainlineViewModel", "Pre-optimized thumbnail: $thumbnailPath")
                Log.d("AddMainlineViewModel", "Pre-optimized full: $fullSizePath")
                
                val carData = CarDataToSync(
                    userId = currentUserId,
                    name = name.trim(),
                    brand = brand.trim(),
                    series = "Mainline", // ✅ FIXED: Always save as Mainline series
                    category = subseries.trim(), // ✅ FIXED: Use selected category (Rally, Convertibles, etc.)
                    color = color.trim(),
                    year = year,
                    barcode = barcode.trim(),
                    notes = notes.trim(),
                    isTH = isTH,
                    isSTH = isSTH,
                    screenType = screenType,
                    pendingPhotos = pendingPhotos.toList(),
                    preOptimizedThumbnailPath = thumbnailPath,
                    preOptimizedFullPath = fullSizePath
                )
                
                Log.d("AddMainlineViewModel", "CarDataToSync created with ${carData.pendingPhotos.size} photos")
                carData.pendingPhotos.forEachIndexed { index, photo ->
                    Log.d("AddMainlineViewModel", "CarData photo $index: ${photo.savedPath}")
                }

                // 3. Delegate all work to AddCarUseCase
                Log.d("AddMainlineViewModel", "Delegating save to AddCarUseCase...")
                val addResult = addCarUseCase.invoke(carData)

                // 4. Update UI based on the result
                withContext(Dispatchers.Main) {
                    if (addResult.isSuccess) {
                        val carId = addResult.getOrNull()
                        _uiState.value = UiState.ContributedToGlobal(carId ?: "", "Car saved successfully!")
                        // ✅ FIXED: Only cleanup after successful save
                        cleanupAfterSave()
                    } else {
                        _uiState.value = UiState.Error(addResult.exceptionOrNull()?.message ?: "Failed to save car")
                    }
                }

            } catch (e: IllegalStateException) { // Specifically for auth errors
                Log.e("AddMainlineViewModel", "Authentication error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Error(e.message ?: "User not authenticated")
                }
            } catch (e: Exception) {
                Log.e("AddMainlineViewModel", "An unexpected error occurred during save: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Error("An unexpected error occurred: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    // ✅ FIXED: Don't cleanup photos here - only cleanup after successful save
                    // cleanupAfterSave() // REMOVED from finally block
                }
            }
        }
    }

    // --- Photo Handling --- //

    fun preparePhotoCapture(launcher: ActivityResultLauncher<Uri>) {
        try {
            val photoFile = createPhotoFile()
            currentPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            currentPhotoUri?.let { launcher.launch(it) }
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Failed to prepare camera: ${e.message}")
        }
    }

    fun processNewPhoto() {
        viewModelScope.launch {
            currentPhotoUri?.let {
                handlePhotoUri(it)
            }
        }
    }

    fun processSelectedPhoto(uri: Uri) {
        viewModelScope.launch {
            handlePhotoUri(uri)
        }
    }
    
    suspend fun processSelectedPhotoSync(uri: Uri) {
        handlePhotoUri(uri)
    }

    private suspend fun handlePhotoUri(uri: Uri) {
        _uiState.value = UiState.ProcessingPhoto
        try {
            Log.d("AddMainlineViewModel", "=== PROCESSING PHOTO URI ===")
            Log.d("AddMainlineViewModel", "URI: $uri")
            Log.d("AddMainlineViewModel", "Current pendingPhotos count: ${pendingPhotos.size}")
            
            val photoData = photoProcessingRepository.processPhotoFile(uri)
            if (photoData != null) {
                Log.d("AddMainlineViewModel", "PhotoData created successfully:")
                Log.d("AddMainlineViewModel", "  - savedPath: ${photoData.savedPath}")
                Log.d("AddMainlineViewModel", "  - file exists: ${File(photoData.savedPath).exists()}")
                Log.d("AddMainlineViewModel", "  - file size: ${File(photoData.savedPath).length()} bytes")
                
                pendingPhotos.add(photoData)
                photos = photos + photoData.originalUri // Update UI
                _uiState.value = UiState.PhotoProcessed
                Log.d("AddMainlineViewModel", "Photo added. Total photos: ${photos.size}, Total pendingPhotos: ${pendingPhotos.size}")
            } else {
                Log.e("AddMainlineViewModel", "PhotoData is null!")
                _uiState.value = UiState.Error("Failed to process photo.")
                }
            } catch (e: Exception) {
            Log.e("AddMainlineViewModel", "Photo processing error: ${e.message}", e)
            _uiState.value = UiState.Error("Photo processing error: ${e.message}")
        }
    }

    fun deletePhoto(uri: Uri) {
        photos = photos - uri
        pendingPhotos.removeAll { it.originalUri == uri }
        viewModelScope.launch {
            uri.path?.let { safePath ->
                photoProcessingRepository.deleteTemporaryPhoto(safePath)
            }
        }
        Log.d("AddMainlineViewModel", "Photo deleted. Remaining photos: ${photos.size}")
    }
    
    // --- Smart Categorization --- //

    fun processMLResults(scannedBarcode: String?, extractedText: String?) {
        barcode = scannedBarcode ?: barcode
        val textList = extractedText?.split("\n") ?: emptyList()

        viewModelScope.launch {
            try {
                val userInfo = CarUserInfo(name = name, brand = brand, series = series)
                val saveLocation = smartCategorizer.categorizeCarAutomatically(barcode, textList, userInfo)

                if (!saveLocation.requiresUserSelection) {
                    autoFillFromSmartCategory(saveLocation)
                }

                _uiState.value = when {
                    saveLocation.confidence > 0.8f -> UiState.SmartCategorized(saveLocation)
                    saveLocation.requiresUserSelection -> UiState.RequiresUserSelection
                    else -> UiState.LowConfidenceCategory(saveLocation)
                }
        } catch (e: Exception) {
                _uiState.value = UiState.Error("Categorization failed: ${e.message}")
            }
        }
    }

    private fun autoFillFromSmartCategory(saveLocation: SaveLocation) {
        saveLocation.series?.let { series = it }
        saveLocation.brand?.let { brand = it }
    }

    // --- Utility Functions --- //

    private fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.uid
            ?: throw IllegalStateException("User must be authenticated.")
    }

    private fun cleanupAfterSave() {
        Log.d("AddMainlineViewModel", "=== CLEANUP AFTER SAVE ===")
        Log.d("AddMainlineViewModel", "Clearing pendingPhotos count: ${pendingPhotos.size}")
        pendingPhotos.forEachIndexed { index, photo ->
            Log.d("AddMainlineViewModel", "Clearing photo $index: ${photo.savedPath}")
        }
        
        name = ""
        brand = ""
        series = ""
        subseries = ""
        color = ""
        year = null
        barcode = ""
        notes = ""
        isTH = false
        isSTH = false
        photos = emptyList()
        pendingPhotos.clear()
        currentPhotoUri = null
        thumbnailPath = ""
        fullSizePath = ""
        isSaving = false
        
        Log.d("AddMainlineViewModel", "Cleanup completed. pendingPhotos count: ${pendingPhotos.size}")
    }
    
    private fun createPhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)?.resolve("HotWheelsPhotos")?.apply { mkdirs() }
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun validateCarData(): String? {
        // Validation is handled by AddCarUseCase
        // This ViewModel just prepares the data
        return null // All good
    }
}

