package com.example.cameramicapp.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import com.example.cameramicapp.data.repositories.MediaRepository
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    private val _mediaItems = MutableLiveData<List<MediaItem>>()
    val mediaItems: LiveData<List<MediaItem>> = _mediaItems

    private val _selectedItem = MutableLiveData<MediaItem?>()
    val selectedItem: LiveData<MediaItem?> = _selectedItem

    init {
        loadAllMedia()
    }

    fun loadAllMedia() {
        viewModelScope.launch {
            _mediaItems.value = repository.getAllMediaItems()
        }
    }

    fun loadPhotoItems() {
        viewModelScope.launch {
            _mediaItems.value = repository.getPhotoItems()
        }
    }

    fun loadAudioItems() {
        viewModelScope.launch {
            _mediaItems.value = repository.getAudioItems()
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _mediaItems.value = repository.getFavoriteItems()
        }
    }

    fun loadByCategory(category: String) {
        viewModelScope.launch {
            _mediaItems.value = repository.getItemsByCategory(category)
        }
    }

    fun selectItem(item: MediaItem) {
        _selectedItem.value = item
    }

    fun clearSelection() {
        _selectedItem.value = null
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(favorite = !item.favorite)
            repository.updateMediaItem(updatedItem)

            // Refresh the list
            when {
                item.type == MediaType.PHOTO -> loadPhotoItems()
                item.type == MediaType.AUDIO -> loadAudioItems()
                else -> loadAllMedia()
            }
        }
    }

    fun updateCategory(item: MediaItem, newCategory: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(category = newCategory)
            repository.updateMediaItem(updatedItem)
            loadAllMedia()
        }
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMediaItem(item)
            loadAllMedia()
            if (_selectedItem.value?.id == item.id) {
                clearSelection()
            }
        }
    }

    fun importItem(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.saveMediaItem(mediaItem)
            // Recargar los elementos según la pestaña activa
            loadAllMedia()
        }
    }
}