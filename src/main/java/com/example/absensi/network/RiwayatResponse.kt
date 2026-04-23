package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class RiwayatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<RiwayatData>?
)

data class RiwayatData(
    @SerializedName("id_absensi") val idAbsensi: Int,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("jam_masuk") val jamMasuk: String?,
    @SerializedName("jam_pulang") val jamPulang: String?,
    @SerializedName("status") val status: String
)