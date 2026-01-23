package com.echocare.app.data.api

import com.echocare.app.data.model.RecentEventsResponse
import com.echocare.app.data.model.StatisticsResponse
import com.echocare.app.data.model.StatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for EchoCare Raspberry Pi backend
 * Base URL: http://192.168.4.1:8000
 */
interface EchoCareApiService {

    /**
     * Get Pi connection status and basic info
     * GET /status
     */
    @GET("status")
    suspend fun getStatus(): Response<StatusResponse>

    /**
     * Get recent cry events
     * GET /recent-events?limit=20&hours=24
     *
     * @param limit Number of events to return (default: 20)
     * @param hours Time window in hours (default: 24)
     */
    @GET("recent-events")
    suspend fun getRecentEvents(
        @Query("limit") limit: Int = 20,
        @Query("hours") hours: Int = 24
    ): Response<RecentEventsResponse>

    /**
     * Get cry statistics for dashboard visualization
     * GET /statistics?hours=24
     *
     * @param hours Time window in hours (default: 24, 168=week)
     */
    @GET("statistics")
    suspend fun getStatistics(
        @Query("hours") hours: Int = 24
    ): Response<StatisticsResponse>

    /**
     * Get filtered events by cry type
     * GET /recent-events?cry_type=hungry&limit=50
     *
     * @param cryType Filter by type: "hungry", "pain", "normal", or null for all
     * @param limit Number of events to return
     * @param hours Time window in hours
     */
    @GET("recent-events")
    suspend fun getEventsByType(
        @Query("cry_type") cryType: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("hours") hours: Int = 24
    ): Response<RecentEventsResponse>
}

/**
 * Singleton object to create and manage Retrofit instance
 */
object RetrofitClient {
    private const val BASE_URL = "http://192.168.4.1:8000/"

    private val retrofit by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .build()
    }

    val apiService: EchoCareApiService by lazy {
        retrofit.create(EchoCareApiService::class.java)
    }
}