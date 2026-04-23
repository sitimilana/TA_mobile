package com.example.absensi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensi.R
import com.example.absensi.network.SalaryItem
import java.text.NumberFormat
import java.util.*

class SlipGajiAdapter(
    private var list: List<SalaryItem>,
    private val onDetailClick: (SalaryItem) -> Unit
) : RecyclerView.Adapter<SlipGajiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvNetSalary: TextView = view.findViewById(R.id.tvNetSalary)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnDetail: View = view.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slip_gaji, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvDate.text = item.tanggal

        // Format angka ke Rupiah
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        holder.tvNetSalary.text = formatRupiah.format(item.gajiBersih)

        holder.tvStatus.text = item.status
        holder.btnDetail.setOnClickListener { onDetailClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<SalaryItem>) {
        list = newList
        notifyDataSetChanged()
    }
}