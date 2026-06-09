package com.example.absensi

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide // Pastikan import Glide ini ada

object NavigationUtils {
    fun setupHeaderWithUserData(activity: Activity) {
        try {
            val tvWelcomeName: TextView? = activity.findViewById(R.id.tvWelcomeName)
            val tvRole: TextView? = activity.findViewById(R.id.tvRole)

            // 1. Ubah tipe ivProfile menjadi ImageView
            val ivProfile: ImageView? = activity.findViewById(R.id.ivProfile)

            if (tvWelcomeName != null || tvRole != null) {
                val sharedPref = activity.getSharedPreferences("AppPrefs", Activity.MODE_PRIVATE)
                val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan") ?: "Karyawan"
                val divisi = sharedPref.getString("DIVISI", "Staff") ?: "Staff"

                // 2. Ambil URL foto profil dari SharedPreferences
                val fotoUrl = sharedPref.getString("FOTO_PROFIL", "")

                tvWelcomeName?.text = "Selamat Datang,\n$namaLengkap"
                tvRole?.text = divisi

                // 3. LOGIKA GLIDE UNTUK MENAMPILKAN FOTO DI HEADER
                if (ivProfile != null) {
                    if (!fotoUrl.isNullOrEmpty()) {
                        // Jika URL foto ada, tampilkan pakai Glide
                        Glide.with(activity)
                            .load(fotoUrl)
                            .placeholder(R.drawable.profile) // Gambar default saat proses loading
                            .error(R.drawable.profile)       // Gambar default jika url gagal dimuat
                            .into(ivProfile)
                    } else {
                        // Jika belum ada foto, tampilkan ikon default
                        ivProfile.setImageResource(R.drawable.profile)
                    }

                    // Fungsi klik untuk masuk ke halaman profil tetap dipertahankan
                    ivProfile.setOnClickListener {
                        val intent = Intent(activity, ProfileActivity::class.java)
                        activity.startActivity(intent)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupBottomNav(activity: Activity) {
        // Find containers for larger touch targets
        val homeBtn = activity.findViewById<View>(R.id.nav_home_container)
        val cutiBtn = activity.findViewById<View>(R.id.nav_cuti_container)
        val gajiBtn = activity.findViewById<View>(R.id.nav_gaji_container)
        val penilaianBtn = activity.findViewById<View>(R.id.nav_penilaian_container)
        val rewardBtn = activity.findViewById<View>(R.id.nav_reward_container)

        // Find icons to update color/active state
        val homeIcon = activity.findViewById<android.widget.ImageView>(R.id.nav_home)
        val cutiIcon = activity.findViewById<android.widget.ImageView>(R.id.nav_cuti)
        val gajiIcon = activity.findViewById<android.widget.ImageView>(R.id.nav_gaji)
        val penilaianIcon = activity.findViewById<android.widget.ImageView>(R.id.nav_penilaian)
        val rewardIcon = activity.findViewById<android.widget.ImageView>(R.id.nav_reward)

        val activeColor = android.graphics.Color.parseColor("#4A86E8") // Avatar blue
        val inactiveColor = android.graphics.Color.parseColor("#888888")

        // Reset all icons to inactive color
        homeIcon?.setColorFilter(inactiveColor)
        cutiIcon?.setColorFilter(inactiveColor)
        gajiIcon?.setColorFilter(inactiveColor)
        penilaianIcon?.setColorFilter(inactiveColor)
        rewardIcon?.setColorFilter(inactiveColor)

        // Highlight the active icon based on current activity
        when (activity) {
            is DashboardActivity -> homeIcon?.setColorFilter(activeColor)
            is PengajuanActivity -> cutiIcon?.setColorFilter(activeColor)
            is SlipGajiActivity -> gajiIcon?.setColorFilter(activeColor)
            is RiwayatPenilaianActivity -> penilaianIcon?.setColorFilter(activeColor)
            is RewardListActivity -> rewardIcon?.setColorFilter(activeColor)
        }

        // Navigasi ke HOME (Dashboard)
        homeBtn?.setOnClickListener {
            if (activity !is DashboardActivity) {
                val intent = Intent(activity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                if (activity !is DashboardActivity) activity.overridePendingTransition(0, 0)
            }
        }

        // Navigasi ke FORM PENGAJUAN CUTI (Icon Kalender)
        cutiBtn?.setOnClickListener {
            if (activity !is PengajuanActivity) {
                val intent = Intent(activity, PengajuanActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        // Navigasi ke SLIP GAJI
        gajiBtn?.setOnClickListener {
            if (activity !is SlipGajiActivity) {
                val intent = Intent(activity, SlipGajiActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        // Navigasi ke PENILAIAN KINERJA
        penilaianBtn?.setOnClickListener {
            if (activity !is RiwayatPenilaianActivity) {
                val intent = Intent(activity, RiwayatPenilaianActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        // Navigasi ke REWARD
        rewardBtn?.setOnClickListener {
            if (activity !is RewardListActivity) {
                val intent = Intent(activity, RewardListActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }
}