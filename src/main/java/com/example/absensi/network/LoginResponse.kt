package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DataPayload? = null
)

data class DataPayload(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserModel?
)

data class UserModel(
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("username") val username: String,
    @SerializedName("nama_lengkap") val namaLengkap: String
)