package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class CutiResponse(
    @SerializedName("karyawan") val karyawan: String?,
    @SerializedName("sisa_cuti") val sisaCuti: Int?,
    @SerializedName("data") val data: List<CutiItem>?
)

data class CutiItem(
    @SerializedName("id_cuti") val idCuti: Int?,
    @SerializedName("tanggal_mulai") val tanggalMulai: String?,
    @SerializedName("tanggal_selesai") val tanggalSelesai: String?,
    @SerializedName("jenis_cuti") val jenisCuti: String?,
    @SerializedName("alasan") val alasan: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("tanggal_pengajuan") val tanggalPengajuan: String?
)