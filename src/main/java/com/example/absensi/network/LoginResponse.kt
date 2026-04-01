package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DataPayload? = null
)

data class DataPayload(
    @SerializedName("token") val token: String
)