package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class ApiMessageResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("success") val success: Boolean? = null
)

