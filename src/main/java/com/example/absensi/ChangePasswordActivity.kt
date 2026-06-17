package com.example.absensi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback // <-- WAJIB DITAMBAHKAN
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ApiMessageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // --- SISTEM TOMBOL BACK MODERN (AndroidX) ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val isFirstLogin = sharedPref.getBoolean("IS_FIRST_LOGIN", false)

                if (isFirstLogin) {
                    // Jika memaksa kembali sebelum ganti password default, keluarkan dari aplikasi
                    Toast.makeText(this@ChangePasswordActivity, "Anda harus mengganti password terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                } else {
                    // Jika bukan first login, matikan hadangan ini dan lakukan back normal
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        // --------------------------------------------

        val etPasswordLama = findViewById<EditText>(R.id.etPasswordLama)
        val etPasswordBaru = findViewById<EditText>(R.id.etPasswordBaru)
        val etKonfirmasiPassword = findViewById<EditText>(R.id.etKonfirmasiPassword)
        val btnSimpanPassword = findViewById<Button>(R.id.btnSimpanPassword)

        btnSimpanPassword.setOnClickListener {
            val lama = etPasswordLama.text.toString()
            val baru = etPasswordBaru.text.toString()
            val konfirmasi = etKonfirmasiPassword.text.toString()

            if (lama.isEmpty() || baru.isEmpty() || konfirmasi.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (baru != konfirmasi) {
                Toast.makeText(this, "Password baru tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            callChangePasswordApi(lama, baru, konfirmasi)
        }
    }

    private fun callChangePasswordApi(lama: String, baru: String, konfirmasi: String) {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = "Bearer " + sharedPref.getString("TOKEN", "")
        val isFirstLogin = sharedPref.getBoolean("IS_FIRST_LOGIN", false) // Ambil status

        // Nonaktifkan tombol sementara agar tidak diklik berkali-kali
        val btnSimpanPassword = findViewById<Button>(R.id.btnSimpanPassword)
        btnSimpanPassword.isEnabled = false
        btnSimpanPassword.text = "Memproses..."

        ApiConfig.getApiService().changePassword(token, lama, baru, konfirmasi)
            .enqueue(object : Callback<ApiMessageResponse> {
                override fun onResponse(call: Call<ApiMessageResponse>, response: Response<ApiMessageResponse>) {
                    btnSimpanPassword.isEnabled = true
                    btnSimpanPassword.text = "Simpan Password"

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ChangePasswordActivity, "Password Berhasil Diubah!", Toast.LENGTH_SHORT).show()

                        if (isFirstLogin) {
                            // Cabut status terkunci karena password sudah diganti
                            sharedPref.edit().putBoolean("IS_FIRST_LOGIN", false).apply()

                            // Arahkan ke Dashboard
                            val intent = Intent(this@ChangePasswordActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Jika bukan first login (ubah rutin dari profil), cukup tutup halaman
                            finish()
                        }

                    } else {
                        Toast.makeText(this@ChangePasswordActivity, response.body()?.message ?: "Gagal mengubah password", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse>, t: Throwable) {
                    btnSimpanPassword.isEnabled = true
                    btnSimpanPassword.text = "Simpan Password"
                    Toast.makeText(this@ChangePasswordActivity, "Error koneksi: " + t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}