package com.example.absensi.network

import com.google.gson.annotations.SerializedName

data class SalaryResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<SalaryData>? // Harus List karena riwayatnya banyak
)

data class SalaryData(
    @SerializedName("id_gaji") val idGaji: Int?,
    @SerializedName("bulan") val bulan: Int?,
    @SerializedName("tahun") val tahun: Int?,
    @SerializedName("periode") val periode: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("status_slip") val statusSlip: String?,
    @SerializedName("total_gaji") val totalGaji: Int?,
    @SerializedName("tanggal_dibuat") val tanggalDibuat: String?
)
data class SalaryDetailResponse(
    @SerializedName("id_gaji") val idGaji: Int?,
    @SerializedName("karyawan") val karyawan: String?,
    @SerializedName("jabatan") val jabatan: String?,
    @SerializedName("periode") val periode: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("total_gaji") val totalGaji: Int?,
    @SerializedName("penerimaan") val penerimaan: PenerimaanData?,
    @SerializedName("potongan") val potongan: PotonganData?,
    @SerializedName("tanggal_dibuat") val tanggalDibuat: String?
)

data class PenerimaanData(
    @SerializedName("gaji_pokok") val gajiPokok: Int?,
    @SerializedName("uang_makan") val uangMakan: Int?,
    @SerializedName("tunjangan_jabatan") val tunjanganJabatan: Int?,
    @SerializedName("insentif_kinerja") val insentifKinerja: Int?,
    @SerializedName("tunjangan_program") val tunjanganProgram: Int?,
    @SerializedName("tunjangan_bpjs") val tunjanganBpjs: Int?,
    @SerializedName("bonus") val bonus: Int?,
    @SerializedName("lain_lain") val lainLain: Int?,
    @SerializedName("total_penerimaan") val totalPenerimaan: Int?
)

data class PotonganData(
    @SerializedName("potongan_absen") val potonganAbsen: Int?,
    @SerializedName("cash_bon") val cashBon: Int?,
    @SerializedName("cash_bon_2") val cashBon2: Int?,
    @SerializedName("potongan_bpjs") val potonganBpjs: Int?,
    @SerializedName("potongan_lain") val potonganLain: Int?
)