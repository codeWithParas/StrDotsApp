package com.xyz.strapp.domain.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "login_table")
data class LoginEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "isSuccess")
    val isSuccess: Boolean,
    @ColumnInfo(name = "token")
    val token: String,
    @ColumnInfo(name = "userId")
    val userId: Int,
    @ColumnInfo(name = "userName")
    val userName: String,
    @ColumnInfo(name = "userImageUrl")
    val userImageUrl: String? = "",
    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = "",
    @ColumnInfo(name = "tenentId")
    val tenentId: String,
    @ColumnInfo(name = "faceImageRequried")
    val faceImageRequried: Boolean,
)
