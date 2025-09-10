package com.xyz.strapp.domain.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xyz.strapp.data.dao.LoginDao
import com.xyz.strapp.domain.model.entity.LoginEntity
import com.xyz.strapp.domain.model.LoginRequest
import com.xyz.strapp.domain.model.LoginResponse
import com.xyz.strapp.endpoints.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val loginDao: LoginDao,
    private val apiService: ApiService,
    private val dataStore: DataStore<Preferences> // Injected DataStore
) {

    object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val AUTH_TENANT_ID = stringPreferencesKey("tenant_id")
    }

    /**
     * Attempts to log in a user via the remote API.
     *
     * @param loginRequest The login credentials.
     * @return A Result wrapping the LoginResponse on success, or an Exception on failure.
     */
    suspend fun loginUserRemote(loginRequest: LoginRequest): Result<LoginResponse> {
        return withContext(Dispatchers.IO) { // Switch to IO for network call
            try {
                val response = apiService.login(loginRequest)
                if (response.isSuccessful) {
                    response.body()?.let { loginData ->
                        loginData.token?.let { token ->
                            // Save token using DataStore (suspend function)

                            val currentTime = System.currentTimeMillis()
                            val expiryTime = currentTime + (24 * 60 * 60 * 1000)
                            saveAuthToken(token,expiryTime)

                            Log.d("LoginRepository", "Auth token saved via DataStore.")
                        } ?: Log.w("LoginRepository", "Access token is null in LoginResponse.")
                        loginData.tenentId?.let { tenantId ->
                            // Save token using DataStore (suspend function)
                            saveTenantId(tenantId)
                            Log.d("LoginRepository", "Auth token saved via DataStore.")
                        } ?: Log.w("LoginRepository", "Access token is null in LoginResponse.")
                        Log.d("LoginRepository", "Login successful: ${loginData.userName}")
                        Result.success(loginData)
                    } ?: run {
                        Log.e("LoginRepository", "Login successful but response body is null")
                        Result.failure(Exception("Login successful but response body is null"))
                    }
                } else {
                    val errorMsg = "Login failed: ${response.code()} ${response.message()}"
                    Log.e("LoginRepository", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("LoginRepository", "Login network request failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        if(getAuthTokenOnce() == null){
            return  false
        }
        val expiryTime = getExpiry().first()
        if(expiryTime == null){
            return false
        }
        if(System.currentTimeMillis() > expiryTime){
            return false
        }
        return true
    }

    suspend fun clearAllUserData() {
        // Clear DataStore preferences
        clearAuthToken()
        clearTenantId()
        clearExpiry()
        // Clear Room database
        deleteAll()
        Log.d("LoginRepository", "All user data has been cleared.")
    }
    private suspend fun saveAuthToken(token: String, expiryTime: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN_EXPIRY] = expiryTime
        }
    }

    private fun getAuthTokenFlow(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("LoginRepository", "Error reading auth token from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.AUTH_TOKEN]
            }
    }

    private suspend fun saveTenantId(tenantId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TENANT_ID] = tenantId
        }
    }

    private fun getTenantIdFlow(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("LoginRepository", "Error reading tenant id token from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.AUTH_TENANT_ID]
            }
    }

    private fun getExpiry(): Flow<Long?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("LoginRepository", "Error reading tenant id token from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.TOKEN_EXPIRY]
            }
    }

    private suspend fun getAuthTokenOnce(): String? {
        return getAuthTokenFlow().first() // Gets the first emitted value
    }

    private suspend fun getTenantIdOnce(): String? {
        return getTenantIdFlow().first() // Gets the first emitted value
    }


    private suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
        }
        Log.d("LoginRepository", "Auth token cleared from DataStore.")
    }

    private suspend fun clearExpiry() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TOKEN_EXPIRY)
        }
        Log.d("LoginRepository", "token expiry cleared from DataStore.")
    }

    private suspend fun clearTenantId() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TENANT_ID)
        }
        Log.d("LoginRepository", "Tenant id cleared from DataStore.")
    }


    private suspend fun deleteAll() = loginDao.deleteAll()
    private suspend fun insertUserLoginInfo(loginEntity: LoginEntity) = loginDao.insert(loginEntity)
    private suspend fun updateUserLoginInfo(loginEntity: LoginEntity) = loginDao.update(loginEntity)
    private suspend fun deleteUserLoginInfo(loginEntity: LoginEntity) = loginDao.delete(loginEntity)
    private fun getUserInfo(): Flow<List<LoginEntity>> = loginDao.getUser()
}