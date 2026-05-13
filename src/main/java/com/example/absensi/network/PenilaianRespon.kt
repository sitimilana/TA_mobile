package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class PenilaianResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    // PERUBAHAN KRUSIAL: data sekarang berupa List/Array
    @SerializedName("data") val data: List<PenilaianData>?
)

data class PenilaianData(
    @SerializedName("disiplin") val disiplin: Int?,
    @SerializedName("produktivitas") val produktivitas: Int?,
    @SerializedName("tanggung_jawab") val tanggungJawab: Int?,
    @SerializedName("sikap_kerja") val sikapKerja: Int?,
    @SerializedName("loyalitas") val loyalitas: Int?,
    @SerializedName("total_skor") val totalSkor: Int?,
    // PERUBAHAN: Menyesuaikan properti bulan dan tahun (Seperti dari DB)
    @SerializedName("bulan") val bulan: Int?,
    @SerializedName("tahun") val tahun: Int?
)