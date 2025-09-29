package com.ecocollet.collector.network

import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.model.AssignmentResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RealTimeService {
    @GET("assignments/available")
    fun getAvailableRequests(): Call<List<CollectionRequest>>

    @GET("assignments/collector/{collectorId}")
    fun getMyAssignments(@Path("collectorId") collectorId: Long): Call<List<CollectionRequest>>

    @POST("assignments/claim/{requestId}")
    fun claimRequest(@Path("requestId") requestId: Long,
                     @Body claimRequest: ClaimRequest): Call<AssignmentResponse>

    @POST("assignments/release/{requestId}")
    fun releaseRequest(@Path("requestId") requestId: Long): Call<AssignmentResponse>

    @POST("assignments/complete/{requestId}")
    fun completeRequest(@Path("requestId") requestId: Long): Call<AssignmentResponse>
}

data class ClaimRequest(
    val collectorId: Long,
    val collectorName: String,
    val timeoutMinutes: Int = 30
)