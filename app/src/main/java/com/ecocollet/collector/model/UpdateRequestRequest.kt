package com.ecocollet.collector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdateRequestRequest(
    val weight: Double?,
    val status: String?,
    val notes: String? = null,
    val assignmentStatus: String? = null
) : Parcelable

@Parcelize
data class UpdateRequestResponse(
    val id: Long,
    val code: String,
    val material: String,
    val description: String?,
    val status: String,
    val weight: Double?,
    val updatedAt: String,
    val message: String? = null
) : Parcelable