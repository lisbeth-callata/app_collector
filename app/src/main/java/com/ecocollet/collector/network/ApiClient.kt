package com.ecocollet.collector.network

import android.content.Context
import com.ecocollet.collector.utils.AuthManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://ec-backend-drg8.onrender.com/api/"
        private var instance: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return instance ?: synchronized(this) {
                instance ?: ApiClient(context.applicationContext).also { instance = it }
            }
        }
    }

    private val authManager by lazy { AuthManager(context) }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                authManager.getToken()?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                requestBuilder.header("Content-Type", "application/json")
                requestBuilder.header("Accept", "application/json")

                val newRequest = requestBuilder.build()
                chain.proceed(newRequest)
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getAuthService(): AuthService = retrofit.create(AuthService::class.java)
    fun getCollectorService(): CollectorService = retrofit.create(CollectorService::class.java)
    fun getAssignmentService(): AssignmentService = retrofit.create(AssignmentService::class.java)
    fun getRealTimeService(): RealTimeService = retrofit.create(RealTimeService::class.java)
}