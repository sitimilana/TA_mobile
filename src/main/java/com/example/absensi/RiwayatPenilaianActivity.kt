package com.example.absensi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.adapter.RiwayatPenilaianAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.PenilaianResponse
import com.example.absensi.network.PenilaianData // Class yang benar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RiwayatPenilaianActivity : AppCompatActivity() {

    private lateinit var rvRiwayatPenilaian: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnFilter: Button

    private lateinit var adapter: RiwayatPenilaianAdapter

    // PERBAIKAN 1: Ganti DataItemPenilaian menjadi PenilaianData
    private var allRiwayatList: List<PenilaianData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_penilaian)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Inisialisasi View
        rvRiwayatPenilaian = findViewById(R.id.rvRiwayatKinerja)
        etSearch = findViewById(R.id.etSearch)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)

        rvRiwayatPenilaian.layoutManager = LinearLayoutManager(this)

        setupSpinners()
        setupListeners()
        ambilDataPenilaian()
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

    private fun ambilDataPenilaian() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        ApiConfig.getApiService().getPenilaian("Bearer $token")
            .enqueue(object : Callback<PenilaianResponse> {
                override fun onResponse(call: Call<PenilaianResponse>, response: Response<PenilaianResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body?.success == true && body.data != null && body.data.isNotEmpty()) {
                            allRiwayatList = body.data

                            adapter = RiwayatPenilaianAdapter(allRiwayatList)
                            rvRiwayatPenilaian.adapter = adapter
                        } else {
                            Toast.makeText(this@RiwayatPenilaianActivity, body?.message ?: "Belum ada riwayat penilaian", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<PenilaianResponse>, t: Throwable) {
                    Toast.makeText(this@RiwayatPenilaianActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim().lowercase()

        val selectedBulanStr = spinnerBulan.selectedItem.toString()
        val selectedTahunStr = spinnerTahun.selectedItem.toString()

        val bulanAngka = when (selectedBulanStr) {
            "Januari" -> "1"; "Februari" -> "2"; "Maret" -> "3"; "April" -> "4"
            "Mei" -> "5"; "Juni" -> "6"; "Juli" -> "7"; "Agustus" -> "8"
            "September" -> "9"; "Oktober" -> "10"; "November" -> "11"; "Desember" -> "12"
            else -> ""
        }

        val filteredList = allRiwayatList.filter { item ->
            // PERBAIKAN 2: Ganti item.total_skor menjadi item.totalSkor
            val matchKeyword = (item.bulan?.toString()?.lowercase()?.contains(keyword) == true) ||
                    (item.tahun?.toString()?.lowercase()?.contains(keyword) == true) ||
                    (item.totalSkor?.toString()?.lowercase()?.contains(keyword) == true)

            val matchBulan = if (selectedBulanStr == "Semua Bulan") true else item.bulan?.toString() == bulanAngka
            val matchTahun = if (selectedTahunStr == "Semua Tahun") true else item.tahun?.toString() == selectedTahunStr

            matchKeyword && matchBulan && matchTahun
        }

        adapter = RiwayatPenilaianAdapter(filteredList)
        rvRiwayatPenilaian.adapter = adapter

        if (filteredList.isEmpty() && allRiwayatList.isNotEmpty()) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}