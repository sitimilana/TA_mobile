package com.example.absensi.network

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.absensi.network.SisaCutiResponse

interface ApiService {
    @GET("config-presensi")
    fun getConfigPresensi(
        @Header("Authorization") token: String
    ): Call<ConfigResponse>

    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @Multipart
    @POST("absensi")
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
    @POST("cuti")
    fun submitCuti(
        @Header("Authorization") token: String,
        @Part("tanggal_mulai") tglMulai: RequestBody,
        @Part("tanggal_selesai") tglSelesai: RequestBody,
        @Part("jenis_cuti") jenisCuti: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part berkasBukti: MultipartBody.Part?
    ): Call<LoginResponse>

    @Multipart
    @POST("cuti/{id}")
    fun updateCuti(
        @Header("Authorization") token: String,
        @Path("id") idCuti: Int,
        @Part("tanggal_mulai") tglMulai: RequestBody,
        @Part("tanggal_selesai") tglSelesai: RequestBody,
        @Part("jenis_cuti") jenisCuti: RequestBody,
        @Part("alasan") alasan: RequestBody,
        @Part berkasBukti: MultipartBody.Part?
    ): Call<ApiMessageResponse>

    @DELETE("cuti/{id}")
    fun deleteCuti(
        @Header("Authorization") token: String,
        @Path("id") idCuti: Int
    ): Call<ApiMessageResponse>

    @GET("cuti")
    fun getRiwayatCuti(@Header("Authorization") token: String): Call<CutiResponse>

    @GET("penilaian")
    fun getPenilaian(
        @Header("Authorization") token: String
    ): Call<PenilaianResponse>

    @GET("rewards")
    fun getRewards(
        @Header("Authorization") token: String
    ): Call<DashboardRewardResponse>

    @GET("penilaian/dashboard")
    fun getDashboardReward(
        @Header("Authorization") token: String
    ): Call<DashboardRewardResponse>

    @GET("gaji")
    fun getSlipGaji(
        @Header("Authorization") token: String
    ): Call<SalaryResponse>

    @GET("gaji/{id}")
    fun getDetailSlipGaji(
        @Header("Authorization") token: String,
        @Path("id") idGaji: Int
    ): Call<SalaryDetailResponse>

    @GET("cuti/sisa")
    fun getSisaCuti(
        @Header("Authorization") token: String
    ): Call<SisaCutiResponse>

    @Multipart
    @POST("update-foto")
    suspend fun updateFoto(@Part foto: MultipartBody.Part): ApiMessageResponse

    @GET("profil")
    fun getProfil(@Header("Authorization") token: String): Call<ProfilResponse>

    @FormUrlEncoded
    @POST("change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Field("password_lama") passwordLama: String,
        @Field("password_baru") passwordBaru: String,
        @Field("konfirmasi_password") konfirmasiPassword: String
    ): Call<ApiMessageResponse>

    @Multipart
    @POST("update-foto")
    fun updateFoto(
        @Header("Authorization") token: String,
        @Part foto: MultipartBody.Part
    ): Call<ApiMessageResponse>
}