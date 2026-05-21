package com.example.absensi.network

import com.google.gson.annotations.SerializedName

// RESPONSE UNTUK LIST REWARD (Standar)
data class RewardResponse(
    @field:SerializedName("success")
    val success: Boolean,
    @field:SerializedName("message")
    val message: String,
    @field:SerializedName("data")
    val data: List<RewardItem>
)

data class RewardItem(
    @field:SerializedName("id_reward") val id: Int,
    @field:SerializedName("nama_karyawan") val nama: String?, // Tambahkan safe call ?
    @field:SerializedName("tanggal_reward") val tanggal: String, // Sesuaikan dengan kolom DB
    @field:SerializedName("keterangan") val alasan: String, // Kita mapping keterangan sebagai alasan
    @field:SerializedName("total_skor") val skor: Int // Diambil dari relasi penilaian
)
data class DashboardRewardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DashboardRewardData?
)

data class DashboardRewardData(
    @SerializedName("is_top_performer_bulan_ini") val isTopPerformer: Boolean,
    @SerializedName("skor_bulan_ini") val skorBulanIni: Int,
    @SerializedName("chart_data") val chartData: List<ChartItem>
)

data class ChartItem(
    @SerializedName("label") val label: String,
    @SerializedName("skor") val skor: Int
)