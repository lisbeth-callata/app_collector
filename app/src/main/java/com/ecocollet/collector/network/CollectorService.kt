package com.ecocollet.collector.network

import com.ecocollet.collector.model.CollectionRequest
import com.ecocollet.collector.model.UpdateRequestResponse
import retrofit2.Call
import retrofit2.http.*

interface CollectorService {

    @GET("requests/today")
    fun getTodayRequests(): Call<List<CollectionRequest>>

    @PATCH("collector/requests/{requestId}")
    fun updateRequest(
        @Path("requestId") requestId: Long,
        @Query("weight") weight: Double?,
        @Query("status") status: String?
    ): Call<UpdateRequestResponse>

    @DELETE("requests/{id}")
    fun deleteRequest(@Path("id") id: Long): Call<Void>

    @GET("requests")
    fun getAllRequests(): Call<List<CollectionRequest>>

    @GET("collector/stats")
    fun getCollectorStats(): Call<Map<String, Int>>

    @GET("requests/available")
    fun getAvailableRequests(): Call<List<CollectionRequest>>

    @GET("assignments/collector/{collectorId}")
    fun getMyAssignments(@Path("collectorId") collectorId: Long): Call<List<CollectionRequest>>

    @GET("requests/search")
    fun searchRequests(@Query("term") searchTerm: String): Call<List<CollectionRequest>>
}