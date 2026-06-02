package com.example.absensi.network

import com.google.gson.annotations.SerializedName

// 1. Response Utama untuk Dashboard Reward
data class DashboardRewardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DashboardData?
)

// 2. Bungkus Data Utama dari API Laravel
data class DashboardData(
    @SerializedName("is_top_performer_bulan_ini") val isTopPerformer: Boolean,
    @SerializedName("skor_bulan_ini") val skorBulanIni: Int,
    @SerializedName("chart_data") val chartData: List<ChartItem>,
    @SerializedName("reward_history") val rewardHistory: List<RewardItem>, // Menampung riwayat penilaian
    @SerializedName("recent_rewards") val recentRewards: List<RewardItem>?,
    @SerializedName("latest_recent_reward") val latestRecentReward: RewardItem?
)

// 3. Item untuk Grafik/Chart Kinerja
data class ChartItem(
    @SerializedName("label") val label: String,
    @SerializedName("skor") val skor: Int
)

// 4. Item untuk List History Reward (Disamakan dengan key buatan dari API)
data class RewardItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String?,      // Sesuai JSON: "nama": null
    @SerializedName("skor") val skor: Int,          // Sesuai JSON: "skor": 93
    @SerializedName("alasan") val alasan: String,    // Sesuai JSON: "alasan": "..."
    @SerializedName("tanggal") val tanggal: String,  // Sesuai JSON: "tanggal": "2026-04-01"
    @SerializedName("bulan") val bulan: Int,
    @SerializedName("tahun") val tahun: Int
)