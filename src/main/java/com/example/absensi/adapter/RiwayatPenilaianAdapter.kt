package com.example.absensi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.PenilaianData

class RiwayatPenilaianAdapter(private val listPenilaian: List<PenilaianData>) :
    RecyclerView.Adapter<RiwayatPenilaianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeriode: TextView = view.findViewById(R.id.tvPeriode)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvSkor: TextView = view.findViewById(R.id.tvSkor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_penilaian, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listPenilaian[position]

        // 1. Menggunakan Int biasa (bukan String, jadi hilangkan toIntOrNull)
        val namaBulan = getNamaBulan(item.bulan)
        // Kita juga tambahkan .tahun (karena sudah dibuat di Data Class)
        val tahunTeks = item.tahun ?: "-"
        holder.tvPeriode.text = "Periode: $namaBulan $tahunTeks"

        // 2. Menampilkan Skor Angka
        val skor = item.totalSkor ?: 0
        holder.tvSkor.text = "$skor/100"

        // 3. Konversi Skor Angka menjadi Teks Kategori & Warna Label
        when {
            skor >= 85 -> {
                holder.tvStatus.text = "Sangat Baik"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            }
            skor >= 70 -> {
                holder.tvStatus.text = "Baik"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            }
            skor >= 60 -> {
                holder.tvStatus.text = "Cukup"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
            else -> {
                holder.tvStatus.text = "Kurang"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected)
            }
        }
    }

    override fun getItemCount(): Int {
        return listPenilaian.size
    }

    private fun getNamaBulan(bulan: Int?): String {
        val namaBulanArray = arrayOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return if (bulan != null && bulan in 1..12) {
            namaBulanArray[bulan]
        } else {
            "-"
        }
    }
}