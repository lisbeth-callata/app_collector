package com.ecocollet.collector.network

import com.ecocollet.collector.model.AssignmentRequest
import com.ecocollet.collector.model.AssignmentResponse
import com.ecocollet.collector.model.CollectionRequest
import retrofit2.Call
import retrofit2.http.*

interface AssignmentService {

    @POST("assignments/claim/{requestId}")
    fun claimRequest(
        @Path("requestId") requestId: Long,
        @Body assignmentRequest: AssignmentRequest
    ): Call<AssignmentResponse>

    @POST("assignments/release/{requestId}")
    fun releaseRequest(@Path("requestId") requestId: Long): Call<AssignmentResponse>

    @POST("assignments/complete/{requestId}")
    fun completeRequest(@Path("requestId") requestId: Long): Call<AssignmentResponse>

    @GET("assignments/collector/{collectorId}")
    fun getCollectorAssignments(@Path("collectorId") collectorId: Long): Call<List<CollectionRequest>>

    @GET("assignments/available")
    fun getAvailableRequests(): Call<List<CollectionRequest>>

    @PUT("assignments/{requestId}/status/{status}")
    fun updateAssignmentStatus(
        @Path("requestId") requestId: Long,
        @Path("status") status: String
    ): Call<CollectionRequest>
}