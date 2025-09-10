package com.xyz.strapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xyz.strapp.data.dao.FaceImageDao
import com.xyz.strapp.data.dao.LoginDao
import com.xyz.strapp.data.dao.ProfileDao
import com.xyz.strapp.domain.model.entity.FaceImageEntity
import com.xyz.strapp.domain.model.entity.LoginEntity
import com.xyz.strapp.domain.model.entity.ProfileEntity

@Database(
    entities = [
        LoginEntity::class, 
        ProfileEntity::class, 
        FaceImageEntity::class
    ], 
    version = 1, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loginDao(): LoginDao
    abstract fun profileDao(): ProfileDao
    abstract fun faceImageDao(): FaceImageDao
}