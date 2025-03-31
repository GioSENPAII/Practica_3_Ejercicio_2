package com.example.cameramicapp.data.local

import android.content.Context
import android.net.Uri
import androidx.room.*
import androidx.room.migration.Migration
import java.util.Date

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val filename: String,
    val typeString: String, // "PHOTO" o "AUDIO"
    val creationDate: Date,
    val favorite: Boolean = false,
    val category: String = "Default",
    val thumbnailUriString: String? = null,
    val duration: Long? = null
)

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY creationDate DESC")
    suspend fun getAllMediaItems(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE typeString = :type ORDER BY creationDate DESC")
    suspend fun getMediaItemsByType(type: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE favorite = 1 ORDER BY creationDate DESC")
    suspend fun getFavoriteMediaItems(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY creationDate DESC")
    suspend fun getMediaItemsByCategory(category: String): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(mediaItem: MediaItemEntity): Long

    @Update
    suspend fun updateMediaItem(mediaItem: MediaItemEntity)

    @Delete
    suspend fun deleteMediaItem(mediaItem: MediaItemEntity)
}

@Database(entities = [MediaItemEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromUriString(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }
}