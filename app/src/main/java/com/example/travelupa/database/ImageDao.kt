package com.example.travelupa.database

import android.media.Image
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.travelupa.ui.theme.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Insert
    fun insert(image:ImageEntity): Long

    @Query("Select * FROM images WHERE id = :imageId")
    fun getImageById(imageId: Long): ImageEntity?

    @Query("SELECT * FROM images")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Delete
    fun delete(image: ImageEntity)
}