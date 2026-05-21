package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    // Banner Karyawan Terbaik
    private lateinit var bannerTopPerformer: LinearLayout

    // Menyimpan data asli kandidat reward agar bisa difilter secara lokal
    private var allRewards: List<RewardItem> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward_list)

        // Inisialisasi Navigasi Bawah dan Header
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // DITAMBAHKAN: Hubungkan tombol Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Inisialisasi View
        rvReward = findViewById(R.id.rvRewardList)
        etSearch = findViewById(R.id.etSearch)
        spinnerBulan = findViewById(R.id.spinnerBulan)
        spinnerTahun = findViewById(R.id.spinnerTahun)
        btnFilter = findViewById(R.id.btnFilter)
        bannerTopPerformer = findViewById(R.id.bannerTopPerformer)

        rvReward.layoutManager = LinearLayoutManager(this)

        setupSpinners()
        setupListeners()
        fetchDataReward()
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

    private fun fetchDataReward() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getRewards(token).enqueue(object : Callback<RewardResponse> {
            override fun onResponse(call: Call<RewardResponse>, response: Response<RewardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val rawData = response.body()?.data ?: emptyList()

                    val candidates = rawData.filter { it.skor >= 85 }


                    // --- LOGIKA MUNCULKAN BANNER "SELAMAT" JIKA DAPAT REWARD BULAN INI ---
                    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                    val isTopThisMonth = candidates.any { item ->
                        val parts = item.tanggal.split("-")
                        val itemYear = parts.getOrNull(0)?.toIntOrNull() ?: 0
                        val itemMonth = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        itemYear == currentYear && itemMonth == currentMonth
                    }

                    if (isTopThisMonth) {
                        bannerTopPerformer.visibility = View.VISIBLE
                    } else {
                        bannerTopPerformer.visibility = View.GONE
                    }

                    allRewards = candidates
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
        val selectedBulanStr = spinnerBulan.selectedItem.toString()
        val selectedTahunStr = spinnerTahun.selectedItem.toString()

        val bulanAngka = when (selectedBulanStr) {
            "Januari" -> 1; "Februari" -> 2; "Maret" -> 3; "April" -> 4
            "Mei" -> 5; "Juni" -> 6; "Juli" -> 7; "Agustus" -> 8
            "September" -> 9; "Oktober" -> 10; "November" -> 11; "Desember" -> 12
            else -> 0
        }

        val filteredList = allRewards.filter { item ->
            val matchKeyword =
                (item.nama?.lowercase()?.contains(keyword) == true) ||
                        (item.alasan.lowercase().contains(keyword)) ||
                        (item.tanggal.lowercase().contains(keyword))


            val dateParts = item.tanggal.split("-")
            val itemYear = dateParts.getOrNull(0) ?: ""
            val itemMonth = dateParts.getOrNull(1)?.toIntOrNull() ?: 0

            val matchBulan = if (selectedBulanStr == "Semua Bulan") true else itemMonth == bulanAngka
            val matchTahun = if (selectedTahunStr == "Semua Tahun") true else itemYear == selectedTahunStr

            matchKeyword && matchBulan && matchTahun
        }

        setAdapter(filteredList)

        if (filteredList.isEmpty() && allRewards.isNotEmpty()) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAdapter(list: List<RewardItem>) {
        adapter = RewardAdapter(list) { reward ->
            val intent = Intent(this, RewardDetailActivity::class.java)
            intent.putExtra("REWARD_ID", reward.id)
            startActivity(intent)
        }
        rvReward.adapter = adapter
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