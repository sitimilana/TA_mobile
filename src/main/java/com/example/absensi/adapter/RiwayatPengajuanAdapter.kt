package com.example.absensi.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.CutiItem

class RiwayatPengajuanAdapter(private val listCuti: List<CutiItem>) : RecyclerView.Adapter<RiwayatPengajuanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJenisCuti: TextView = view.findViewById(R.id.tvJenisCuti)
        val tvTanggalAjukan: TextView = view.findViewById(R.id.tvTanggalAjukan) // Pastikan ID ini ada di XML item Anda
        val tvDurasiCuti: TextView = view.findViewById(R.id.tvDurasiCuti)
        val tvAlasan: TextView = view.findViewById(R.id.tvAlasan)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val viewCategoryIndicator: View = view.findViewById(R.id.viewCategoryIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riwayat_pengajuan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cuti = listCuti[position]
        holder.tvJenisCuti.text = cuti.jenisCuti
        holder.tvTanggalAjukan.text = "Diajukan: ${cuti.tanggalPengajuan}"
        holder.tvDurasiCuti.text = "${cuti.tanggalMulai} s/d ${cuti.tanggalSelesai}"
        holder.tvAlasan.text = "Alasan: ${cuti.alasan}"

        // Atur warna status
        when (cuti.status) {
            "disetujui_hrd" -> {
                holder.tvStatusBadge.text = "Disetujui"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#155724")) // Hijau tua
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved) // Pastikan drawable ini ada
            }
            "ditolak" -> {
                holder.tvStatusBadge.text = "Ditolak"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#721C24")) // Merah tua
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected) // Pastikan drawable ini ada
            }
            else -> { // Pending
                holder.tvStatusBadge.text = "Menunggu"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#856404")) // Kuning tua
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending) // Pastikan drawable ini ada
            }
        }
    }

    override fun getItemCount() = listCuti.size
}