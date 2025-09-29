package com.ecocollet.collector.model

data class AuthResponse(
    val token: String,
    val email: String,
    val name: String,
    val username: String,
    val phone: String,
    val lastname: String,
    val role: String,
    val userId: Long
)