package com.example.absensi.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.DetailPenilaianActivity // Import Activity Baru
import com.example.absensi.R
import com.example.absensi.network.PenilaianData

class RiwayatPenilaianAdapter(private val listPenilaian: List<PenilaianData>) :
    RecyclerView.Adapter<RiwayatPenilaianAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeriode: TextView = view.findViewById(R.id.tvPeriode)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvSkor: TextView = view.findViewById(R.id.tvSkor)
        // DITAMBAHKAN: Deklarasi Tombol Detail
        val btnDetailPenilaian: Button = view.findViewById(R.id.btnDetailPenilaian)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_penilaian, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listPenilaian[position]

        val namaBulan = getNamaBulan(item.bulan)
        val tahunTeks = item.tahun ?: "-"
        holder.tvPeriode.text = "Periode: $namaBulan $tahunTeks"

        val skor = item.totalSkor ?: 0
        holder.tvSkor.text = "$skor/100"

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

        // DITAMBAHKAN: Listener Klik untuk pindah ke halaman Detail
        holder.btnDetailPenilaian.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailPenilaianActivity::class.java).apply {
                // Mengirim data per kategori melalui Extra
                putExtra("EXTRA_BULAN", namaBulan)
                putExtra("EXTRA_TAHUN", tahunTeks.toString())
                putExtra("EXTRA_SKOR_TOTAL", skor)
                putExtra("EXTRA_DISIPLIN", item.disiplin ?: 0)
                putExtra("EXTRA_PRODUKTIVITAS", item.produktivitas ?: 0)
                putExtra("EXTRA_TANGGUNG_JAWAB", item.tanggungJawab ?: 0)
                putExtra("EXTRA_SIKAP_KERJA", item.sikapKerja ?: 0)
                putExtra("EXTRA_LOYALITAS", item.loyalitas ?: 0)
            }
            context.startActivity(intent)
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