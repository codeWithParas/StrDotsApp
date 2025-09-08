package com.xyz.strapp.domain.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xyz.strapp.data.dao.ProfileDao
import com.xyz.strapp.domain.model.ProfileEntity
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.endpoints.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject


class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val authApiService: ApiService,
    private val dataStore: DataStore<Preferences> // Injected DataStore
) {

    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
    }

    suspend fun getUserProfile(): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) { // Switch to IO for network call
            try {
                val tokenSP = getAuthTokenOnce()
                if(tokenSP!=null)
                {
                    val response = authApiService.getUserProfile("Bearer $tokenSP")
                    if (response.isSuccessful) {
                        response.body()?.let { profileData ->
                            profileData.email?.let { email ->
                                saveProfileEmail(email)
                                Log.d("ProfileRepository", "Email saved via DataStore.")
                            } ?: Log.w("ProfileRepository", "Email is null in ProfileResponse.")
                            Result.success(profileData)
                        } ?: run {
                            Log.e("ProfileRepository", "Get Profile successful but response body is null")
                            Result.failure(Exception("Get Profile successful but response body is null"))
                        }
                    } else {
                        val errorMsg = "Failed to get profile: ${response.code()} ${response.message()}"
                        Log.e("ProfileRepository", errorMsg)
                        Result.failure(Exception(errorMsg))
                    }
                }
                else{
                    val errorMsg = "Failed to get token from SP."
                    Log.e("ProfileRepository", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "GetUserDetails network request failed", e)
                Result.failure(e)
            }
        }
    }
    suspend fun getEmail(): String {
        return if(getEmailFlow().first() == null){
            ""
        } else{
            getEmailFlow().first().toString()
        }
    }
    private suspend fun getAuthTokenOnce(): String? {
        return getAuthTokenFlow().first() // Gets the first emitted value
    }
    private fun getAuthTokenFlow(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("ProfileRepository", "Error reading auth token from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[ProfileRepository.PreferencesKeys.AUTH_TOKEN]
            }
    }
    private fun getEmailFlow(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("ProfileRepository", "Error reading Email from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[ProfileRepository.PreferencesKeys.PROFILE_EMAIL]
            }
    }
    private suspend fun saveProfileEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_EMAIL] = email
        }
    }
    private suspend fun insertUserProfileInfo(profileEntity: ProfileEntity) = profileDao.insert(profileEntity)
    private suspend fun updateUserProfileInfo(profileEntity: ProfileEntity) = profileDao.update(profileEntity)
    private suspend fun deleteUUserProfileInfo(profileEntity: ProfileEntity) = profileDao.delete(profileEntity)
    private suspend fun getUserInfo(): Flow<List<ProfileEntity>> = profileDao.getProfile()

}