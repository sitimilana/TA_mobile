package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class SalaryResponse(
    val success: Boolean,
    val message: String,
    val data: List<SalaryItem>
)

data class SalaryItem(
    val id: Int,
    val tanggal: String,
    @SerializedName("gaji_pokok") val gajiPokok: Long,
    @SerializedName("uang_makan") val uangMakan: Long,
    val leader: Long,
    val kinerja: Long,
    val program: Long,
    val bpjs: Long,
    val bonus: Long,
    @SerializedName("lain_lain") val lainLain: Long,
    @SerializedName("total_pendapatan") val totalPendapatan: Long,
    val potongan: Long,
    @SerializedName("gaji_bersih") val gajiBersih: Long,
    val status: String
)