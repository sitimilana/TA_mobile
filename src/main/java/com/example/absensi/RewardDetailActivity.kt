package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RewardItem
import com.example.absensi.network.RewardResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RewardDetailActivity : AppCompatActivity() {

    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailPeriode: TextView // DITAMBAHKAN
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailScore: TextView
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward_detail)

        // 1. Inisialisasi Navigasi Bawah dan Header
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Hubungkan tombol Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // 2. Inisialisasi View
        tvDetailName = findViewById(R.id.tvDetailRewardName)
        tvDetailPeriode = findViewById(R.id.tvDetailRewardPeriode) // DITAMBAHKAN
        tvDetailDate = findViewById(R.id.tvDetailRewardDate)
        tvDetailScore = findViewById(R.id.tvDetailRewardScore)
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        // 3. Ambil ID Reward dari Intent
        val rewardId = intent.getIntExtra("REWARD_ID", -1)
        if (rewardId != -1) {
            fetchRewardDetail(rewardId)
        } else {
            Toast.makeText(this, "Data reward tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchRewardDetail(id: Int) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getRewards(token).enqueue(object : Callback<RewardResponse> {
            override fun onResponse(call: Call<RewardResponse>, response: Response<RewardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val reward = response.body()?.data?.find { it.id == id }
                    if (reward != null) {
                        displayReward(reward)
                    } else {
                        Toast.makeText(this@RewardDetailActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RewardDetailActivity, "Gagal memuat detail reward", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RewardResponse>, t: Throwable) {
                Toast.makeText(this@RewardDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayReward(reward: RewardItem) {
        tvDetailName.text = reward.nama
        tvDetailDate.text = reward.tanggal
        tvDetailScore.text = reward.skor.toString()

        // DITAMBAHKAN: Mengambil info periode dari kolom keterangan database
        tvDetailPeriode.text = reward.alasan // Menggunakan properti alasan/keterangan yang menampung string DB tersebut
    }

    // Fungsi Dialog Konfirmasi Logout
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

    // Fungsi Hapus Sesi dan Kembali ke Login
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