package com.ecocollet.collector.network

import com.ecocollet.collector.model.AuthResponse
import com.ecocollet.collector.model.LoginRequest
import com.ecocollet.collector.model.ProfileData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthService {
    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Call<AuthResponse>

    @GET("users/{userId}/profile")
    fun getProfile(@Path("userId") userId: Long): Call<ProfileData>

    @GET("users/me/profile")
    fun getMyProfile(): Call<ProfileData>
}