package com.xyz.strapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xyz.strapp.domain.model.LoginEntity
import com.xyz.strapp.domain.model.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(profileEntity: ProfileEntity)

    @Update
    suspend fun update(profileEntity: ProfileEntity)

    @Delete
    suspend fun delete(profileEntity: ProfileEntity)

    @Query("SELECT * FROM profile_table")
    fun getProfile(): Flow<List<ProfileEntity>>

}