package com.example.absensi

import android.app.Activity
import android.content.Intent
import android.view.View

object NavigationUtils {
    fun setupBottomNav(activity: Activity) {
        // Navigasi ke HOME (Dashboard)
        activity.findViewById<View>(R.id.nav_home)?.setOnClickListener {
            if (activity !is DashboardActivity) {
                val intent = Intent(activity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }

        // Navigasi ke FORM PENGAJUAN CUTI (Icon Kalender)
        activity.findViewById<View>(R.id.nav_cuti)?.setOnClickListener {
            if (activity !is PengajuanActivity) {
                val intent = Intent(activity, PengajuanActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // Navigasi ke SLIP GAJI
        activity.findViewById<View>(R.id.nav_gaji)?.setOnClickListener {
            if (activity !is SlipGajiActivity) {
                try {
                    val intent = Intent(activity, SlipGajiActivity::class.java)
                    activity.startActivity(intent)
                } catch (e: Exception) {}
            }
        }

        // Navigasi ke PENILAIAN KINERJA
        activity.findViewById<View>(R.id.nav_penilaian)?.setOnClickListener {
            if (activity !is RiwayatPenilaianActivity) {
                try {
                    val intent = Intent(activity, RiwayatPenilaianActivity::class.java)
                    activity.startActivity(intent)
                } catch (e: Exception) {}
            }
        }

        // Navigasi ke REWARD
        activity.findViewById<View>(R.id.nav_reward)?.setOnClickListener {
            if (activity !is RewardListActivity) {
                try {
                    val intent = Intent(activity, RewardListActivity::class.java)
                    activity.startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }
}
