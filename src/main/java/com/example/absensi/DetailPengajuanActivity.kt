package com.example.absensi

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ApiMessageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailPengajuanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CUTI_ID = "extra_cuti_id"
        const val EXTRA_JENIS_CUTI = "extra_jenis_cuti"
        const val EXTRA_TANGGAL_MULAI = "extra_tanggal_mulai"
        const val EXTRA_TANGGAL_SELESAI = "extra_tanggal_selesai"
        const val EXTRA_ALASAN = "extra_alasan"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_TANGGAL_PENGAJUAN = "extra_tanggal_pengajuan"
        const val EXTRA_SISA_CUTI = "extra_sisa_cuti"
        const val EXTRA_KETERANGAN_PIMPINAN = "extra_keterangan_pimpinan"
        const val EXTRA_BERKAS_BUKTI = "extra_berkas_bukti"
    }

    private var cutiId: Int = -1
    private var status: String = ""
    private var keteranganPimpinan: String = ""
    private var berkasBukti: String = ""

    private lateinit var tvJenisCuti: TextView
    private lateinit var tvTanggalPengajuan: TextView
    private lateinit var tvDurasiCuti: TextView
    private lateinit var tvAlasan: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvSisaCuti: TextView
    private lateinit var tvCatatanPimpinan: TextView
    private lateinit var layoutBerkasBukti: LinearLayout
    private lateinit var tvBerkasBuktiLabel: TextView
    private lateinit var btnBukaBerkas: Button
    private lateinit var btnEditPengajuan: Button
    private lateinit var btnHapusPengajuan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pengajuan)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener { showLogoutConfirmation() }

        cutiId = intent.getIntExtra(EXTRA_CUTI_ID, -1)
        val jenisCuti = intent.getStringExtra(EXTRA_JENIS_CUTI).orEmpty()
        val tanggalMulai = intent.getStringExtra(EXTRA_TANGGAL_MULAI).orEmpty()
        val tanggalSelesai = intent.getStringExtra(EXTRA_TANGGAL_SELESAI).orEmpty()
        val alasan = intent.getStringExtra(EXTRA_ALASAN).orEmpty()
        status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        val tanggalPengajuan = intent.getStringExtra(EXTRA_TANGGAL_PENGAJUAN).orEmpty()
        val sisaCuti = intent.getIntExtra(EXTRA_SISA_CUTI, 0)
        keteranganPimpinan = intent.getStringExtra(EXTRA_KETERANGAN_PIMPINAN).orEmpty()
        berkasBukti = intent.getStringExtra(EXTRA_BERKAS_BUKTI).orEmpty()

        tvJenisCuti = findViewById(R.id.tvJenisCuti)
        tvTanggalPengajuan = findViewById(R.id.tvTanggalPengajuan)
        tvDurasiCuti = findViewById(R.id.tvDurasiCuti)
        tvAlasan = findViewById(R.id.tvAlasan)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvSisaCuti = findViewById(R.id.tvSisaCuti)
        tvCatatanPimpinan = findViewById(R.id.tvCatatanPimpinan)
        layoutBerkasBukti = findViewById(R.id.layoutBerkasBukti)
        tvBerkasBuktiLabel = findViewById(R.id.tvBerkasBuktiLabel)
        btnBukaBerkas = findViewById(R.id.btnBukaBerkas)
        btnEditPengajuan = findViewById(R.id.btnEditPengajuan)
        btnHapusPengajuan = findViewById(R.id.btnHapusPengajuan)

        tvJenisCuti.text = jenisCuti.ifBlank { "-" }
        tvTanggalPengajuan.text = if (tanggalPengajuan.isNotBlank()) "Diajukan: $tanggalPengajuan" else "Diajukan: -"
        tvDurasiCuti.text = if (tanggalMulai.isNotBlank() && tanggalSelesai.isNotBlank()) {
            "$tanggalMulai s/d $tanggalSelesai"
        } else {
            "-"
        }
        tvAlasan.text = alasan.ifBlank { "-" }
        tvSisaCuti.text = "$sisaCuti hari"

        if (keteranganPimpinan.isNotBlank()) {
            tvCatatanPimpinan.visibility = View.VISIBLE
            tvCatatanPimpinan.text = keteranganPimpinan
        } else {
            tvCatatanPimpinan.visibility = View.GONE
        }

        // Handle berkas bukti display
        if (berkasBukti.isNotBlank()) {
            layoutBerkasBukti.visibility = View.VISIBLE
            tvBerkasBuktiLabel.text = "Bukti Dokumen"
            btnBukaBerkas.setOnClickListener {
                openBerkasBukti()
            }
        } else {
            layoutBerkasBukti.visibility = View.GONE
        }

        setStatusBadge(status)

        val canModify = status.lowercase().startsWith("pending")
        btnEditPengajuan.visibility = if (canModify) View.VISIBLE else View.GONE
        btnHapusPengajuan.visibility = if (canModify) View.VISIBLE else View.GONE

        btnEditPengajuan.setOnClickListener {
            startActivity(Intent(this, PengajuanActivity::class.java).apply {
                putExtra(PengajuanActivity.EXTRA_MODE, PengajuanActivity.MODE_EDIT)
                putExtra(PengajuanActivity.EXTRA_CUTI_ID, cutiId)
                putExtra(PengajuanActivity.EXTRA_JENIS_CUTI, jenisCuti)
                putExtra(PengajuanActivity.EXTRA_TANGGAL_MULAI, tanggalMulai)
                putExtra(PengajuanActivity.EXTRA_TANGGAL_SELESAI, tanggalSelesai)
                putExtra(PengajuanActivity.EXTRA_ALASAN, alasan)
                putExtra(PengajuanActivity.EXTRA_KETERANGAN_PIMPINAN, keteranganPimpinan)
            })
            finish()
        }

        btnHapusPengajuan.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun setStatusBadge(status: String) {
        val normalized = status.lowercase()
        tvStatusBadge.text = when (normalized) {
            "approved" -> "Disetujui"
            "rejected" -> "Ditolak"
            else -> "Menunggu"
        }

        when (normalized) {
            "approved" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)
                tvStatusBadge.setTextColor(Color.parseColor("#155724"))
            }
            "rejected" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)
                tvStatusBadge.setTextColor(Color.parseColor("#721C24"))
            }
            else -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                tvStatusBadge.setTextColor(Color.parseColor("#856404"))
            }
        }
    }

    private fun openBerkasBukti() {
        if (berkasBukti.isBlank()) {
            Toast.makeText(this, "File bukti tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(berkasBukti)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Tidak dapat membuka file. Pastikan file masih tersedia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pengajuan")
            .setMessage("Yakin ingin menghapus pengajuan ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                deletePengajuan()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePengajuan() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        if (cutiId <= 0) {
            Toast.makeText(this, "ID pengajuan tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        ApiConfig.getApiService().deleteCuti("Bearer $token", cutiId)
            .enqueue(object : Callback<ApiMessageResponse> {
                override fun onResponse(call: Call<ApiMessageResponse>, response: Response<ApiMessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DetailPengajuanActivity, response.body()?.message ?: "Pengajuan berhasil dihapus.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string()?.let { body ->
                                org.json.JSONObject(body).optString("message", "Gagal menghapus pengajuan.")
                            } ?: "Gagal menghapus pengajuan."
                        } catch (e: Exception) {
                            "Gagal menghapus pengajuan."
                        }
                        Toast.makeText(this@DetailPengajuanActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse>, t: Throwable) {
                    Toast.makeText(this@DetailPengajuanActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya, Logout") { _, _ -> logout() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

