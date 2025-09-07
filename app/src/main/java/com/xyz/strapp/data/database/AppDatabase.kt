package com.xyz.strapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xyz.strapp.data.dao.FaceImageDao
import com.xyz.strapp.data.dao.LoginDao
import com.xyz.strapp.domain.model.FaceImageEntity
import com.xyz.strapp.domain.model.LoginEntity

@Database(entities = [LoginEntity::class, FaceImageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loginDao(): LoginDao
    abstract fun faceImageDao(): FaceImageDao
}