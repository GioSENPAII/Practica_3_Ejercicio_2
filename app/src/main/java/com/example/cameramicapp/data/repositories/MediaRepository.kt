package com.example.cameramicapp.data.repositories

import android.content.Context
import android.net.Uri
import com.example.cameramicapp.data.local.AppDatabase
import com.example.cameramicapp.data.local.MediaItemEntity
import com.example.cameramicapp.data.models.MediaItem
import com.example.cameramicapp.data.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class MediaRepository(private val context: Context) {
    private val mediaItemDao = AppDatabase.getDatabase(context).mediaItemDao()

    suspend fun getAllMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItemDao.getAllMediaItems().map { it.toMediaItem() }
    }

    suspend fun getPhotoItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItemDao.getMediaItemsByType(MediaType.PHOTO.name).map { it.toMediaItem() }
    }

    suspend fun getAudioItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItemDao.getMediaItemsByType(MediaType.AUDIO.name).map { it.toMediaItem() }
    }

    suspend fun getFavoriteItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItemDao.getFavoriteMediaItems().map { it.toMediaItem() }
    }

    suspend fun getItemsByCategory(category: String): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItemDao.getMediaItemsByCategory(category).map { it.toMediaItem() }
    }

    suspend fun saveMediaItem(mediaItem: MediaItem): Long = withContext(Dispatchers.IO) {
        mediaItemDao.insertMediaItem(mediaItem.toEntity())
    }

    suspend fun updateMediaItem(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        mediaItemDao.updateMediaItem(mediaItem.toEntity())
    }

    suspend fun deleteMediaItem(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        // Borra el archivo físico
        try {
            val file = File(mediaItem.uri.path ?: "")
            if (file.exists()) {
                file.delete()
            }

            // Si tiene thumbnail, lo borra también
            mediaItem.thumbnailUri?.let {
                val thumbnailFile = File(it.path ?: "")
                if (thumbnailFile.exists()) {
                    thumbnailFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Borra el registro de la base de datos
        mediaItemDao.deleteMediaItem(mediaItem.toEntity())
    }

    // Métodos para convertir entre entidades y modelos
    private fun MediaItemEntity.toMediaItem(): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.parse(uriString),
            filename = filename,
            type = MediaType.valueOf(typeString),
            creationDate = creationDate,
            favorite = favorite,
            category = category,
            thumbnailUri = thumbnailUriString?.let { Uri.parse(it) },
            duration = duration
        )
    }

    private fun MediaItem.toEntity(): MediaItemEntity {
        return MediaItemEntity(
            id = id,
            uriString = uri.toString(),
            filename = filename,
            typeString = type.name,
            creationDate = creationDate,
            favorite = favorite,
            category = category,
            thumbnailUriString = thumbnailUri?.toString(),
            duration = duration
        )
    }
}