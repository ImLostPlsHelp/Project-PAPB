package com.example.travelupa.ui.theme

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travelupa.TempatWisata
import java.util.Date

@Entity(tableName = "images")
data class ImageEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val localPath: String,
    val tempatWisataId: String? = null
)