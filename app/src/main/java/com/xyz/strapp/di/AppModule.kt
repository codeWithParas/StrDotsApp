package com.xyz.strapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.xyz.strapp.data.dao.AttendanceLogDao
import com.xyz.strapp.data.dao.FaceImageDao
import com.xyz.strapp.data.dao.LoginDao
import com.xyz.strapp.data.dao.ProfileDao
import com.xyz.strapp.data.database.AppDatabase
import com.xyz.strapp.domain.model.auth.AuthInterceptor
import com.xyz.strapp.domain.model.auth.MyTokenProvider
import com.xyz.strapp.domain.repository.AttendanceLogsRepository
import com.xyz.strapp.domain.repository.FaceLivenessRepository
import com.xyz.strapp.endpoints.ApiService
import com.xyz.strapp.presentation.components.GlobalFeedbackViewModel
import com.xyz.strapp.utils.NetworkUtils
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
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Define the DataStore name at a top-level or in a companion object for global access
private const val USER_PREFERENCES_NAME = "strapp_user_prefs"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_NAME)

val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

private const val TIMEOUT_CONNECTION: Long = 60
private const val TIMEOUT_READ: Long = 60
private const val TIMEOUT_WRITE: Long = 60

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
    fun provideTokenProvider(
        dataStore: DataStore<Preferences>
    ): MyTokenProvider {
        return MyTokenProvider(dataStore)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: MyTokenProvider
    ): AuthInterceptor {
        return AuthInterceptor(tokenProvider)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_CONNECTION, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_READ, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS)
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .followRedirects(false)
            //.cookieJar(CookieStore.get(context))
            .build()
    }

    // http://103.186.230.15:7400/api/Auth/Login
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://103.186.230.15:7401/") // E.g., "https://api.yourdomain.com/"
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For simplicity, recreate database if schema changes
        .build()
    }

    @Singleton
    @Provides
    fun provideLoginDao(appDatabase: AppDatabase): LoginDao {
        return appDatabase.loginDao()
    }

    @Singleton
    @Provides
    fun provideProfileDao(appDatabase: AppDatabase): ProfileDao {
        return appDatabase.profileDao()
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
     * Provides the DAO for attendance log operations.
     */
    @Singleton
    @Provides
    fun provideAttendanceLogDao(appDatabase: AppDatabase): AttendanceLogDao {
        return appDatabase.attendanceLogDao()
    }

    /**
     * Provides the repository for face liveness operations.
     * This repository will handle saving face images and uploading them.
     * Assumes FaceLivenessRepository takes FaceImageDao as a constructor parameter.*/

    @Singleton
    @Provides
    fun provideFaceLivenessRepository(
        faceImageDao: FaceImageDao,
        apiService: ApiService,
        networkUtils: NetworkUtils,
    ): FaceLivenessRepository {
        return FaceLivenessRepository(faceImageDao, apiService, networkUtils)// , faceApiService )
    }
    
    /**
     * Provides the repository for attendance logs operations.
     */
    @Singleton
    @Provides
    fun provideAttendanceLogsRepository(
        apiService: ApiService,
        networkUtils: NetworkUtils,
        attendanceLogDao: AttendanceLogDao
    ): AttendanceLogsRepository {
        return AttendanceLogsRepository(apiService, networkUtils, attendanceLogDao)
    }

    /**
     * Provides the repository for global status feedback.
     */
    /*@Singleton
    @Provides
    fun provideGlobalFeedbackStatus(): GlobalFeedbackViewModel {
        return GlobalFeedbackViewModel()
    }*/

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
    
    /**
     * Provides the NetworkUtils for checking network connectivity
     */
    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    /*@Provides
    @Singleton // Or appropriate scope
    fun provideCheckInWorkManager(
        @ApplicationContext context: Context,
        workerParameters: WorkerParameters,
        faceLivenessRepository: FaceLivenessRepository,
        apiService: ApiService
    ): ImageUploadWorker {
        return ImageUploadWorker(context,
            workerParameters, faceLivenessRepository, apiService
        )
    }*/

}