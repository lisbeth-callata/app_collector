package com.ecocollet.collector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Route(
    val id: Long,
    val name: String,
    val requests: List<CollectionRequest>,
    val totalDistance: Double,
    val estimatedTime: Double,
    val optimizedOrder: List<Long>
) : Parcelable

@Parcelize
data class RouteStep(
    val order: Int,
    val request: CollectionRequest,
    val distanceFromPrevious: Double,
    val estimatedTime: Double
) : Parcelable