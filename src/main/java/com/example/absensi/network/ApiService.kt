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
import retrofit2.http.Multipart
import retrofit2.http.Part

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
}
