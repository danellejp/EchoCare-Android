package com.echocare.app.data.api

import com.echocare.app.data.model.RecentEventsResponse
import com.echocare.app.data.model.StatisticsResponse
import com.echocare.app.data.model.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST

/**
 * Retrofit API interface for EchoCare Raspberry Pi backend
 * Base URL: http://192.168.4.1:8000
 *
 * Endpoints:
 * GET /status          — Pi connection status and system info
 * GET /recent-events   — List of recent cry events from SQLite
 * GET /statistics       — Aggregated cry statistics for dashboard
 *
 */
interface EchoCareApiService {

    /**
     * Check Pi connection status.
     *
     * GET /status
     * Returns: StatusResponse with online/offline status, total events, pi info.
     */
    @GET("status")
    suspend fun getStatus(): Response<StatusResponse>

    /**
     * Get recent cry events from the Pi's database.
     *
     * GET /recent-events?limit=50
     *
     * @param limit Max number of events to return (1-50, default 50).
     * @return RecentEventsResponse containing list of CryEvent objects.
     *
     * Note: The Pi returns events ordered by timestamp DESC (most recent first).
     * Filtering by time range and cry type is done client-side since
     * the Pi's endpoint doesn't support those query params natively.
     */
    @GET("recent-events")
    suspend fun getRecentEvents(
        @Query("limit") limit: Int = 50
    ): Response<RecentEventsResponse>

    /**
     * Get aggregated cry statistics.
     *
     * GET /statistics?hours=24
     *
     * @param hours Time window in hours (1-168, default 24).
     * @return StatisticsResponse with totals, breakdown by type, avg confidence.
     */
    @GET("statistics")
    suspend fun getStatistics(
        @Query("hours") hours: Int = 24
    ): Response<StatisticsResponse>

    @POST("/set-time")
    suspend fun setTime(@Body body: Map<String, String>): Response<Map<String, String>>

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