package com.example.hotwheelscollectors.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotwheelscollectors.data.repository.AuthRepository
import com.example.hotwheelscollectors.data.repository.FirestoreRepository
import com.example.hotwheelscollectors.data.repository.GoogleDriveRepository
import com.example.hotwheelscollectors.data.repository.OneDriveRepository
import com.example.hotwheelscollectors.data.repository.DropboxRepository
import com.example.hotwheelscollectors.data.repository.LocalRepository
import com.example.hotwheelscollectors.data.local.UserPreferences
import com.example.hotwheelscollectors.data.local.entities.PhotoType
import com.example.hotwheelscollectors.data.local.entities.SyncStatus
import com.example.hotwheelscollectors.utils.PhotoOptimizer
import com.example.hotwheelscollectors.utils.PhotoVersions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Date
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val localRepository: LocalRepository,
    private val googleDriveRepository: GoogleDriveRepository,
    private val oneDriveRepository: OneDriveRepository,
    private val dropboxRepository: DropboxRepository,
    private val userPreferences: UserPreferences,
    private val photoOptimizer: PhotoOptimizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraCaptureUiState())
    val uiState: StateFlow<CameraCaptureUiState> = _uiState.asStateFlow()

    private var currentPhotoUri: Uri? = null

    fun setCurrentPhotoUri(uri: Uri) {
        currentPhotoUri = uri
    }

    fun getCurrentPhotoUri(): Uri? = currentPhotoUri

    fun processPhotosAndReturn(
        frontPhotoUri: Uri?,
        backPhotoUri: Uri?,
        navController: NavController
    ) {
        if (frontPhotoUri == null || backPhotoUri == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "Error: Missing photos"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            message = "Processing photos..."
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Extract barcode from back photo
                val barcode = extractBarcodeFromImage(backPhotoUri)
                android.util.Log.d("CameraCaptureViewModel", "Extracted barcode: $barcode")

                // Step 2: Process front photo - create optimized versions
                val photoVersions = processFrontPhoto(frontPhotoUri)
                android.util.Log.d("CameraCaptureViewModel", "Processed front photo: ${photoVersions.thumbnailSizeKB}KB thumbnail, ${photoVersions.fullSizeSizeKB}KB full")

                // Step 3: Save photos to AddMainlineScreen via savedStateHandle
                val previousEntry = navController.previousBackStackEntry
                if (previousEntry != null) {
                    previousEntry.savedStateHandle.set("front_photo_uri", frontPhotoUri.toString())
                    previousEntry.savedStateHandle.set("back_photo_uri", backPhotoUri.toString())
                    previousEntry.savedStateHandle.set("extracted_barcode", barcode)
                    previousEntry.savedStateHandle.set("thumbnail_path", photoVersions.thumbnailPath)
                    previousEntry.savedStateHandle.set("full_size_path", photoVersions.fullSizePath)
                    
                    android.util.Log.d("CameraCaptureViewModel", "Photos saved to AddMainlineScreen")
                }

                // Step 4: Delete temporary back photo
                try {
                    val backFile = File(backPhotoUri.path!!)
                    if (backFile.exists()) {
                        backFile.delete()
                        android.util.Log.d("CameraCaptureViewModel", "Deleted temporary back photo")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CameraCaptureViewModel", "Failed to delete back photo: ${e.message}")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Photos processed successfully!"
                )

                // Navigate back to AddMainlineScreen
                navController.popBackStack()

            } catch (e: Exception) {
                android.util.Log.e("CameraCaptureViewModel", "Error processing photos: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error processing photos: ${e.message}"
                )
            }
        }
    }

    private suspend fun extractBarcodeFromImage(imageUri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val scanner = BarcodeScanning.getClient()
            val result = scanner.process(image).await()
            
            result.firstOrNull()?.rawValue ?: ""
        } catch (e: Exception) {
            android.util.Log.e("CameraCaptureViewModel", "Failed to extract barcode: ${e.message}", e)
            ""
        }
    }

    private suspend fun processFrontPhoto(frontPhotoUri: Uri): PhotoVersions {
        return try {
            // Load and correct orientation
            val originalBitmap = loadAndCorrectBitmap(frontPhotoUri)
            if (originalBitmap == null) {
                throw Exception("Failed to load front photo")
            }

            // Crop center for better composition
            val croppedBitmap = cropCenter(originalBitmap, 800, 600)
            
            // Save cropped bitmap to temp file
            val croppedFile = File(context.cacheDir, "FRONT_CROP_${System.currentTimeMillis()}.jpg")
            FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Create optimized versions using PhotoOptimizer
            val optimizedDir = File(context.cacheDir, "optimized_photos")
            if (!optimizedDir.exists()) optimizedDir.mkdirs()

            android.util.Log.d("CameraCaptureViewModel", "Creating optimized versions...")
            val photoVersions = photoOptimizer.createOptimizedVersions(
                Uri.fromFile(croppedFile),
                optimizedDir,
                "car_${System.currentTimeMillis()}"
            )
            android.util.Log.d("CameraCaptureViewModel", "PhotoVersions result:")
            android.util.Log.d("CameraCaptureViewModel", "  - thumbnailPath: '${photoVersions.thumbnailPath}'")
            android.util.Log.d("CameraCaptureViewModel", "  - fullSizePath: '${photoVersions.fullSizePath}'")
            android.util.Log.d("CameraCaptureViewModel", "  - thumbnailSizeKB: ${photoVersions.thumbnailSizeKB}")
            android.util.Log.d("CameraCaptureViewModel", "  - fullSizeSizeKB: ${photoVersions.fullSizeSizeKB}")

            // Cleanup
            originalBitmap.recycle()
            croppedBitmap.recycle()
            croppedFile.delete()

            photoVersions
        } catch (e: Exception) {
            android.util.Log.e("CameraCaptureViewModel", "Failed to process front photo: ${e.message}", e)
            PhotoVersions("", "", 0, 0, 0, 0)
        }
    }

    private fun loadAndCorrectBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Handle EXIF orientation
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifInputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            exifInputStream.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraCaptureViewModel", "Failed to load bitmap: ${e.message}", e)
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropCenter(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        // Calculate aspect ratios
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        
        val (cropWidth, cropHeight) = if (bitmapAspect > targetAspect) {
            // Bitmap is wider than target - crop width
            val newWidth = (bitmap.height * targetAspect).toInt()
            newWidth to bitmap.height
        } else {
            // Bitmap is taller than target - crop height
            val newHeight = (bitmap.width / targetAspect).toInt()
            bitmap.width to newHeight
        }
        
        val cropX = (bitmap.width - cropWidth) / 2
        val cropY = (bitmap.height - cropHeight) / 2
        
        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    }
}

data class CameraCaptureUiState(
    val isLoading: Boolean = false,
    val message: String = ""
)
