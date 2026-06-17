package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String?, // Token dipindah ke sini (sesuai Laravel)
    @SerializedName("data") val data: UserModel? // Langsung ke UserModel
)

data class UserModel(
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("username") val username: String?,
    @SerializedName("nama_lengkap") val namaLengkap: String?,
    @SerializedName("divisi") val divisi: String?,
    @SerializedName("tanggal_masuk") val tanggalMasuk: String?,
    @SerializedName("is_first_login") val isFirstLogin: Boolean?
)