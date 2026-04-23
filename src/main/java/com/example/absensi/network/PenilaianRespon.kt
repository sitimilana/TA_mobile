package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class PenilaianResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PenilaianData?
)

data class PenilaianData(
    @SerializedName("disiplin") val disiplin: Int?,
    @SerializedName("produktivitas") val produktivitas: Int?,
    @SerializedName("tanggung_jawab") val tanggungJawab: Int?,
    @SerializedName("sikap_kerja") val sikapKerja: Int?,
    @SerializedName("loyalitas") val loyalitas: Int?,
    @SerializedName("total_skor") val totalSkor: Int?,
    @SerializedName("bulan_tahun") val bulanTahun: String?
)