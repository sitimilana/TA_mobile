package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class SisaCutiResponse(
    val success: Boolean,
    val message: String,
    val data: SisaCutiData?
)

data class SisaCutiData(
    @SerializedName("nama")
    val nama: String? = null,

    @SerializedName("total_cuti_tahun_ini")
    val totalCutiTahunIni: Int,

    @SerializedName("cuti_digunakan")
    val cutiDigunakan: Int,

    @SerializedName("sisa_cuti_tahun_ini")
    val sisaCutiTahunIni: Int,

    @SerializedName("cuti_pending")
    val cutiPending: Int,

    @SerializedName("cuti_approved")
    val cutiApproved: Int,
    
    @SerializedName("sisa_cuti_bulan_ini")
    val sisaCutiBulanIni: Int? = 1  // Default 1 jika tidak ada pengajuan bulan ini
)

