package com.example.absensi.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.RiwayatData

class RiwayatAbsensiAdapter(private var list: List<RiwayatData>) :
    RecyclerView.Adapter<RiwayatAbsensiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Menggunakan view.findViewById agar konsisten
        val tvNo: TextView = view.findViewById(R.id.tvNo)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvJamMasuk: TextView = view.findViewById(R.id.tvJamMasuk)
        val tvJamPulang: TextView = view.findViewById(R.id.tvJamPulang)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table_absensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Pastikan memanggil nama variabel yang benar sesuai di ViewHolder
        holder.tvNo.text = (position + 1).toString()
        holder.tvTanggal.text = item.tanggal
        holder.tvJamMasuk.text = item.jamMasuk ?: "--:--"
        holder.tvJamPulang.text = item.jamPulang ?: "--:--"
        holder.tvStatus.text = item.status.uppercase()

        // Logika Warna Status
        when (item.status.lowercase()) {
            "hadir" -> holder.tvStatus.setTextColor(Color.parseColor("#2ECC71")) // Hijau
            "terlambat" -> holder.tvStatus.setTextColor(Color.parseColor("#F1C40F")) // Kuning
            "alfa" -> holder.tvStatus.setTextColor(Color.parseColor("#E74C3C")) // Merah (Ubah alpha jadi alfa sesuai database)
            "cuti", "izin", "sakit" -> holder.tvStatus.setTextColor(Color.parseColor("#3498DB")) // Biru
            else -> holder.tvStatus.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<RiwayatData>) {
        list = newList
        notifyDataSetChanged()
    }
}