package com.xyz.strapp.domain.repository

import android.database.Observable
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xyz.strapp.data.dao.ProfileDao
import com.xyz.strapp.domain.model.entity.ProfileEntity
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.domain.model.Unspecified
import com.xyz.strapp.endpoints.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
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
                if (tokenSP != null) {
                    val response = authApiService.getUserProfile("Bearer $tokenSP")
                    if (response.isSuccessful) {
                        response.body()?.let { profileData ->
                            // API call successful and body is not null
                            profileData.email?.let { email ->
                                saveProfileEmail(email)
                                Log.d("ProfileRepository", "Email saved via DataStore.")
                            } ?: Log.w("ProfileRepository", "Email is null in ProfileResponse from API.")
                            saveProfileData(profileData) // Save API data to local DB
                            Log.i("ProfileRepository", "Successfully fetched profile from API.")
                            Result.success(profileData)
                        } ?: run {
                            // API call successful BUT response body is NULL
                            Log.w("ProfileRepository", "API profile response body is null. Attempting to fetch from local DB.")
                            val localProfileResult = getProfileFromLocalDb() // Call the new function
                            if (localProfileResult.isSuccess) {
                                localProfileResult // Return local data
                            } else {
                                // API response was empty, and local fallback also failed
                                Log.e("ProfileRepository", "Failed to fetch profile from local DB after empty API response.", localProfileResult.exceptionOrNull())
                                Result.failure(Exception("Profile data not available: API response was empty and no local data found."))
                            }
                        }
                    } else {
                        val localProfileResult = getProfileFromLocalDb() // Call the new function
                        if (localProfileResult.isSuccess) {
                            localProfileResult // Return local data
                        } else {
                            // API response was empty, and local fallback also failed
                            Log.e("ProfileRepository", "Failed to fetch profile from local DB after empty API response.", localProfileResult.exceptionOrNull())
                            Result.failure(Exception("Profile data not available: API response was empty and no local data found."))
                        }
                    }
                } else {
                    val errorMsg = "Failed to get profile: Auth token is null."
                    Log.e("ProfileRepository", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "GetUserDetails network request or processing failed", e)
                when(e){
                    is ConnectException -> {
                        val localProfileResult = getProfileFromLocalDb() // Call the new function
                        if (localProfileResult.isSuccess) {
                            localProfileResult // Return local data
                        } else {
                            // API response was empty, and local fallback also failed
                            Log.e("ProfileRepository", "Failed to fetch profile from local DB after empty API response.", localProfileResult.exceptionOrNull())
                            Result.failure(Exception("Profile data not available: API response was empty and no local data found."))
                        }
                    }
                    else -> Result.failure(e)
                }
            }
        }
    }

    private suspend fun getProfileFromLocalDb(): Result<ProfileResponse> {
        return try {
            // Assuming profileDao.getProfile() returns Flow<List<ProfileEntity>>
            // and .first() gets the latest list from the Flow.
            val profileEntities = profileDao.getProfile().first()
            if (profileEntities.isNotEmpty()) {
                val profileEntity = profileEntities.first() // Using the first profile if multiple exist
                val profileResponse = ProfileResponse(
                    name = profileEntity.name,
                    code = profileEntity.code,
                    gender = profileEntity.gender,
                    employeeType = profileEntity.employeeType,
                    circle = profileEntity.circle,
                    division = profileEntity.division,
                    range = profileEntity.range,
                    section = profileEntity.section,
                    beat = profileEntity.beat,
                    shift = profileEntity.shift,
                    startTime = profileEntity.startTime,
                    endTime = profileEntity.endTime,
                    image = profileEntity.image,
                    email = profileEntity.email,
                    mobileNo = profileEntity.mobileNo,
                    agencyName = profileEntity.agencyName,
                    faceImageRequried = profileEntity.faceImageRequried
                )
                Log.i("ProfileRepository", "Successfully fetched profile from local DB.")
                Result.success(profileResponse)
            } else {
                Log.w("ProfileRepository", "No profile data found in local database.")
                Result.failure(Exception("No profile data found in local database."))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error fetching profile from local database", e)
            Result.failure(Exception("Error fetching profile from local database: ${e.message}", e))
        }
    }

    fun saveProfileData(profileData: ProfileResponse) {
        CoroutineScope(Dispatchers.IO).launch {
            val profileEntry = ProfileEntity(
                name = profileData.name ?: "",
                code = profileData.code ?: "",
                gender = profileData.gender ?: "",
                employeeType = profileData.employeeType ?: "",
                circle = profileData.circle ?: "",
                division = profileData.division ?: "",
                range = profileData.range ?: "",
                section = profileData.section ?: "",
                beat = profileData.beat ?: "",
                shift = profileData.shift ?: "",
                startTime = profileData.startTime ?: "",
                endTime = profileData.endTime ?: "",
                image = profileData.image ?: "",
                email = profileData.email ?: "",
                mobileNo = profileData.mobileNo ?: "",
                agencyName = profileData.agencyName ?: "",
                faceImageRequried = profileData.faceImageRequried ?: false
            )
            insertUserProfileInfo(profileEntry)
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