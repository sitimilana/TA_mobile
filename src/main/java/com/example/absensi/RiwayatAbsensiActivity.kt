package com.example.absensi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatAbsensiAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RiwayatData
import com.example.absensi.network.RiwayatResponse
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatAbsensiActivity : AppCompatActivity() {

    private lateinit var rvTable: RecyclerView
    private lateinit var adapter: RiwayatAbsensiAdapter
    private var listFull = mutableListOf<RiwayatData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_absensi)

        // 1. Setup Header & Navigasi
        setupHeader()
        NavigationUtils.setupBottomNav(this)

        // 2. Inisialisasi RecyclerView
        rvTable = findViewById(R.id.rvTableAbsensi)
        rvTable.layoutManager = LinearLayoutManager(this)
        adapter = RiwayatAbsensiAdapter(listFull)
        rvTable.adapter = adapter

        // 3. Setup Fitur Search
        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterSearch(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchRiwayatAbsensi()
    }

    private fun setupHeader() {
        val tvWelcomeName = findViewById<TextView>(R.id.tvWelcomeName)
        val tvRole = findViewById<TextView>(R.id.tvRole)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        tvWelcomeName.text = "Selamat Datang, ${sharedPref.getString("NAMA_LENGKAP", "Karyawan")}"
        tvRole.text = sharedPref.getString("DIVISI", "Belum ada divisi")
    }

    private fun fetchRiwayatAbsensi() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val idUser = sharedPref.getString("ID_USER", "") ?: ""

        ApiConfig.getApiService().getRiwayatAbsensi(idUser)
            .enqueue(object : Callback<RiwayatResponse> {
                override fun onResponse(call: Call<RiwayatResponse>, response: Response<RiwayatResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data ?: emptyList()
                        listFull.clear()
                        listFull.addAll(data)
                        adapter.updateData(listFull)
                    } else {
                        Toast.makeText(this@RiwayatAbsensiActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RiwayatResponse>, t: Throwable) {
                    Toast.makeText(this@RiwayatAbsensiActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterSearch(query: String) {
        val filteredList = listFull.filter {
            it.tanggal.contains(query, ignoreCase = true) || it.status.contains(query, ignoreCase = true)
        }
        adapter.updateData(filteredList)
    }
}