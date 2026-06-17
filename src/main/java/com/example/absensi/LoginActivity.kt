package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.LoginResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "")
        val isFirstLogin = sharedPref.getBoolean("IS_FIRST_LOGIN", false)

        // AUTO-LOGIN CHECKER YANG LEBIH AMAN
        if (!token.isNullOrEmpty()) {
            if (isFirstLogin) {
                // Jika sebelumnya keluar app tanpa ganti password, kembalikan ke ChangePassword
                val intent = Intent(this, ChangePasswordActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Jika sudah aman, ke Dashboard
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }
            return
        }

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
            } else {
                ApiConfig.getApiService().login(username, password).enqueue(object : Callback<LoginResponse> {

                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        val body = response.body()

                        if (response.isSuccessful && body?.success == true) {
                            val editor = sharedPref.edit()

                            editor.putString("TOKEN", body.token ?: "")

                            val userData = body.data
                            val isFirst = userData?.isFirstLogin == true // Cek status first login

                            editor.putString("ID_USER", userData?.idUser?.toString() ?: "")
                            editor.putString("USERNAME", userData?.username ?: "")
                            editor.putString("NAMA_LENGKAP", userData?.namaLengkap ?: "")
                            editor.putString("TANGGAL_MASUK", userData?.tanggalMasuk ?: "")
                            editor.putBoolean("IS_FIRST_LOGIN", isFirst) // Simpan status keamanan ke HP

                            val divisi = userData?.divisi
                            if (divisi.isNullOrEmpty()) {
                                editor.putString("DIVISI", "Divisi belum diatur")
                            } else {
                                editor.putString("DIVISI", divisi)
                            }

                            editor.apply()

                            // LOGIKA ROUTING BLOCK
                            if (isFirst) {
                                Toast.makeText(this@LoginActivity, "Keamanan: Anda Wajib Mengganti Password Default!", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@LoginActivity, ChangePasswordActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, body.message, Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                                startActivity(intent)
                                finish()
                            }

                        } else {
                            try {
                                val errorBody = response.errorBody()
                                val errorString = errorBody?.string() ?: "Gagal"

                                if (errorString.contains("<!DOCTYPE html>") || errorString.contains("<html>")) {
                                    Toast.makeText(this@LoginActivity, "Server sedang Error (500). Cek Laravel Log!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@LoginActivity, body?.message ?: "Login Gagal", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@LoginActivity, "Terjadi kesalahan sistem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Koneksi Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }
}