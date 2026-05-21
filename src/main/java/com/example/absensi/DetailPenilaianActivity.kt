package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator

class DetailPenilaianActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_penilaian)

        // Mengaktifkan fungsionalitas Header dan Bottom Navigation
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Fitur Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Mengikat (Binding) Komponen UI (btnBack sudah dihapus dari sini)
        val tvPeriodeDetail: TextView = findViewById(R.id.tvPeriodeDetail)
        val tvTotalSkorDetail: TextView = findViewById(R.id.tvTotalSkorDetail)

        val tvSkorDisiplin: TextView = findViewById(R.id.tvSkorDisiplin)
        val tvSkorProduktivitas: TextView = findViewById(R.id.tvSkorProduktivitas)
        val tvSkorTanggungJawab: TextView = findViewById(R.id.tvSkorTanggungJawab)
        val tvSkorSikap: TextView = findViewById(R.id.tvSkorSikap)
        val tvSkorLoyalitas: TextView = findViewById(R.id.tvSkorLoyalitas)

        // Menghubungkan ID Progress Bar (Untuk Animasi Modern)
        val progressDisiplin: LinearProgressIndicator = findViewById(R.id.progressDisiplin)
        val progressProduktivitas: LinearProgressIndicator = findViewById(R.id.progressProduktivitas)
        val progressTanggungJawab: LinearProgressIndicator = findViewById(R.id.progressTanggungJawab)
        val progressSikap: LinearProgressIndicator = findViewById(R.id.progressSikap)
        val progressLoyalitas: LinearProgressIndicator = findViewById(R.id.progressLoyalitas)

        // Menerima data dari halaman sebelumnya
        val intent = intent
        val bulan = intent.getStringExtra("EXTRA_BULAN") ?: "-"
        val tahun = intent.getStringExtra("EXTRA_TAHUN") ?: "-"
        val totalSkor = intent.getIntExtra("EXTRA_SKOR_TOTAL", 0)

        val disiplin = intent.getIntExtra("EXTRA_DISIPLIN", 0)
        val produktivitas = intent.getIntExtra("EXTRA_PRODUKTIVITAS", 0)
        val tanggungJawab = intent.getIntExtra("EXTRA_TANGGUNG_JAWAB", 0)
        val sikap = intent.getIntExtra("EXTRA_SIKAP_KERJA", 0)
        val loyalitas = intent.getIntExtra("EXTRA_LOYALITAS", 0)

        // Menulis Text ke Layar
        tvPeriodeDetail.text = "Periode: $bulan $tahun"
        tvTotalSkorDetail.text = "$totalSkor/100"

        tvSkorDisiplin.text = "$disiplin/5"
        tvSkorProduktivitas.text = "$produktivitas/5"
        tvSkorTanggungJawab.text = "$tanggungJawab/5"
        tvSkorSikap.text = "$sikap/5"
        tvSkorLoyalitas.text = "$loyalitas/5"

        // Menggerakkan Progress Bar sesuai nilai (Max 5)
        progressDisiplin.progress = disiplin
        progressProduktivitas.progress = produktivitas
        progressTanggungJawab.progress = tanggungJawab
        progressSikap.progress = sikap
        progressLoyalitas.progress = loyalitas
    }

    // Fungsi Konfirmasi Logout dengan Dialog
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

    // Fungsi Eksekusi Membersihkan Sesi dan Kembali ke Login
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