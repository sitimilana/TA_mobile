package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatAbsensiAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RiwayatData
import com.example.absensi.network.RiwayatResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatAbsensiActivity : AppCompatActivity() {

    private lateinit var rvTable: RecyclerView
    private lateinit var adapter: RiwayatAbsensiAdapter
    private var listFull = mutableListOf<RiwayatData>()

    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerTgl: Spinner
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnFilter: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_absensi)

        // 1. Setup Header & Navigasi
        NavigationUtils.setupHeaderWithUserData(this)
        NavigationUtils.setupBottomNav(this)

        // DITAMBAHKAN: Hubungkan tombol Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // 2. Inisialisasi RecyclerView
        rvTable = findViewById(R.id.rvTableAbsensi)
        rvTable.layoutManager = LinearLayoutManager(this)
        adapter = RiwayatAbsensiAdapter(listFull)
        rvTable.adapter = adapter

        // 3. Setup Fitur Search & Filter
        etSearch = findViewById(R.id.etSearch)
        spinnerTgl = findViewById(R.id.spinnerTgl)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)

        setupSpinners()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilter.setOnClickListener {
            applyFilter()
        }

        fetchRiwayatAbsensi()
    }

    private fun setupSpinners() {
        val listTgl = mutableListOf("Semua Tgl")
        for (i in 1..31) listTgl.add(i.toString().padStart(2, '0'))

        val listBulan = listOf("Semua Bln", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val listTahun = mutableListOf("Semua Thn")
        for (i in currentYear downTo 2020) listTahun.add(i.toString())

        spinnerTgl.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listTgl)
        spinnerBulan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listBulan)
        spinnerTahun.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listTahun)
    }

    private fun fetchRiwayatAbsensi() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val idUser = sharedPref.getString("ID_USER", "") ?: ""
        val tanggalMasuk = sharedPref.getString("TANGGAL_MASUK", "") ?: ""

        ApiConfig.getApiService().getRiwayatAbsensi(idUser)
            .enqueue(object : Callback<RiwayatResponse> {
                override fun onResponse(call: Call<RiwayatResponse>, response: Response<RiwayatResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data ?: emptyList()
                        
                        // ============================================================
                        // FILTER: Hanya tampilkan riwayat dari tanggal_masuk ke depan
                        // ============================================================
                        listFull.clear()
                        if (tanggalMasuk.isNotEmpty()) {
                            // Filter data berdasarkan tanggal_masuk (approval date)
                            listFull.addAll(data.filter { 
                                it.tanggal >= tanggalMasuk  // Hanya tampilkan dari tanggal approval ke depan
                            })
                        } else {
                            // Jika tanggalMasuk kosong/null, tampilkan semua (fallback)
                            listFull.addAll(data)
                        }
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

    private fun applyFilter() {
        val query = etSearch.text.toString().trim()
        val tgl = if (spinnerTgl.selectedItemPosition > 0) spinnerTgl.selectedItem.toString() else ""
        val bulan = if (spinnerBulan.selectedItemPosition > 0) spinnerBulan.selectedItem.toString() else ""
        val tahun = if (spinnerTahun.selectedItemPosition > 0) spinnerTahun.selectedItem.toString() else ""

        val filteredList = listFull.filter {
            val matchQuery = it.tanggal.contains(query, true) || it.status.contains(query, true)

            val matchTgl = tgl.isEmpty() || it.tanggal.contains("-$tgl") || it.tanggal.startsWith("$tgl-")
            val matchBulan = bulan.isEmpty() || it.tanggal.contains("-$bulan-")
            val matchTahun = tahun.isEmpty() || it.tanggal.contains(tahun)

            matchQuery && matchTgl && matchBulan && matchTahun
        }
        adapter.updateData(filteredList)
    }

    // DITAMBAHKAN: Fungsi Dialog Konfirmasi Logout
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya, Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // DITAMBAHKAN: Fungsi Hapus Sesi dan Kembali ke Login
    private fun logout() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}