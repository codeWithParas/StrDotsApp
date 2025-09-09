package com.xyz.strapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xyz.strapp.domain.model.entity.LoginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoginDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(loginEntity: LoginEntity)

    @Update
    suspend fun update(loginEntity: LoginEntity)

    @Delete
    suspend fun delete(loginEntity: LoginEntity)

    @Query("SELECT * FROM login_table")
    fun getUser(): Flow<List<LoginEntity>>

    //delete all data
    @Query("DELETE FROM login_table")
    suspend fun deleteAll()
}