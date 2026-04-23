package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class RewardResponse(
    @field:SerializedName("success")
    val success: Boolean,
    @field:SerializedName("message")
    val message: String,
    @field:SerializedName("data")
    val data: List<RewardItem>
)

data class RewardItem(
    @field:SerializedName("id_reward")
    val id: Int,
    @field:SerializedName("nama_karyawan")
    val nama: String,
    @field:SerializedName("tanggal")
    val tanggal: String,
    @field:SerializedName("jenis_reward")
    val jenis: String,
    @field:SerializedName("alasan")
    val alasan: String,
    @field:SerializedName("skor_kinerja")
    val skor: Int,
    @field:SerializedName("jumlah_alpha")
    val alpha: Int
)