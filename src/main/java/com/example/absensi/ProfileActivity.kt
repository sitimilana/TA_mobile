package com.example.absensi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ProfilResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvProfilNama: TextView = findViewById(R.id.tvProfilNama)
        val tvProfilRole: TextView = findViewById(R.id.tvProfilRole)
        val btnUbahPassword: Button = findViewById(R.id.btnUbahPassword)
        val btnProfilLogout: Button = findViewById(R.id.btnProfilLogout)

        // Ambil data sementara dari SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        tvProfilNama.text = sharedPref.getString("NAMA_LENGKAP", "Pengguna")
        tvProfilRole.text = sharedPref.getString("ROLE", "Karyawan")

        // Inisialisasi data biodata dari layout include
        val rowEmail = findViewById<View>(R.id.row_email)
        rowEmail.findViewById<TextView>(R.id.tvLabel).text = "Email"
        rowEmail.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("EMAIL", "-")

        val rowHp = findViewById<View>(R.id.row_hp)
        rowHp.findViewById<TextView>(R.id.tvLabel).text = "No. HP"
        rowHp.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("NO_HP", "-")

        val rowAlamat = findViewById<View>(R.id.row_alamat)
        rowAlamat.findViewById<TextView>(R.id.tvLabel).text = "Alamat"
        rowAlamat.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("ALAMAT", "-")

        val rowTglMasuk = findViewById<View>(R.id.row_tgl_masuk)
        rowTglMasuk.findViewById<TextView>(R.id.tvLabel).text = "Tanggal Masuk"
        rowTglMasuk.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("TANGGAL_MASUK", "-")

        // Panggil API untuk update data terbaru
        fetchDataProfil()

        // Tombol Ubah Password
        btnUbahPassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Tombol Logout
        btnProfilLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya, Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchDataProfil() {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = "Bearer " + sharedPref.getString("TOKEN", "")

        ApiConfig.getApiService().getProfil(token).enqueue(object : Callback<ProfilResponse> {
            override fun onResponse(call: Call<ProfilResponse>, response: Response<ProfilResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data

                    // Update TextView
                    findViewById<TextView>(R.id.tvProfilNama).text = data.namaLengkap
                    findViewById<TextView>(R.id.tvProfilRole).text = data.divisi

                    // Update Data Row
                    updateRow(R.id.row_email, "Email", data.email)
                    updateRow(R.id.row_hp, "No. HP", data.noHp)
                    updateRow(R.id.row_alamat, "Alamat", data.alamat)
                    updateRow(R.id.row_tgl_masuk, "Tanggal Masuk", data.tanggalMasuk)
                }
            }

            override fun onFailure(call: Call<ProfilResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateRow(rowId: Int, label: String, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.tvLabel).text = label
        row.findViewById<TextView>(R.id.tvValue).text = value
    }
}