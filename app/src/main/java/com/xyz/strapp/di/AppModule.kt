package com.xyz.strapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.xyz.strapp.data.dao.FaceImageDao
import com.xyz.strapp.data.dao.LoginDao
import com.xyz.strapp.data.database.AppDatabase
import com.xyz.strapp.domain.repository.FaceLivenessRepository
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.endpoints.ApiService
import com.xyz.strapp.utils.Utils.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Define the DataStore name at a top-level or in a companion object for global access
private const val USER_PREFERENCES_NAME = "strapp_user_prefs"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore // Use the extension property
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
            })
            // Add other interceptors like for Auth tokens here
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // http://103.186.230.15:7400/api/Auth/Login
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://103.186.230.15:7400/") // E.g., "https://api.yourdomain.com/"
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }



    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideLoginDao(appDatabase: AppDatabase): LoginDao {
        return appDatabase.loginDao()
    }

    /*@Singleton
    @Provides
    fun provideLoginRepository(loginDao: LoginDao): LoginRepository {
        return LoginRepository(
            loginDao,
            provideAuthApiService(
                provideRetrofit(provideOkHttpClient())
            ),
            providePreferencesDataStore(context = this)
        )
    }*/


    // --- New Providers for Face Liveness Feature ---

    /**
     * Provides the DAO for face image operations.
     * Assumes AppDatabase has a method like: abstract fun faceImageDao(): FaceImageDao
     */
    @Singleton // DAOs can be singletons if their database is a singleton
    @Provides
    fun provideFaceImageDao(appDatabase: AppDatabase): FaceImageDao { // Use the fully qualified name or import
        return appDatabase.faceImageDao()
    }

    /**
     * Provides the repository for face liveness operations.
     * This repository will handle saving face images and uploading them.
     * Assumes FaceLivenessRepository takes FaceImageDao as a constructor parameter.*/

    @Singleton // Repositories are often singletons
    @Provides
    fun provideFaceLivenessRepository(
        faceImageDao: FaceImageDao // Use the fully qualified name or import
        // Add other dependencies if needed, e.g., a Retrofit API service for uploads
        // , faceApiService: FaceApiService
    ): FaceLivenessRepository { // Use the fully qualified name or import
        return FaceLivenessRepository(faceImageDao)// , faceApiService )
    }

}