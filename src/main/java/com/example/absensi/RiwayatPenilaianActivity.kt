package com.example.absensi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatPenilaianAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.PenilaianResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatPenilaianActivity : AppCompatActivity() {

    private lateinit var rvRiwayatPenilaian: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_penilaian)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Inisialisasi RecyclerView dari XML
        // (Pastikan di dalam activity_riwayat_penilaian.xml Anda ada RecyclerView dengan id "rvRiwayatPenilaian")
        rvRiwayatPenilaian = findViewById(R.id.rvRiwayatKinerja)
        rvRiwayatPenilaian.layoutManager = LinearLayoutManager(this)

        ambilDataPenilaian()
    }

    // Fungsi ini diubah, tidak lagi mengambil bulan saat ini saja.
    // Jika API Laravel sudah dibuat benar, GET ini akan mereturn semua riwayat penilaian user tersebut.
    private fun ambilDataPenilaian() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        // Pastikan ApiService Anda (getPenilaian) bisa dipanggil HANYA dengan token (tanpa parameter bulan&tahun)
        // jika ingin mengambil seluruh data riwayat
        ApiConfig.getApiService().getPenilaian("Bearer $token")
            .enqueue(object : Callback<PenilaianResponse> {
                override fun onResponse(call: Call<PenilaianResponse>, response: Response<PenilaianResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        // Cek apakah data list tidak kosong
                        if (body?.success == true && body.data != null && body.data.isNotEmpty()) {
                            // Pasang adapter!
                            val adapter = RiwayatPenilaianAdapter(body.data)
                            rvRiwayatPenilaian.adapter = adapter
                        } else {
                            Toast.makeText(this@RiwayatPenilaianActivity, body?.message ?: "Belum ada riwayat penilaian", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<PenilaianResponse>, t: Throwable) {
                    Toast.makeText(this@RiwayatPenilaianActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            })
    }
}