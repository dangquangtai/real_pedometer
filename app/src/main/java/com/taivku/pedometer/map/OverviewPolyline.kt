package com.taivku.pedometer.map

import com.google.gson.annotations.SerializedName

data class OverviewPolyline(
        @SerializedName("points")
        var points: String?
)