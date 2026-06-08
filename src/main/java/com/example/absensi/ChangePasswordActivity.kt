package com.example.absensi

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ApiMessageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pastikan Anda sudah membuat activity_change_password.xml
        setContentView(R.layout.activity_change_password)

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

        ApiConfig.getApiService().changePassword(token, lama, baru, konfirmasi)
            .enqueue(object : Callback<ApiMessageResponse> {
                override fun onResponse(call: Call<ApiMessageResponse>, response: Response<ApiMessageResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ChangePasswordActivity, "Password Berhasil Diubah!", Toast.LENGTH_SHORT).show()
                        finish() // Kembali ke profil
                    } else {
                        // Menampilkan pesan error dari server (misal: Password lama salah)
                        Toast.makeText(this@ChangePasswordActivity, response.body()?.message ?: "Gagal mengubah password", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse>, t: Throwable) {
                    Toast.makeText(this@ChangePasswordActivity, "Error koneksi: " + t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}