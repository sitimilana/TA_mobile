package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class ConfigResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: PresensiConfigData? = null
)

data class PresensiConfigData(
    @SerializedName(value = "office_lat", alternate = ["latitude", "lat"]) val officeLat: Double? = null,
    @SerializedName(value = "office_lon", alternate = ["longitude", "lon"]) val officeLon: Double? = null,
    @SerializedName(value = "max_radius", alternate = ["radius", "radius_meter"]) val maxRadius: Double? = null
)
