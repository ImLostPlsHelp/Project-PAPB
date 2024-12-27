package com.example.travelupa.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.travelupa.ui.theme.ImageEntity

@Database(entities = [ImageEntity::class], version=1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
}