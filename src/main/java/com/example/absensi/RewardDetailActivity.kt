package com.example.absensi

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RewardItem
import com.example.absensi.network.RewardResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RewardDetailActivity : AppCompatActivity() {

    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailType: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailScore: TextView
    private lateinit var tvDetailReason: TextView
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward_detail)

        // 1. Inisialisasi Navigasi Bawah
        NavigationUtils.setupBottomNav(this)

        // 2. Inisialisasi View
        tvDetailName = findViewById(R.id.tvDetailRewardName)
        tvDetailType = findViewById(R.id.tvDetailRewardType)
        tvDetailDate = findViewById(R.id.tvDetailRewardDate)
        tvDetailScore = findViewById(R.id.tvDetailRewardScore)
        tvDetailReason = findViewById(R.id.tvDetailRewardReason)
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        // 3. Tampilkan Data User di Header (dari SharedPreferences)
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")
        tvWelcomeName.text = "Selamat Datang, $namaLengkap"
        tvRole.text = "Karyawan"

        // 4. Ambil ID Reward dari Intent
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

        // Mengambil semua data reward lalu mencari yang ID-nya cocok
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
        tvDetailType.text = reward.jenis
        tvDetailDate.text = reward.tanggal
        tvDetailScore.text = reward.skor.toString()
        tvDetailReason.text = reward.alasan
    }
}
