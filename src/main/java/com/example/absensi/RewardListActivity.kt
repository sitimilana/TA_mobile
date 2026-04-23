package com.example.absensi

import android.content.Intent
import android.os.Bundle
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

class RewardListActivity : AppCompatActivity() {

    private lateinit var rvReward: RecyclerView
    private lateinit var adapter: RewardAdapter
    private var allRewards = mutableListOf<RewardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward_list)

        // Inisialisasi Navigasi Bawah
        NavigationUtils.setupBottomNav(this)

        rvReward = findViewById(R.id.rvRewardList)
        rvReward.layoutManager = LinearLayoutManager(this)

        adapter = RewardAdapter(allRewards) { reward ->
            // Alur: Memilih penerima -> Menampilkan Detail
            val intent = Intent(this, RewardDetailActivity::class.java)
            intent.putExtra("REWARD_ID", reward.id)
            startActivity(intent)
        }
        rvReward.adapter = adapter

        fetchDataReward()
    }

    private fun fetchDataReward() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("TOKEN", "")}"

        ApiConfig.getApiService().getRewards(token).enqueue(object : Callback<RewardResponse> {
            override fun onResponse(call: Call<RewardResponse>, response: Response<RewardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val rawData = response.body()?.data ?: emptyList()

                    // --- IMPLEMENTASI ALUR DIAGRAM AKTIVITAS ---
                    // Logika: Skor >= 85 & Tidak Ada Alpha
                    val candidates = rawData.filter { it.skor >= 85 && it.alpha == 0 }

                    if (candidates.isEmpty()) {
                        Toast.makeText(this@RewardListActivity, "Tidak ada kandidat reward bulan ini", Toast.LENGTH_SHORT).show()
                    }

                    adapter.updateData(candidates)
                } else {
                    Toast.makeText(this@RewardListActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RewardResponse>, t: Throwable) {
                Toast.makeText(this@RewardListActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
