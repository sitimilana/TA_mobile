package com.example.absensi.network
//import login
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
// Import untuk fitur Absensi Kamera (Multipart)
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @GET("config-presensi")
    fun getConfigPresensi(): Call<ConfigResponse>

    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @Multipart
    @POST("absensi") // Pastikan URL ini sama dengan route di Laravel Anda
    fun submitAbsensi(
        @Part("id_user") idUser: RequestBody,
        @Part("jenis") jenis: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part foto: MultipartBody.Part
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("absensi/riwayat")
    fun getRiwayatAbsensi(@Field("id_user") idUser: String): Call<RiwayatResponse>

    @Multipart
    @POST("cuti") // <--- Cukup "cuti" saja
    fun submitCuti(
        @Header("Authorization") token: String,
        @Part("tanggal_mulai") tglMulai: RequestBody,
        @Part("tanggal_selesai") tglSelesai: RequestBody,
        @Part("jenis_cuti") jenisCuti: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part berkasBukti: MultipartBody.Part?
    ): Call<LoginResponse>

    @GET("cuti")
    fun getRiwayatCuti(@Header("Authorization") token: String): Call<CutiResponse>


    @GET("penilaian")
    fun getPenilaian(
        @Header("Authorization") token: String,
        @Query("bulan") bulan: Int,
        @Query("tahun") tahun: Int
    ): Call<PenilaianResponse>

    @GET("rewards") // Sesuaikan dengan endpoint di Laravel
    fun getRewards(
        @Header("Authorization") token: String
    ): Call<RewardResponse>

    @GET("gaji")
    fun getSalarySlips(@Header("Authorization") token: String): Call<SalaryResponse>
}
