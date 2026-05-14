package com.example.absensi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

import com.example.absensi.network.ApiConfig
import com.example.absensi.network.RiwayatResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvJamMasukDash: TextView
    private lateinit var tvJamPulangDash: TextView
    private lateinit var tvStatusDash: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Tambah Logout Button Listener
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // ...existing code...
        val cardStatusHariIni: MaterialCardView = findViewById(R.id.cardStatusHariIni)

        // Diubah menjadi Button
        val btnLihatRiwayat: Button = findViewById(R.id.btnLihatRiwayat)

        tvJamMasukDash = findViewById(R.id.tvJamMasukDash)
        tvJamPulangDash = findViewById(R.id.tvJamPulangDash)
        tvStatusDash = findViewById(R.id.tvStatusDash)


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

                        // Penambahan Logika Warna
                        when (status.lowercase(Locale.getDefault())) {
                            "hadir" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#E8F5E9")) // Background Hijau Muda
                                tvStatusDash.setTextColor(Color.parseColor("#2E7D32")) // Text Hijau Tua
                            }
                            "terlambat" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#FFF3E0"))
                                tvStatusDash.setTextColor(Color.parseColor("#EF6C00"))
                            }
                            "alfa" -> {
                                tvStatusDash.setBackgroundColor(Color.parseColor("#FFEBEE"))
                                tvStatusDash.setTextColor(Color.parseColor("#C62828"))
                            }
                            else -> { // Untuk Cuti, Sakit, atau status lainnya
                                tvStatusDash.setBackgroundColor(Color.parseColor("#E3F2FD"))
                                tvStatusDash.setTextColor(Color.parseColor("#1565C0"))
                            }
                        }
                    } else {
                        tvJamMasukDash.text = "--:--"
                        tvJamPulangDash.text = "--:--"
                        tvStatusDash.text = "Belum Absen"

                        // Warna default Abu-abu untuk Belum Absen
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

    // FUNGSI LOGOUT
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
        // Hapus semua data session di SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear() // Hapus semua data
        editor.apply()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        // Kembali ke LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear stack
        startActivity(intent)
        finish()
    }
}