package com.example.absensi.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.SalaryData
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale
import android.content.Intent
import com.example.absensi.DetailGajiActivity

class SlipGajiAdapter(private val listGaji: List<SalaryData>) :
    RecyclerView.Adapter<SlipGajiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Menggunakan ID yang persis sama dengan XML Anda
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvNetSalary: TextView = view.findViewById(R.id.tvNetSalary)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnDetail: MaterialButton = view.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slip_gaji, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listGaji[position]

        // 1. Format Bulan & Tahun (Menyesuaikan id tvDate)
        if (item.periode != null) {
            holder.tvDate.text = item.periode
        } else {
            val namaBulan = getNamaBulan(item.bulan)
            holder.tvDate.text = "$namaBulan ${item.tahun ?: ""}".trim()
        }

        // 2. Format Uang Rupiah (Menyesuaikan id tvNetSalary)
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val totalGaji = item.totalGaji ?: 0
        // Replace agar spasi setelah Rp tampil rapi: "Rp 4.200.000"
        holder.tvNetSalary.text = formatRupiah.format(totalGaji).replace("Rp", "Rp ")

        // 3. Status Slip dengan Perubahan Warna Dinamis
        val status = item.status ?: item.statusSlip ?: "draft"
        if (status.equals("final", ignoreCase = true) || status.equals("selesai", ignoreCase = true)) {
            holder.tvStatus.text = "Selesai"
            holder.tvStatus.setTextColor(Color.parseColor("#155724")) // Hijau tua
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
        } else {
            holder.tvStatus.text = "Diproses"
            holder.tvStatus.setTextColor(Color.parseColor("#856404")) // Kuning/Coklat tua
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
        }

        // 4. Aksi Tombol Detail
        holder.btnDetail.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailGajiActivity::class.java)
            // Mengirimkan ID Gaji ke halaman detail
            intent.putExtra("ID_GAJI", item.idGaji ?: -1)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listGaji.size

    private fun getNamaBulan(bulan: Int?): String {
        val namaBulanArray = arrayOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return if (bulan != null && bulan in 1..12) namaBulanArray[bulan] else "-"
    }
}