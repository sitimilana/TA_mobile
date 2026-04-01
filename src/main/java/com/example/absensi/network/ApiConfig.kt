package com.example.absensi.network // Sesuaikan nama package

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    // SANGAT PENTING: Jika Anda pakai emulator, localhost Laravel itu alamatnya 10.0.2.2, BUKAN 127.0.0.1!
    // Ganti dengan IP komputer Anda (misal: 192.168.1.10) jika Anda menguji pakai HP asli.
    private const val BASE_URL = "http://192.168.0.9:8000/api/"

    fun getApiService(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(ApiService::class.java)
    }
}