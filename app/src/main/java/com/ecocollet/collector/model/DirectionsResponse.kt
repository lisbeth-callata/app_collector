package com.ecocollet.collector.model

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<DirectionsRoute>,
    @SerializedName("status") val status: String
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline,
    @SerializedName("legs") val legs: List<Leg>
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Leg(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration,
    @SerializedName("steps") val steps: List<Step>
)

data class Distance(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

data class Duration(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

data class Step(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration,
    @SerializedName("html_instructions") val htmlInstructions: String,
    @SerializedName("maneuver") val maneuver: String?,
    @SerializedName("polyline") val polyline: StepPolyline,
    @SerializedName("travel_mode") val travelMode: String
)

data class StepPolyline(
    @SerializedName("points") val points: String
)