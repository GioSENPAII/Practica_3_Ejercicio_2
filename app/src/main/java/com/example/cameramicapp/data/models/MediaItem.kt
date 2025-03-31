package com.example.cameramicapp.data.models

import android.net.Uri
import java.util.Date

enum class MediaType {
    PHOTO,
    AUDIO
}

data class MediaItem(
    val id: Long = 0,
    val uri: Uri,
    val filename: String,
    val type: MediaType,
    val creationDate: Date = Date(),
    val favorite: Boolean = false,
    val category: String = "Default",
    // Para fotos
    val thumbnailUri: Uri? = null,
    // Para audio
    val duration: Long? = null
)