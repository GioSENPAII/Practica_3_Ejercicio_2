package com.example.cameramicapp.ui.camera

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import com.example.cameramicapp.data.repositories.MediaRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    private val _flashMode = MutableLiveData(FlashMode.AUTO)
    val flashMode: LiveData<FlashMode> = _flashMode

    private val _timerDuration = MutableLiveData(0) // 0 = no timer
    val timerDuration: LiveData<Int> = _timerDuration

    private val _currentFilter = MutableLiveData(CameraFilter.NORMAL)
    val currentFilter: LiveData<CameraFilter> = _currentFilter

    // Directorio para guardar fotos
    private val outputDirectory: File by lazy {
        val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraMicApp").apply { mkdirs() }
        }

        if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            application.filesDir
        }
    }

    fun setFlashMode(mode: FlashMode) {
        _flashMode.value = mode
    }

    fun setTimerDuration(seconds: Int) {
        _timerDuration.value = seconds
    }

    fun setFilter(filter: CameraFilter) {
        _currentFilter.value = filter
    }

    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "IMG_$timestamp.jpg"
        return File(outputDirectory, fileName)
    }

    fun savePhotoToGallery(photoUri: Uri, thumbnailUri: Uri? = null) {
        viewModelScope.launch {
            val file = File(photoUri.path ?: "")
            if (file.exists()) {
                val mediaItem = MediaItem(
                    uri = photoUri,
                    filename = file.name,
                    type = MediaType.PHOTO,
                    creationDate = Date(),
                    thumbnailUri = thumbnailUri
                )
                repository.saveMediaItem(mediaItem)
            }
        }
    }
}

enum class FlashMode {
    ON, OFF, AUTO
}

enum class CameraFilter {
    NORMAL, GRAYSCALE, SEPIA, NEGATIVE, VINTAGE
}