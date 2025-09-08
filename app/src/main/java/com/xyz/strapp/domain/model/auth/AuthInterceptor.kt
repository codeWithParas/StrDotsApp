package com.xyz.strapp.domain.model.auth

import android.preference.PreferenceDataStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.xyz.strapp.domain.repository.LoginRepository.PreferencesKeys
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// Assume you have a way to get the token, e.g., from SharedPreferences, DataStore, or an AuthManager
// This is a placeholder; you'll need to implement token retrieval.
interface TokenProvider {
    fun getToken(): String?
}

// Example implementation (replace with your actual token storage)
@Singleton
class MyTokenProvider @Inject constructor(
    // private val loginRepository: LoginRepository, // Assuming this is not needed if directly accessing DataStore for token
    private val dataStore: DataStore<Preferences>
) : TokenProvider {
    override fun getToken(): String? {
        // Use runBlocking to synchronously get the token from DataStore.
        // This will block the thread until the DataStore operation is complete.
        // Use with caution in performance-sensitive contexts like OkHttp interceptors.
        return runBlocking {
            dataStore.data
                .catch { exception ->
                    // dataStore.data throws an IOException when an error is encountered when reading data
                    if (exception is IOException) {
                        Log.e("MyTokenProvider", "Error reading auth token from DataStore.", exception)
                        emit(emptyPreferences()) // Emit empty preferences to allow firstOrNull to return null
                    } else {
                        throw exception // Rethrow other exceptions
                    }
                }
                .map { preferences ->
                    preferences[PreferencesKeys.AUTH_TOKEN] // PreferencesKeys.AUTH_TOKEN should be accessible here
                }
                .firstOrNull() // Get the first emitted value or null
        }
    }
}

class AuthInterceptor @Inject constructor(
    private val tokenProvider: MyTokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider.getToken()

        // Add the Authorization header if a token exists
        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            // Common practice is "Bearer <token>"
            requestBuilder.header("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "Token added to request: Bearer $token")
        } else {
            Log.d("AuthInterceptor", "No token found to add to request.")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
