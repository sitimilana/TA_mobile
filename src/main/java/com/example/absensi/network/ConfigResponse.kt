package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class ConfigResponse(
    val success: Boolean,
    val message: String,
    val data: ConfigData?
)

data class ConfigData(
    @SerializedName("office_lat")
    val officeLat: Double,

    @SerializedName("office_lon")
    val officeLon: Double,

    @SerializedName("max_radius")
    val maxRadius: Double,

    // TAMBAHKAN DUA BARIS INI
    @SerializedName("is_libur")
    val isLibur: Boolean?,

    @SerializedName("pesan_libur")
    val pesanLibur: String?
)