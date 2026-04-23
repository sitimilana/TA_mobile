package com.example.absensi

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.PenilaianResponse
import com.google.android.material.progressindicator.LinearProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RiwayatPenilaianActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_penilaian) // Ganti sesuai nama file XML Penilaian Anda
        NavigationUtils.setupBottomNav(this)

        // TODO: Anda harus mensetup Spinner Bulan & Tahun di sini.
        // Untuk contoh ini, kita pakai bulan saat ini saja.
        val cal = Calendar.getInstance()
        val bulanSekarang = cal.get(Calendar.MONTH) + 1
        val tahunSekarang = cal.get(Calendar.YEAR)

        ambilDataPenilaian(bulanSekarang, tahunSekarang)
    }

    private fun ambilDataPenilaian(bulan: Int, tahun: Int) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        ApiConfig.getApiService().getPenilaian("Bearer $token", bulan, tahun)
            .enqueue(object : Callback<PenilaianResponse> {
                override fun onResponse(call: Call<PenilaianResponse>, response: Response<PenilaianResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true && body.data != null) {
                            updateUI(body.data)
                        } else {
                            Toast.makeText(this@RiwayatPenilaianActivity, body?.message ?: "Belum ada penilaian", Toast.LENGTH_SHORT).show()
                            // Set nilai 0 jika belum ada
                            resetUI()
                        }
                    }
                }

                override fun onFailure(call: Call<PenilaianResponse>, t: Throwable) {
                    Toast.makeText(this@RiwayatPenilaianActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(data: com.example.absensi.network.PenilaianData) {
        // Cari TextView & ProgressIndicator berdasarkan XML Anda (Anda harus menambahkan ID ke nilai skor di XML)
        // Contoh untuk Disiplin (Asumsi Anda sudah menambah ID tvSkorDisiplin & pbDisiplin di XML):
        /*
        findViewById<TextView>(R.id.tvSkorDisiplin).text = "${data.disiplin}/10"
        findViewById<LinearProgressIndicator>(R.id.pbDisiplin).progress = (data.disiplin ?: 0) * 10

        findViewById<TextView>(R.id.tvTotalSkor).text = data.totalSkor.toString()
        */
    }

    private fun resetUI() {
        // Set semua teks skor ke "0/10" dan progress ke 0
    }
}