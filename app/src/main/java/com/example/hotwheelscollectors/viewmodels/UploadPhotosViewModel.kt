package com.example.hotwheelscollectors.viewmodels

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadPhotosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedPhotos: StateFlow<Set<Uri>> = _selectedPhotos.asStateFlow()

    private val _cameraLauncher = MutableStateFlow<ActivityResultLauncher<Uri>?>(null)

    fun addPhotos(uris: List<Uri>?) {
        uris?.let { newUris ->
            val currentPhotos = _selectedPhotos.value.toMutableSet()
            currentPhotos.addAll(newUris)
            _selectedPhotos.value = currentPhotos
        }
    }

    fun removePhoto(uri: Uri) {
        val currentPhotos = _selectedPhotos.value.toMutableSet()
        currentPhotos.remove(uri)
        _selectedPhotos.value = currentPhotos
    }

    fun clearPhotos() {
        _selectedPhotos.value = emptySet()
    }

    fun preparePhotoCapture(launcher: ActivityResultLauncher<Uri>) {
        _cameraLauncher.value = launcher
    }

    fun processNewPhoto() {
        // This would be called after a photo is taken
        // For now, we'll just update the UI state
        _uiState.value = UiState.Success
    }

    fun retryFailedOperation() {
        _uiState.value = UiState.Success
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}