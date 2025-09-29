package com.ecocollet.collector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProfileData(
    val userId: Long,
    val name: String,
    val username: String,
    val lastname: String,
    val phone: String,
    val email: String,
    val role: String,
    val totalRequests: Int,
    val totalWeight: Double
) : Parcelable