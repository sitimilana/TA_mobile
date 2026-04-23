package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        NavigationUtils.setupBottomNav(this)

        // 1. Ambil data yang disimpan saat Login tadi
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")

        // 2. Cari TextView di XML
        // PERHATIAN: Pastikan TextView "Selamat Datang" di XML Anda memiliki id="@+id/tvWelcomeName"
        // Ubah XML Anda sedikit untuk menambahkan ID ini pada TextView-nya.
        val tvWelcomeName: TextView = findViewById(R.id.tvWelcomeName)
        val tvRole: TextView = findViewById(R.id.tvRole)
        val cardStatusHariIni: MaterialCardView = findViewById(R.id.cardStatusHariIni)
        val btnLihatRiwayat: TextView = findViewById(R.id.btnLihatRiwayat)
        // 3. Pasang nama ke layar
        tvWelcomeName.text = "Selamat Datang, $namaLengkap"
        tvRole.text = "Karyawan" // Karena aplikasi mobile ini khusus karyawan

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
}