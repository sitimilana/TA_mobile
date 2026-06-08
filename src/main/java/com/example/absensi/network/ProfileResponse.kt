package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class ProfilResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: KaryawanData
)

data class KaryawanData(
    @SerializedName("nama_lengkap") val namaLengkap: String,
    @SerializedName("divisi") val divisi: String,
    @SerializedName("email") val email: String,
    @SerializedName("no_hp") val noHp: String,
    @SerializedName("alamat") val alamat: String,
    @SerializedName("tanggal_masuk") val tanggalMasuk: String,
    @SerializedName("foto") val foto: String?
)