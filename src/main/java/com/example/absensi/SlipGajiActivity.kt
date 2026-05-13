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
import com.example.absensi.adapter.SlipGajiAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.SalaryData
import com.example.absensi.network.SalaryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SlipGajiActivity : AppCompatActivity() {

    private lateinit var rvSlipGaji: RecyclerView
    private lateinit var adapter: SlipGajiAdapter
    private var allSalaryList: List<SalaryData> = emptyList()

    private lateinit var etSearch: EditText
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnFilter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slip_gaji)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        rvSlipGaji = findViewById(R.id.rvSalarySlips)
        rvSlipGaji.layoutManager = LinearLayoutManager(this)

        etSearch = findViewById(R.id.etSearch)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)

        setupSpinners()
        setupListeners()
        ambilDataGaji()
    }

    private fun setupSpinners() {
        val bulanList = listOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val bulanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bulanList)
        spinnerBulan.adapter = bulanAdapter

        val tahunList = listOf("Semua Tahun", "2024", "2025", "2026", "2027") // Modify as needed
        val tahunAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tahunList)
        spinnerTahun.adapter = tahunAdapter
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilter.setOnClickListener {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()
        val filterBulan = spinnerBulan.selectedItem.toString()
        val filterTahun = spinnerTahun.selectedItem.toString()

        var filteredList = allSalaryList

        // Filter String Query
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                val periode = it.periode?.lowercase() ?: ""
                val bulanStr = getBulanName(it.bulan ?: 0).lowercase()
                val tahunStr = (it.tahun ?: "").toString()
                
                periode.contains(query) || bulanStr.contains(query) || tahunStr.contains(query)
            }
        }

        // Filter Spinner
        if (filterBulan != "Semua Bulan") {
            filteredList = filteredList.filter {
                val periode = it.periode ?: ""
                val bulannya = getBulanName(it.bulan ?: 0)
                periode.contains(filterBulan, ignoreCase = true) || bulannya.equals(filterBulan, ignoreCase = true)
            }
        }
        if (filterTahun != "Semua Tahun") {
            filteredList = filteredList.filter {
                val periode = it.periode ?: ""
                val tahunnya = (it.tahun ?: "").toString()
                periode.contains(filterTahun, ignoreCase = true) || tahunnya.equals(filterTahun, ignoreCase = true)
            }
        }

        updateRecyclerView(filteredList)
    }

    private fun getBulanName(bulan: Int): String {
        val namaBulanArray = arrayOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return if (bulan in 1..12) namaBulanArray[bulan] else ""
    }

    private fun updateRecyclerView(list: List<SalaryData>) {
        adapter = SlipGajiAdapter(list)
        rvSlipGaji.adapter = adapter
    }

    private fun ambilDataGaji() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        ApiConfig.getApiService().getSlipGaji("Bearer $token")
            .enqueue(object : Callback<SalaryResponse> {
                override fun onResponse(call: Call<SalaryResponse>, response: Response<SalaryResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true && body.data != null && body.data.isNotEmpty()) {
                            allSalaryList = body.data
                            updateRecyclerView(allSalaryList)
                        } else {
                            Toast.makeText(this@SlipGajiActivity, "Belum ada riwayat gaji.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SlipGajiActivity, "Gagal mengambil data gaji.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SalaryResponse>, t: Throwable) {
                    Toast.makeText(this@SlipGajiActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}