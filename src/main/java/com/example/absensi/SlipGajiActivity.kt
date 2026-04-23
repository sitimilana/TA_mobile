package com.example.absensi

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.SlipGajiAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.SalaryItem
import com.example.absensi.network.SalaryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SlipGajiActivity : AppCompatActivity() {

    private lateinit var rvSalary: RecyclerView
    private lateinit var adapter: SlipGajiAdapter
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slip_gaji)

        // 1. Hubungkan Bottom Navigation
        NavigationUtils.setupBottomNav(this)

        // 2. Setup Header
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)
        tvWelcomeName.text = "Selamat Datang, ${sharedPref.getString("NAMA_LENGKAP", "User")}"
        tvRole.text = "Karyawan"

        // 3. Setup RecyclerView
        rvSalary = findViewById(R.id.rvSalarySlips)
        rvSalary.layoutManager = LinearLayoutManager(this)

        adapter = SlipGajiAdapter(emptyList()) { salaryItem ->
            // Alur Diagram: "Menampilkan komponen gaji" (Gaji Pokok, Bonus, Potongan, dll)
            // Di sini Anda bisa menampilkan rincian dalam bentuk Dialog atau pindah ke Activity Detail
            showSalaryDetailDialog(salaryItem)
        }
        rvSalary.adapter = adapter

        fetchSalaryData()
    }

    private fun fetchSalaryData() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getSalarySlips(token).enqueue(object : Callback<SalaryResponse> {
            override fun onResponse(call: Call<SalaryResponse>, response: Response<SalaryResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data ?: emptyList()
                    adapter.updateData(data)
                } else {
                    Toast.makeText(this@SlipGajiActivity, "Gagal memuat data slip gaji", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SalaryResponse>, t: Throwable) {
                Toast.makeText(this@SlipGajiActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showSalaryDetailDialog(item: SalaryItem) {
        // Alur: Menampilkan rincian (Gaji Pokok, Uang Makan, dll)
        // Untuk implementasi sederhana, kita gunakan Toast rincian
        val detailMsg = "Rincian Gaji ${item.tanggal}:\n" +
                "Pokok: Rp ${item.gajiPokok}\n" +
                "Makan: Rp ${item.uangMakan}\n" +
                "Potongan: Rp ${item.potongan}"
        Toast.makeText(this, detailMsg, Toast.LENGTH_LONG).show()
    }
}
