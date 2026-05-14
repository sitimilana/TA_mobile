package com.example.absensi

import android.content.Intent
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
import com.example.absensi.adapter.RewardAdapter
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RewardItem
import com.example.absensi.network.RewardResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class RewardListActivity : AppCompatActivity() {

    private lateinit var rvReward: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnFilter: Button

    private lateinit var adapter: RewardAdapter

    // Menyimpan data asli kandidat reward agar bisa difilter secara lokal
    private var allRewards: List<RewardItem> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward_list)

        // Inisialisasi Navigasi Bawah dan Header
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Inisialisasi View
        rvReward = findViewById(R.id.rvRewardList)
        etSearch = findViewById(R.id.etSearch)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)

        rvReward.layoutManager = LinearLayoutManager(this)

        setupSpinners()
        setupListeners()
        fetchDataReward()
    }

    private fun setupSpinners() {
        // Setup Spinner Bulan
        val listBulan = arrayOf(
            "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val bulanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listBulan)
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBulan.adapter = bulanAdapter

        // Setup Spinner Tahun (Dinamis dari tahun 2023 sampai tahun saat ini)
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
        // Listener Pencarian Real-time
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener Tombol Filter
        btnFilter.setOnClickListener {
            applyFilter()
        }
    }

    private fun fetchDataReward() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getRewards(token).enqueue(object : Callback<RewardResponse> {
            override fun onResponse(call: Call<RewardResponse>, response: Response<RewardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val rawData = response.body()?.data ?: emptyList()

                    // --- IMPLEMENTASI ALUR DIAGRAM AKTIVITAS ---
                    // Logika bawaan Anda: Skor >= 85 & Tidak Ada Alpha
                    val candidates = rawData.filter { it.skor >= 85 && it.alpha == 0 }

                    if (candidates.isEmpty()) {
                        Toast.makeText(this@RewardListActivity, "Tidak ada kandidat reward", Toast.LENGTH_SHORT).show()
                    }

                    // Simpan data kandidat ke variabel global untuk proses filter
                    allRewards = candidates

                    // Tampilkan data utuh pertama kali
                    setAdapter(allRewards)
                } else {
                    Toast.makeText(this@RewardListActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RewardResponse>, t: Throwable) {
                Toast.makeText(this@RewardListActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilter() {
        val keyword = etSearch.text.toString().trim().lowercase()

        // Dapatkan value dari Spinner
        val selectedBulanStr = spinnerBulan.selectedItem.toString()
        val selectedTahunStr = spinnerTahun.selectedItem.toString()

        // Konversi nama bulan menjadi angka (contoh: "Januari" -> 1)
        val bulanAngka = when (selectedBulanStr) {
            "Januari" -> 1; "Februari" -> 2; "Maret" -> 3; "April" -> 4
            "Mei" -> 5; "Juni" -> 6; "Juli" -> 7; "Agustus" -> 8
            "September" -> 9; "Oktober" -> 10; "November" -> 11; "Desember" -> 12
            else -> 0 // "Semua Bulan"
        }

        val filteredList = allRewards.filter { item ->
            // Pencarian berdasarkan nama, jenis reward, atau alasan
            val matchKeyword = (item.nama.lowercase().contains(keyword)) ||
                    (item.jenis.lowercase().contains(keyword)) ||
                    (item.alasan.lowercase().contains(keyword)) ||
                    (item.tanggal.lowercase().contains(keyword))

            // Ekstrak Tahun dan Bulan dari format string tanggal (misal: "2024-05-14")
            val dateParts = item.tanggal.split("-")
            val itemYear = dateParts.getOrNull(0) ?: ""
            // Gunakan toIntOrNull agar "05" terbaca menjadi angka 5
            val itemMonth = dateParts.getOrNull(1)?.toIntOrNull() ?: 0

            // Cek kecocokan bulan & tahun dari spinner
            val matchBulan = if (selectedBulanStr == "Semua Bulan") true else itemMonth == bulanAngka
            val matchTahun = if (selectedTahunStr == "Semua Tahun") true else itemYear == selectedTahunStr

            // Gabungkan kondisi
            matchKeyword && matchBulan && matchTahun
        }

        // Terapkan hasil filter ke adapter
        setAdapter(filteredList)

        if (filteredList.isEmpty() && allRewards.isNotEmpty()) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi bantuan untuk set/reset adapter beserta aksi kliknya
    private fun setAdapter(list: List<RewardItem>) {
        adapter = RewardAdapter(list) { reward ->
            val intent = Intent(this, RewardDetailActivity::class.java)
            intent.putExtra("REWARD_ID", reward.id)
            startActivity(intent)
        }
        rvReward.adapter = adapter
    }
}