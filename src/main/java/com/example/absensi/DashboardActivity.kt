package com.example.absensi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RewardItem
import com.example.absensi.network.RewardResponse
import com.example.absensi.network.RiwayatResponse
import com.example.absensi.network.PenilaianResponse
import com.example.absensi.network.PenilaianData
import com.example.absensi.network.SisaCutiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit // DITAMBAHKAN untuk hitung hari

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvJamMasukDash: TextView
    private lateinit var tvJamPulangDash: TextView
    private lateinit var tvStatusDash: TextView
    private lateinit var layoutBannerReward: LinearLayout
    
    // ← TAMBAHAN: Views untuk sisa cuti
    private lateinit var tvSisaCutiDash: TextView
    private lateinit var tvSisaCutiHariIni: TextView

    private lateinit var barChartKinerja: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        val cardStatusHariIni: MaterialCardView = findViewById(R.id.cardStatusHariIni)
        val btnLihatRiwayat: Button = findViewById(R.id.btnLihatRiwayat)

        tvJamMasukDash = findViewById(R.id.tvJamMasukDash)
        tvJamPulangDash = findViewById(R.id.tvJamPulangDash)
        tvStatusDash = findViewById(R.id.tvStatusDash)
        layoutBannerReward = findViewById(R.id.layoutBannerReward)
        
        // ← TAMBAHAN: Initialize views sisa cuti
        tvSisaCutiDash = findViewById(R.id.tvSisaCutiDash)
        tvSisaCutiHariIni = findViewById(R.id.tvSisaCutiHariIni)
        barChartKinerja = findViewById(R.id.barChartKinerja)

        val btnMasuk: MaterialCardView = findViewById(R.id.btnMasuk)
        val btnPulang: MaterialCardView = findViewById(R.id.btnPulang)

        btnMasuk.setOnClickListener {
            val intent = Intent(this, PresensiActivity::class.java)
            intent.putExtra("JENIS_ABSEN", "masuk")
            startActivity(intent)
        }

        btnPulang.setOnClickListener {
            val intent = Intent(this, PresensiActivity::class.java)
            intent.putExtra("JENIS_ABSEN", "pulang")
            startActivity(intent)
        }

        val intentRiwayat = Intent(this, RiwayatAbsensiActivity::class.java)

        cardStatusHariIni.setOnClickListener {
            startActivity(intentRiwayat)
        }

        btnLihatRiwayat.setOnClickListener {
            startActivity(intentRiwayat)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchPresensiHariIni()
        fetchMyRewardData()
        fetchDataKinerjaChart()
        fetchSisaCuti()  // ← TAMBAHAN: Fetch sisa cuti
    }

    private fun fetchPresensiHariIni() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val idUser = sharedPref.getString("ID_USER", "")

        if (idUser.isNullOrEmpty()) return

        ApiConfig.getApiService().getRiwayatAbsensi(idUser).enqueue(object : Callback<RiwayatResponse> {
            override fun onResponse(call: Call<RiwayatResponse>, response: Response<RiwayatResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val riwayatList = response.body()?.data
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = sdf.format(Date())
                    val presensiHariIni = riwayatList?.find { it.tanggal == today }

                    if (presensiHariIni != null) {
                        tvJamMasukDash.text = presensiHariIni.jamMasuk ?: "--:--"
                        tvJamPulangDash.text = presensiHariIni.jamPulang ?: "--:--"

                        val status = presensiHariIni.status
                        tvStatusDash.text = status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }

                        when (status.lowercase(Locale.getDefault())) {
                            "hadir" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#E8F5E9"))
                                tvStatusDash.setTextColor(Color.parseColor("#2E7D32"))
                            }
                            "terlambat" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#FFF3E0"))
                                tvStatusDash.setTextColor(Color.parseColor("#EF6C00"))
                            }
                            "alfa" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#FFEBEE"))
                                tvStatusDash.setTextColor(Color.parseColor("#C62828"))
                            }
                            else -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#E3F2FD"))
                                tvStatusDash.setTextColor(Color.parseColor("#1565C0"))
                            }
                        }
                    } else {
                        tvJamMasukDash.text = "--:--"
                        tvJamPulangDash.text = "--:--"
                        tvStatusDash.text = "Belum Absen"
                        tvStatusDash.setBackgroundColor(Color.parseColor("#F5F5F5"))
                        tvStatusDash.setTextColor(Color.parseColor("#888888"))
                    }
                }
            }

            override fun onFailure(call: Call<RiwayatResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Gagal memuat status hari ini", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchMyRewardData() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getRewards(token).enqueue(object : Callback<RewardResponse> {
            override fun onResponse(call: Call<RewardResponse>, response: Response<RewardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val rewardList = response.body()?.data ?: emptyList()
                    checkMyReward(rewardList)
                }
            }
            override fun onFailure(call: Call<RewardResponse>, t: Throwable) {}
        })
    }

    // YANG DIUBAH: Logika hilangnya banner menjadi 7 Hari setelah tanggal_reward dari database
    private fun checkMyReward(listReward: List<RewardItem>) {
        if (listReward.isEmpty()) {
            layoutBannerReward.visibility = View.GONE
            return
        }

        val latestReward = listReward.maxByOrNull { it.tanggal }

        if (latestReward != null) {
            try {
                // 1. Ekstrak teks bulan untuk ditampilkan di banner (misal: "April")
                val parts = latestReward.tanggal.split("-") // Format: "yyyy-MM-dd"
                if (parts.size >= 3) {
                    val inputYear = parts[0].toInt()
                    val inputMonth = parts[1].toInt()

                    // MUNDUR 1 BULAN: Jika reward diinput Mei, berarti untuk kinerja bulan April
                    var rewardMonth = inputMonth - 1
                    var rewardYear = inputYear
                    if (rewardMonth <= 0) {
                        rewardMonth = 12
                        rewardYear -= 1
                    }

                    // 2. Hitung selisih hari antara "Hari Ini" dengan "Tanggal Pemberian Reward"
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val rewardDate = sdf.parse(latestReward.tanggal)
                    val currentDate = Date()

                    if (rewardDate != null) {
                        val diffInMillis = currentDate.time - rewardDate.time
                        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                        // 3. LOGIKA EXPIRED: Tampilkan jika selisihnya 7 hari atau kurang
                        if (diffInDays <= 7) {
                            layoutBannerReward.visibility = View.VISIBLE

                            val namaBulanArray = arrayOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                            val bulanTeks = if (rewardMonth in 1..12) namaBulanArray[rewardMonth] else ""

                            val tvPesanReward = layoutBannerReward.getChildAt(1) as TextView
                            tvPesanReward.text = "Anda terpilih sebagai Karyawan Terbaik bulan $bulanTeks $rewardYear!"
                        } else {
                            // Sembunyikan jika sudah lewat 7 hari dari tanggal reward
                            layoutBannerReward.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                layoutBannerReward.visibility = View.GONE
            }
        } else {
            layoutBannerReward.visibility = View.GONE
        }
    }

    private fun fetchDataKinerjaChart() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getPenilaian(token).enqueue(object : Callback<PenilaianResponse> {
            override fun onResponse(call: Call<PenilaianResponse>, response: Response<PenilaianResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val rawData = response.body()?.data ?: emptyList()

                    val sortedList = rawData.sortedWith(compareBy<PenilaianData> { it.tahun ?: 0 }.thenBy { it.bulan ?: 0 })

                    setupKinerjaBarChart(sortedList)
                }
            }

            override fun onFailure(call: Call<PenilaianResponse>, t: Throwable) {
            }
        })
    }

    private fun setupKinerjaBarChart(dataPenilaian: List<PenilaianData>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        val namaBulan = arrayOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")

        dataPenilaian.forEachIndexed { index, item ->
            val skor = item.totalSkor?.toFloat() ?: 0f
            val tahun = item.tahun ?: 0
            val bulan = item.bulan ?: 0

            entries.add(BarEntry(index.toFloat(), skor))

            val shortYear = tahun.toString().takeLast(2)
            val labelBulan = if (bulan in 1..12) namaBulan[bulan] else "Bln"
            labels.add("$labelBulan '$shortYear")
        }

        val barDataSet = BarDataSet(entries, "Skor Bulanan")
        barDataSet.color = Color.parseColor("#8F9FC4")
        barDataSet.valueTextColor = Color.parseColor("#333333")
        barDataSet.valueTextSize = 10f

        val barData = BarData(barDataSet)
        barData.barWidth = 0.5f

        barChartKinerja.data = barData

        val xAxis = barChartKinerja.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.parseColor("#666666")

        val axisLeft = barChartKinerja.axisLeft
        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 100f
        axisLeft.textColor = Color.parseColor("#666666")

        barChartKinerja.axisRight.isEnabled = false

        barChartKinerja.description.isEnabled = false
        barChartKinerja.legend.isEnabled = false
        barChartKinerja.animateY(1000)
        barChartKinerja.invalidate()
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
    private fun fetchSisaCuti() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getSisaCuti(token).enqueue(object : Callback<SisaCutiResponse> {
            override fun onResponse(call: Call<SisaCutiResponse>, response: Response<SisaCutiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data

                    if (data != null) {
                        // Simpan data lengkap ke SharedPreferences sebagai cache lokal
                        val editor = sharedPref.edit()
                        editor.putInt("SISA_CUTI_TAHUN_INI", data.sisaCutiTahunIni)
                        editor.putInt("TOTAL_CUTI_TAHUN_INI", data.totalCutiTahunIni)
                        editor.putInt("CUTI_DIGUNAKAN", data.cutiDigunakan)
                        editor.putInt("CUTI_PENDING", data.cutiPending)
                        editor.putInt("CUTI_APPROVED", data.cutiApproved)
                        editor.putInt("SISA_CUTI_BULAN_INI", data.sisaCutiBulanIni ?: 1)
                        editor.apply()

                        // UPDATE: Tampilkan "TAHUN INI"
                        tvSisaCutiDash.text = "${data.sisaCutiTahunIni} hari"

                        // TAMBAHAN: Tampilkan "BULAN INI"
                        val sisaCutiBulanIni = data.sisaCutiBulanIni ?: 1
                        tvSisaCutiHariIni.text = "$sisaCutiBulanIni hari"

                        // ===== PEWARNAAN TAHUN INI =====
                        when {
                            data.sisaCutiTahunIni <= 0 -> {
                                tvSisaCutiDash.setTextColor(Color.parseColor("#E53935")) // Merah - Habis
                            }
                            data.sisaCutiTahunIni <= 2 -> {
                                tvSisaCutiDash.setTextColor(Color.parseColor("#FB8C00")) // Oranye - Kritis
                            }
                            else -> {
                                tvSisaCutiDash.setTextColor(Color.parseColor("#00C853")) // Hijau - Tersedia
                            }
                        }

                        // ===== PEWARNAAN BULAN INI =====
                        when {
                            sisaCutiBulanIni <= 0 -> {
                                tvSisaCutiHariIni.setTextColor(Color.parseColor("#E53935")) // Merah - Habis
                            }
                            sisaCutiBulanIni <= 1 -> {
                                tvSisaCutiHariIni.setTextColor(Color.parseColor("#FB8C00")) // Oranye - Kritis
                            }
                            else -> {
                                tvSisaCutiHariIni.setTextColor(Color.parseColor("#00C853")) // Hijau - Tersedia
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<SisaCutiResponse>, t: Throwable) {
                android.util.Log.e("SISA_CUTI", "Gagal memuat data cuti: ${t.message}")
            }
        })
    }
}