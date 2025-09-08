package com.xyz.strapp.domain.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject


class LanguageRepository @Inject constructor(
    private val dataStore: DataStore<Preferences> // Injected DataStore
) {

    private object PreferencesKeys {
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

//    suspend fun loginUserRemote(language: String) {
//        return withContext(Dispatchers.IO) { // Switch to IO for network call
//            saveLanguage(language)
//        }
//    }

    suspend fun saveLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = language
        }
    }

    suspend fun getSelectedLanguage(): String? {
        return getSelectedLanguageRecord().first() // Gets the first emitted value
    }

   private fun getSelectedLanguageRecord(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                if (exception is IOException) {
                    Log.e("LanguageRepository", "Error reading selected language from DataStore.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.SELECTED_LANGUAGE]
            }
    }

   private suspend fun isLanguageSelected(): Boolean {
        return getSelectedLanguage() != null
    }

}