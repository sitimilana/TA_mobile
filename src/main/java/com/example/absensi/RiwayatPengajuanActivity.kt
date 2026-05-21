package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatPengajuanAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.CutiResponse
import com.example.absensi.network.CutiItem // MENGGUNAKAN CLASS MODEL YANG BENAR
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RiwayatPengajuanActivity : AppCompatActivity() {

    private lateinit var rvRiwayatCuti: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnFilter: Button

    private lateinit var adapter: RiwayatPengajuanAdapter

    // SEKARANG BENAR: Menggunakan tipe data CutiItem secara langsung
    private var allCutiList: List<CutiItem> = listOf()
    private var currentSisaCuti: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_pengajuan)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Hubungkan tombol Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Inisialisasi View
        rvRiwayatCuti = findViewById(R.id.rvRiwayatCuti)
        etSearch = findViewById(R.id.etSearch)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)

        rvRiwayatCuti.layoutManager = LinearLayoutManager(this)
        adapter = RiwayatPengajuanAdapter(emptyList()) { item -> bukaDetailPengajuan(item) }
        rvRiwayatCuti.adapter = adapter

        setupSpinners()
        setupListeners()
        ambilDataCuti()
    }

    private fun setupSpinners() {
        val listBulan = arrayOf(
            "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val bulanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listBulan)
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBulan.adapter = bulanAdapter

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val listTahun = mutableListOf("Semua Tahun")
        for (i in 2023..currentYear) {
            listTahun.add(i.toString())
        }
        val tahunAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listTahun)
        tahunAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTahun.adapter = tahunAdapter
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilter.setOnClickListener {
            applyFilter()
        }
    }

    private fun ambilDataCuti() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        ApiConfig.getApiService().getRiwayatCuti("Bearer $token").enqueue(object : Callback<CutiResponse> {
            override fun onResponse(call: Call<CutiResponse>, response: Response<CutiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val dataCuti = response.body()?.data ?: emptyList()
                    allCutiList = dataCuti
                    currentSisaCuti = response.body()?.sisaCuti ?: 0
                    applyFilter()
                } else {
                    Toast.makeText(this@RiwayatPengajuanActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CutiResponse>, t: Throwable) {
                Toast.makeText(this@RiwayatPengajuanActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Menggunakan pemfilteran objek langsung (Strongly-Typed)
    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim().lowercase()
        val selectedBulanStr = spinnerBulan.selectedItem.toString()
        val selectedTahunStr = spinnerTahun.selectedItem.toString()

        val bulanAngka = when (selectedBulanStr) {
            "Januari" -> "01"; "Februari" -> "02"; "Maret" -> "03"; "April" -> "04"
            "Mei" -> "05"; "Juni" -> "06"; "Juli" -> "07"; "Agustus" -> "08"
            "September" -> "09"; "Oktober" -> "10"; "November" -> "11"; "Desember" -> "12"
            else -> ""
        }

        val filteredList = allCutiList.filter { item ->
            // Mengakses langsung property milik model CutiItem
            val tglMulai = item.tanggalMulai ?: ""
            val jenis = item.jenisCuti ?: ""
            val alasan = item.alasan ?: ""
            val status = item.status ?: ""

            val matchKeyword = tglMulai.lowercase().contains(keyword) ||
                    jenis.lowercase().contains(keyword) ||
                    alasan.lowercase().contains(keyword) ||
                    status.lowercase().contains(keyword)

            val matchBulan = if (selectedBulanStr == "Semua Bulan") true else tglMulai.contains("-$bulanAngka-")
            val matchTahun = if (selectedTahunStr == "Semua Tahun") true else tglMulai.startsWith(selectedTahunStr)

            matchKeyword && matchBulan && matchTahun
        }

        if (filteredList.isEmpty() && allCutiList.isNotEmpty()) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        adapter = RiwayatPengajuanAdapter(filteredList) { item ->
            bukaDetailPengajuan(item)
        }
        rvRiwayatCuti.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        ambilDataCuti()
    }

    private fun bukaDetailPengajuan(item: CutiItem) {
        val intent = Intent(this, DetailPengajuanActivity::class.java)
        intent.putExtra(DetailPengajuanActivity.EXTRA_CUTI_ID, item.idCuti ?: -1)
        intent.putExtra(DetailPengajuanActivity.EXTRA_JENIS_CUTI, item.jenisCuti.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_TANGGAL_MULAI, item.tanggalMulai.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_TANGGAL_SELESAI, item.tanggalSelesai.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_ALASAN, item.alasan.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_STATUS, item.status.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_TANGGAL_PENGAJUAN, item.tanggalPengajuan.orEmpty())
        intent.putExtra(DetailPengajuanActivity.EXTRA_SISA_CUTI, currentSisaCuti)
        intent.putExtra(DetailPengajuanActivity.EXTRA_KETERANGAN_PIMPINAN, item.keteranganPimpinan.orEmpty())
        startActivity(intent)
    }

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