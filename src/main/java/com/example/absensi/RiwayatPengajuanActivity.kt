package com.example.absensi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatPengajuanAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.CutiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatPengajuanActivity : AppCompatActivity() {

    private lateinit var rvRiwayatCuti: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_pengajuan)
        NavigationUtils.setupBottomNav(this)

        rvRiwayatCuti = findViewById(R.id.rvRiwayatCuti)
        ambilDataCuti()
    }

    private fun ambilDataCuti() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        ApiConfig.getApiService().getRiwayatCuti("Bearer $token").enqueue(object : Callback<CutiResponse> {
            override fun onResponse(call: Call<CutiResponse>, response: Response<CutiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val dataCuti = response.body()?.data ?: emptyList()
                    val adapter = RiwayatPengajuanAdapter(dataCuti)
                    rvRiwayatCuti.adapter = adapter
                } else {
                    Toast.makeText(this@RiwayatPengajuanActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CutiResponse>, t: Throwable) {
                Toast.makeText(this@RiwayatPengajuanActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}