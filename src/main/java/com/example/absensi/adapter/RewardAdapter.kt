package com.example.absensi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.RewardItem

class RewardAdapter(
    private var list: List<RewardItem>,
    private val onClick: (RewardItem) -> Unit
) : RecyclerView.Adapter<RewardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaReward)
        val tvDate: TextView = view.findViewById(R.id.tvDateReward)
        val tvScore: TextView = view.findViewById(R.id.tvScoreReward)
        val btnDetail: View = view.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Tetap menggunakan item.nama karena sudah dijembatani oleh @SerializedName("nama_karyawan")
        holder.tvNama.text = item.nama ?: "-"
        holder.tvDate.text = item.tanggal

        // DIUBAH: Menambahkan string "Nilai : " agar format tampilan teks rapi dan konsisten
        holder.tvScore.text = "${item.skor}"

        holder.btnDetail.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<RewardItem>) {
        list = newList
        notifyDataSetChanged()
    }
}