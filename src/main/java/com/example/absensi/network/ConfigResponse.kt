package com.example.absensi.network


data class ConfigResponse(
    val success: Boolean,
    val message: String,
    val data: ConfigData?
)

data class ConfigData(
    val officeLat: Double,
    val officeLon: Double,
    val maxRadius: Double
)
